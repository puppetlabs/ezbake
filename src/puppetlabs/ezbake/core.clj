(ns puppetlabs.ezbake.core
  (:import (java.io File InputStream InputStreamReader FileOutputStream)
           (java.util.jar JarEntry JarFile)
           (java.util.zip ZipOutputStream))
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-time.local :as local-time]
            [stencil.core :as stencil]
            [leiningen.core.main :as lein-main]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as lein-classpath]
            [leiningen.deploy :as deploy]
            [leiningen.jar :as jar]
            [leiningen.pom :as pom]
            [leiningen.uberjar :as uberjar]
            [schema.core :as schema]
            [puppetlabs.ezbake.dependency-utils :as deputils]
            [puppetlabs.ezbake.exec :as exec]
            [puppetlabs.config.typesafe :as ts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Vars

(def ^:dynamic resource-path
  ;; This is bound dynamically so that the value can be modified appropriately
  ;; when run in leiningen plugin context. When ezbake is run as an app (ie,
  ;; from a git repo head) the default given here is sufficient, but when run as
  ;; a plugin on a project, this is necessary to ensure that files are copied to
  ;; the project's preferred resources directory.
  "tmp/resources")
(def resource-prefix "puppetlabs/lein-ezbake/")
(def staging-dir "target/staging")
(def shared-bin-prefix "ext/bin/")
(def shared-config-prefix "ext/config/")
(def config-dir "ext/config/")
(def system-config-dir "ext/system-config/")
(def shared-cli-apps-prefix "ext/cli/")
(def shared-cli-defaults-prefix "ext/cli_defaults/")
(def docs-prefix "ext/docs/")
(def build-scripts-prefix "ext/build-scripts/")
(def terminus-prefix "puppet/")
(def additional-uberjar-checkouts-dir "target/uberjars")
(def cli-defaults-filename (str shared-cli-defaults-prefix "cli-defaults.sh.erb"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(def BootstrapSource
  (schema/enum :bootstrap-cfg :services-d))

(def ReplacesPkgs
  [{:package schema/Str
    :version schema/Str}])

(def RPMTriggers
  [{:package schema/Str
    :scripts [schema/Str]}])

(def DEBTriggers
  [{:interest-name schema/Str
    :scripts [schema/Str]}])

(def DockerConfig
  {(schema/optional-key :base-image) schema/Str
   (schema/optional-key :image-name) schema/Str
   (schema/optional-key :ports) [schema/Int]})

(def LocalProjectVars
  {(schema/optional-key :user) schema/Str
   (schema/optional-key :numeric-uid-gid) schema/Int
   (schema/optional-key :group) schema/Str
   (schema/optional-key :puppet-platform-version) schema/Int
   (schema/optional-key :bootstrap-source) BootstrapSource
   (schema/optional-key :create-dirs) [schema/Str]
   (schema/optional-key :build-type) schema/Str
   (schema/optional-key :reload-timeout) schema/Int
   (schema/optional-key :repo-target) schema/Str
   (schema/optional-key :nonfinal-repo-target) schema/Str
   (schema/optional-key :replaces-pkgs) ReplacesPkgs
   (schema/optional-key :start-after) [schema/Str]
   (schema/optional-key :start-before) [schema/Str]
   (schema/optional-key :start-timeout) schema/Int
   (schema/optional-key :stop-timeout) schema/Int
   (schema/optional-key :open-file-limit) schema/Int
   (schema/optional-key :main-namespace) schema/Str
   (schema/optional-key :java-args) schema/Str
   (schema/optional-key :java-args-cli) schema/Str
   (schema/optional-key :tk-args) schema/Str
   (schema/optional-key :redhat-postinst-install-triggers) RPMTriggers
   (schema/optional-key :redhat-postinst-upgrade-triggers) RPMTriggers
   (schema/optional-key :debian-interested-install-triggers) DEBTriggers
   (schema/optional-key :debian-interested-upgrade-triggers) DEBTriggers
   (schema/optional-key :debian-activated-triggers) [schema/Str]
   (schema/optional-key :logrotate-enabled) schema/Bool
   (schema/optional-key :docker) DockerConfig})

(def UberjarInfo
  {:uberjar File
   :dependency-coordinates [schema/Any]
   :lein-project {schema/Any schema/Any}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Shell / Filesystem Helpers

(defn get-resource-file
  [resource-type args]
  (->> (concat [resource-path resource-prefix resource-type] args)
       (str/join "/")
       io/file))

(defn get-template-file
  [& args]
  (get-resource-file "template" args))

(defn get-staging-template-file
  [& args]
  (get-resource-file "staging-templates" args))

(defn staging-dir-git-cmd
  [& args]
  (apply exec/exec "git"
         (format "--git-dir=%s" (fs/file staging-dir ".git"))
         (format "--work-tree=%s" staging-dir)
         args))

(defn files-from-dir-iter
  "Given an individual entry from a `fs/iterate-dir` result set, return a flat
  list of File objects for all of the files referenced in the entry."
  [iter-entry]
  {:pre [(vector? iter-entry)
         (= 3 (count iter-entry))]
   :post [(coll? %)
          (every? #(instance? File %) %)]}
  (let [dir   (first iter-entry)
        files (get iter-entry 2)]
    (->> files
         (remove (partial re-find #"^\."))
         (map #(fs/file dir %)))))

(defn find-files-recursively
  "Given a File object representing a directory, walks the directory (recursively)
  and returns a list of File objects for all of the files in the directory.
  (Return value does not include directories, only regular files.)"
  [dir]
  {:pre [(instance? File dir)
         (.isDirectory dir)]
   :post [(coll? %)
          (every? #(instance? File %) %)]}
  (let [iter (fs/iterate-dir dir)]
    (mapcat files-from-dir-iter iter)))

(defn get-ezbake-version
  "Get the version number of the ezbake plugin being used for this build."
  [lein-project]
  (->> (:plugins lein-project)
       (filter #(= 'puppetlabs/lein-ezbake (first %)))
       first
       second))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; General Staging Helper functions

(defn clean
  []
  (lein-main/info "deleting staging directory:" staging-dir)
  (fs/delete-dir staging-dir)
  (lein-main/info "deleting uberjar checkout directory:" additional-uberjar-checkouts-dir)
  (fs/delete-dir additional-uberjar-checkouts-dir))

(defn relativize
  "Convert an absolute File to a relative File"
  [base-path absolute-file]
  (-> base-path
      io/as-file
      .toURI
      (.relativize (.toURI absolute-file))
      .getPath
      (File.)))

(defn remove-erb-extension
  [f]
  {:pre [(or (instance? JarEntry f) (instance? File f) (instance? String f))]}
  (let [filename (condp instance? f
                   JarEntry (.getName f)
                   File (.getPath f)
                   String f)]
    (if (.endsWith filename ".erb")
      (.substring filename 0 (- (.length filename) 4))
      filename)))

(defprotocol AsRubyLiteral
  "Converts item to a corresponding, properly escaped ruby literal.
  e.g. [\"x\" \"'\"] becomes \"['x', '\\'']\"."
  (as-ruby-literal [this]))

(extend-protocol AsRubyLiteral
  ;; No floating point for now since we don't need it and haven't
  ;; checked the syntax correspondence carefully yet.

  nil (as-ruby-literal [s] "nil")

  Boolean (as-ruby-literal [s] (str s))

  Byte (as-ruby-literal [s] (str s))
  Short (as-ruby-literal [s] (str s))
  Integer (as-ruby-literal [s] (str s))
  Long (as-ruby-literal [s] (str s))
  BigInteger (as-ruby-literal [s] (str s))
  clojure.lang.BigInt (as-ruby-literal [s] (str s))

  String
  (as-ruby-literal [s]
    (let [esc (-> s
                  (str/replace "\\" "\\\\")
                  (str/replace "'" "\\'"))]
      (str "'" esc "'")))

  clojure.lang.Sequential
  (as-ruby-literal [s]
    (str "["
         (str/join ", " (map as-ruby-literal s))
         "]")))

(defn cp-template-files
  [template-dir]
  (lein-main/info "copying template files from" (.toString template-dir) "to" staging-dir)
  (doseq [f (fs/glob (fs/file template-dir) "*")]
    (if (fs/directory? f)
      (fs/copy-dir f staging-dir)
      (fs/copy+ f (format "%s/%s" staging-dir (fs/base-name f))))))

(defn get-project-config-dir
  [lein-project]
  (let [config-dir (get-in lein-project [:lein-ezbake :config-dir] "config")]
    (fs/file config-dir)))

(defn get-system-config-dir
  [lein-project]
  (let [config-dir (get-in lein-project [:lein-ezbake :system-config-dir] "system-config")]
    (fs/file config-dir)))

(defn generate-manifest-file
  [lein-project additional-uberjars-info]
  (let [uberjar-info (for [uberjar-info additional-uberjars-info]
                       {:uberjar (fs/base-name (:uberjar uberjar-info))
                        :dependencies (->> uberjar-info
                                           :lein-project
                                           deputils/generate-dependency-tree-string
                                           (str (:dependency-coordinates
                                                 uberjar-info) "\n"))})
        template-map
        {:ezbake-version (get-ezbake-version lein-project)
         :package-group (:group lein-project)
         :package-name (:name lein-project)
         :package-version (:version lein-project)
         :manifest-string (deputils/generate-manifest-string lein-project)
         :dependency-tree (deputils/generate-dependency-tree-string lein-project)
         :additional-uberjar-info uberjar-info
         :has-additional-uberjars (not (empty? additional-uberjars-info))}]
    (spit
     (fs/file staging-dir "ext" "ezbake.manifest")
     (stencil/render-string "
This package was built by the Puppet Labs packaging system.

EZBake version: {{{ezbake-version}}}
Release package: {{{package-group}}}/{{{package-name}}} ({{{package-version}}})
Bundled packages: {{{manifest-string}}}
{{#has-additional-uberjars}}
Additional Uberjars:
{{/has-additional-uberjars}}
{{#additional-uberjar-info}}
{{{uberjar}}}
{{/additional-uberjar-info}}

Dependency tree:

{{{dependency-tree}}}
{{#has-additional-uberjars}}
Additional uberjar dependencies:

{{/has-additional-uberjars}}
{{#additional-uberjar-info}}
{{{uberjar}}}:

{{{dependencies}}}
{{/additional-uberjar-info}}
"
                            template-map))))

(defn- get-cli-app-files-in
  [jar]
  (deputils/find-files-in-dir-in-jar jar shared-cli-apps-prefix))

(defn- get-cli-app-files-in-directory
  [dir-path]
  (deputils/find-files-in-dir dir-path shared-cli-apps-prefix))

(defn- get-cli-defaults-files-in
  [jar]
  (deputils/find-files-in-dir-in-jar jar shared-cli-defaults-prefix))

(defn- get-cli-defaults-files-in-directory
  [dir-path]
  (deputils/find-files-in-dir dir-path shared-cli-defaults-prefix))

(defn- get-terminus-files-in
  [jar]
  (deputils/find-files-in-dir-in-jar jar terminus-prefix))

(defn- get-terminus-files-in-directory
  [dir-path]
  (deputils/find-files-in-dir dir-path terminus-prefix))

(defn- get-bin-files-in
  [jar]
  (deputils/find-files-in-dir-in-jar jar shared-bin-prefix))

(defn- get-bin-files-in-directory
  [dir-path]
  (deputils/find-files-in-dir dir-path shared-bin-prefix))

(defn- get-config-files-in
  [jar]
  (deputils/find-files-in-dir-in-jar jar shared-config-prefix))

(defn- get-config-files-in-directory
  [dir-path]
  (deputils/find-files-in-dir dir-path shared-config-prefix))

(defn- get-build-scripts-files-in
  [jar]
  (deputils/find-files-in-dir-in-jar jar build-scripts-prefix))

(defn- get-build-scripts-files-in-directory
  [dir-path]
  (deputils/find-files-in-dir dir-path build-scripts-prefix))

(defn cp-shared-files
  [dependencies files-fn]
  (let [files (for [{:keys [project jar]} dependencies]
                [project jar (files-fn jar)])]
    (doseq [[project jar shared-files] files]
      (deputils/cp-files-from-jar shared-files jar staging-dir))
    ;; Return just a list of the files
    (mapcat last files)))

(defn cp-project-build-scripts
  "Copy build-scripts in the project directory into the staging directory"
  [lein-project]
  ;; TODO: try all resource-paths, not just first
  (let [project-resource-path (first (get lein-project :resource-paths))]
    (fs/copy-dir-into (fs/file project-resource-path build-scripts-prefix) (fs/file staging-dir build-scripts-prefix))))

(schema/defn cp-to-staging-dir :- File
  "Copy a file to the staging directory"
  [config-dir :- File
   config-file :- File
   destination :- schema/Str]
  (let [out-file (fs/file staging-dir destination
                          (relativize config-dir config-file))
        out-dir (.getParent out-file)]
    (fs/mkdirs out-dir)
    (fs/copy config-file out-file)
    (relativize staging-dir out-file)))

(schema/defn cp-project-config-files :- [File]
  "Copy files from config-dir to the staging directory"
  [lein-project]
  (let [project-config-dir    (get-project-config-dir lein-project)
        project-config-files  (if (fs/directory? project-config-dir)
                                (find-files-recursively project-config-dir))]
    (for [config-file project-config-files]
      (cp-to-staging-dir project-config-dir config-file config-dir))))

(schema/defn cp-system-config-files :- [File]
  "Copy files from system-config-dir to the staging directory"
  [lein-project]
  (let [config-dir    (get-system-config-dir lein-project)
        config-files  (if (fs/directory? config-dir)
                        (find-files-recursively config-dir))]
    (for [config-file config-files]
      (cp-to-staging-dir config-dir config-file system-config-dir))))

(defn get-real-name
  [project-name]
  (str/replace-first project-name #"^pe-" ""))

(defn cp-cli-wrapper-scripts
  [project-name]
  (fs/copy+ (get-staging-template-file "cli-app.erb")
            (fs/file staging-dir "ext" "bin" (str (get-real-name project-name) ".erb"))))

(defn get-out-dir-for-doc-file
  [dep jar-entry]
  (let [proj-name (name (first dep))
        orig-file (File. (.getName jar-entry))
        ;; This is a bit complex; we want to put the doc files into the staging
        ;; dir under `ext/docs/<project-name>/<original-dir-structure-in-project`.
        ;; To build this path we need to find <original-dir-structure-in-project>
        ;; and then remove the first two elements (which will be "ext/docs").
        rel-dir (->> orig-file
                     .getParentFile
                     .toPath
                     .iterator
                     iterator-seq
                     (drop 2)
                     (mapv #(.toString %))
                     (str/join "/")
                     (File.))]
    (fs/file staging-dir "ext" "docs" proj-name rel-dir)))

(defn cp-doc-files
  [lein-project]
  (mapv (partial relativize staging-dir)
        (deputils/cp-files-of-type lein-project "doc"
                                   docs-prefix get-out-dir-for-doc-file)))

(defn deploy-snapshot
  "Given a project map with a snapshot version, deploy to the configured
  snapshots repository and return the version of the deployed artifact. If given
  a map with a non-snapshot version, does nothing and returns nil."
  [lein-project]
  (when (deputils/snapshot-version? (:version lein-project))
    (-> (deploy/deploy lein-project)
      .getArtifacts
      (nth 0)
      .getVersion)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Upstream EZBake config handling

(defn get-local-ezbake-var
  "Get the value of a variable from the local ezbake config (inside of the
  ezbake lein project file."
  [lein-project k default]
  (get-in lein-project
          [:lein-ezbake :vars k]
          default))

(defn get-ezbake-vars
  [lein-project]
  ;; This function should build up a map of variables that are allowed to
  ;; be interpolated into an upstream ezbake config file.  For now, the only one
  ;; we've had a need for is :user.
  {:user (get-local-ezbake-var lein-project :user (:name lein-project))})

(defn interpolate-ezbake-config
  [ezbake-vars s]
  {:pre [(map? ezbake-vars)
         (string? s)]
   :post [(string? %)]}
  ;; TODO: now that we've introduced a dependency on stencil/mustache, we probably
  ;; might as well replace the heredoc-y template stuff elsewhere in this file
  ;; with it as well.
  (stencil/render-string s ezbake-vars))

(defn get-ezbake-value
  [ezbake-vars upstream-ezbake-configs build-target os ezbake-keyword]
  {:pre [(map? ezbake-vars)
         (map? upstream-ezbake-configs)
         (string? build-target)
         (contains? #{:redhat :debian} os)
         (keyword? ezbake-keyword)]}
  (map (partial interpolate-ezbake-config ezbake-vars)
       (mapcat #(get-in % [:ezbake (keyword build-target) os ezbake-keyword])
               (vals upstream-ezbake-configs))))

(defn add-ezbake-config-to-map
  [acc [proj-name config-stream]]
  (assoc acc proj-name (ts/reader->map config-stream)))

(defn get-upstream-ezbake-configs
  [lein-project]
  (let [upstream-config-streams (deputils/file-file-in-jars lein-project "ext/ezbake.conf")]
    (reduce add-ezbake-config-to-map {} upstream-config-streams)))

(defn- prefix-project-name
  [project-name build-target]
  (if (= build-target "pe")
    (str "pe-" (name project-name))
    (name project-name)))

(defn generate-terminus-list
  [dependencies build-target]
  (for [{:keys [project version jar]} dependencies
        :let [terminus-files (get-terminus-files-in jar)]
        :when (not (empty? terminus-files))]
    [(prefix-project-name project build-target) version terminus-files jar]))

(defn cp-terminus-files "Stage all terminus files. Returns a sequence zipping project names and
  their terminus files."
  [dependencies build-target]
  (let [files (generate-terminus-list dependencies build-target)]
    (doseq [[project-name version terminus-files jar] files]
      (lein-main/info (str "Staging terminus files for " project-name " version " version))
      (deputils/cp-files-from-jar terminus-files jar staging-dir))
    ;; Remove the jars from the returned data
    (map (partial take 3) files)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Ephemeral Git Repo functions

(defn get-git-sha
  "Returns a git sha from the git repo in the current working directory"
  []
  (str/trim-newline (:out (exec/exec "git" "rev-parse" "HEAD"))))

(defn get-timestamp-string
  []
  (-> (local-time/format-local-time (local-time/local-now) :date-hour-minute)
      ;; packaging system expects for there to be no colons or dashes after
      ;; the 'x.y.z-' version string prefix
      (str/replace ":" "")
      (str/replace "-" ".")))

(defn generate-git-tag-from-version
  [lein-version timestamp]
  {:pre [(string? lein-version)]
   :post [(string? %)]}
  (if (.endsWith lein-version "-SNAPSHOT")
    (format "%s.%s"
            (str/replace lein-version "-" ".")
            timestamp)
    lein-version))

(defn generate-package-version-from-version
  [lein-version]
  {:pre [(string? lein-version)]
   :post [(string? %)]}
  (if (.endsWith lein-version "-SNAPSHOT")
    (format "%s"
            (str/replace lein-version #"-SNAPSHOT|-" { "-SNAPSHOT" "" "-" "."}))
    lein-version))

(defn generate-package-release-from-version
  [lein-version timestamp]
  {:pre [(string? lein-version)]
   :post [(string? %)]}
  (if (.endsWith lein-version "-SNAPSHOT")
    (format "%s.%s"
            (str/replace lein-version #".*-" "0.1")
            timestamp)
    "1"))

;; TODO: this is a horrible, horrible hack; I can't yet see a good way to
;; let the packaging library know what the version number is without faking
;; up a git tag; it seems like the packaging code is pretty well hard-coded
;; to try to pull this info from git.
(defn create-git-repo
  [lein-project timestamp]
  (lein-main/info "Creating temporary git repo")
  (exec/exec "git" "init" staging-dir)
  (lein-main/info "Adding all files to git repo")
  (staging-dir-git-cmd "add" "*")
  (lein-main/info "Committing git repo")
  (staging-dir-git-cmd "commit" "-m" "'Temporary git repo to house packaging code'")
  (let [git-tag (generate-git-tag-from-version (:version lein-project) timestamp)]
    (lein-main/info "Tagging git repo at" git-tag)
    (staging-dir-git-cmd "tag" "-a" git-tag "-m" "Tag for packaging code")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; File templates

;; create rpm trigger hash
(defn extract-rpm-package-scripts
  [{:keys [package scripts]}]
  {:package (as-ruby-literal package) :scripts (as-ruby-literal scripts)})

;; create debian trigger hash
(defn extract-deb-package-scripts
  [{:keys [interest-name scripts]}]
  {:interest-name (as-ruby-literal interest-name) :scripts (as-ruby-literal scripts)})

(defn make-template-map
  "Construct the map of variables to pass on to the ezbake.rb template"
  [lein-project build-target
   config-files system-config-files cli-app-files bin-files terminus-files
   upstream-ezbake-configs additional-uberjars timestamp]
  (let [termini (for [[name version files] terminus-files]
                  {:name (as-ruby-literal name)
                   :version (as-ruby-literal version)
                   ;; The files are JarFileEntries
                   :files (as-ruby-literal (map #(.getName %1) files))})
        get-val (fn [platform name]
                  (get-ezbake-value (get-ezbake-vars lein-project)
                                    upstream-ezbake-configs
                                    build-target
                                    platform
                                    name))
        val->ruby #(as-ruby-literal (get-val %1 %2))
        get-local #(get-local-ezbake-var lein-project %1 %2)
        local->ruby #(as-ruby-literal (get-local %1 %2))]
    {:project                            (as-ruby-literal (:name lein-project))
     :packaging-version                  (-> (:version lein-project)
                                             generate-package-version-from-version
                                             as-ruby-literal)
     :packaging-release                  (-> (:version lein-project)
                                             (generate-package-release-from-version timestamp)
                                             as-ruby-literal)
     :puppet-platform-version            (local->ruby :puppet-platform-version 7)
     :real-name                          (-> (:name lein-project) get-real-name as-ruby-literal)
     :user                               (local->ruby :user (:name lein-project))
     :numeric-uid-gid                    (local->ruby :numeric-uid-gid nil)
     :group                              (local->ruby :group (:name lein-project))
     :uberjar-name                       (as-ruby-literal (:uberjar-name lein-project))
     :config-files                       (as-ruby-literal (map remove-erb-extension config-files))
     :system-config-files                (as-ruby-literal (map remove-erb-extension system-config-files))
     :cli-app-files                      (as-ruby-literal (map remove-erb-extension cli-app-files))
     :cli-defaults-file                  (as-ruby-literal (remove-erb-extension cli-defaults-filename))
     :bin-files                          (as-ruby-literal (map #(.getName %1) bin-files)) ;; These are JarFileEntries
     :create-dirs                        (local->ruby :create-dirs [])
     :debian-deps                        (val->ruby :debian :dependencies)
     :debian-build-deps                  (val->ruby :debian :build-dependencies)
     :debian-preinst                     (val->ruby :debian :preinst)
     ;; debian-prerm currently unused?
     :debian-prerm                       (val->ruby :debian :prerm)
     :debian-postinst                    (val->ruby :debian :postinst)
     :debian-postinst-install            (val->ruby :debian :postinst-install)
     :debian-install                     (val->ruby :debian :install)
     :debian-pre-start-action            (val->ruby :debian :pre-start-action)
     :debian-post-start-action           (val->ruby :debian :post-start-action)
     :debian-activated-triggers          (local->ruby :debian-activated-triggers [])
     :debian-interested-install-triggers (map extract-deb-package-scripts
                                              (get-local :debian-interested-install-triggers []))
     :debian-interested-upgrade-triggers (map extract-deb-package-scripts
                                              (get-local :debian-interested-upgrade-triggers []))
     :redhat-deps                        (val->ruby :redhat :dependencies)
     :redhat-build-deps                  (val->ruby :redhat :build-dependencies)
     :redhat-preinst                     (val->ruby :redhat :preinst)
     :redhat-postinst                    (val->ruby :redhat :postinst)
     :redhat-postinst-install            (val->ruby :redhat :postinst-install)
     :redhat-install                     (val->ruby :redhat :install)
     :redhat-pre-start-action            (val->ruby :redhat :pre-start-action)
     :redhat-post-start-action           (val->ruby :redhat :post-start-action)
     :terminus-map                       termini
     :replaces-pkgs                      (for [{:keys [package version]}
                                               (get-local :replaces-pkgs [])]
                                           {:package (as-ruby-literal package)
                                            :version (as-ruby-literal version)})
     :redhat-postinst-install-triggers   (map extract-rpm-package-scripts
                                              (get-local :redhat-postinst-install-triggers []))
     :redhat-postinst-upgrade-triggers   (map extract-rpm-package-scripts
                                              (get-local :redhat-postinst-upgrade-triggers []))
     :start-before                       (local->ruby :start-before [])
     :start-after                        (local->ruby :start-after [])
     :reload-timeout                     (-> (get-local :reload-timeout 120)
                                             str as-ruby-literal)
     :start-timeout                      (-> (get-local :start-timeout 300)
                                             str as-ruby-literal)
     :stop-timeout                       (-> (get-local :stop-timeout 60)
                                             str as-ruby-literal)
     :open-file-limit                    (as-ruby-literal (get-local :open-file-limit nil))
     :is-pe-build                        (-> (= "pe" (get-local :build-type "foss"))
                                             as-ruby-literal)
     :main-namespace                     (local->ruby :main-namespace
                                                      "puppetlabs.trapperkeeper.main")
     :java-args                          (local->ruby :java-args
                                                      "-Xmx192m")
     :java-args-cli                      (local->ruby :java-args-cli "")
     :tk-args                            (local->ruby :tk-args "")
     :bootstrap-source                   (-> (get-local :bootstrap-source :bootstrap-cfg)
                                             name as-ruby-literal)
     :logrotate-enabled                  (local->ruby :logrotate-enabled true)
     :additional-uberjars                (as-ruby-literal additional-uberjars)}))

;; TODO: this is wonky; we're basically doing some templating here and it
;; might make more sense to use an actual template for it.  However, I'm a bit
;; leery of introducing another template language since we're already using
;; erb... but I think I'd have to in order to do this from clojure.  The other
;; option would be to implement all of this logic in ruby and just use erb, but
;; then I couldn't use the lein project file format as the way to represent
;; the data for a project (e.g. `configs/jvm-puppet.clj`).  I'd have to use some
;; other config format that ruby could read, and then generate a lein project
;; file from that.  All of these options sound unappealing in their own special
;; ways.
(defn generate-ezbake-config-file!
  [lein-project
   build-target
   config-files
   system-config-files
   cli-app-files
   bin-files
   terminus-files
   upstream-ezbake-configs
   additional-uberjars
   timestamp]
  (lein-main/info "generating ezbake config file")
  (spit
   (fs/file staging-dir "ezbake.rb")
   (stencil/render-string
    (slurp (get-staging-template-file "ezbake.rb.mustache"))
    (make-template-map lein-project
                       build-target
                       config-files
                       system-config-files
                       cli-app-files
                       bin-files
                       terminus-files
                       upstream-ezbake-configs
                       additional-uberjars
                       timestamp))))

(defn generate-project-data-yaml
  [lein-project build-target additional-uberjars]
  (lein-main/info "generating project_data.yaml file")
  (spit
    (fs/file staging-dir "ext" "project_data.yaml")
    (stencil/render-string
      (slurp (get-staging-template-file "project_data.yaml.mustache"))
      {:project (:name lein-project)
       :summary (:description lein-project)
       :description (format "%s (%s)"
                            (:description lein-project)
                            (deputils/generate-manifest-string lein-project))
       :uberjar-name (:uberjar-name lein-project)
       :additional-uberjars (mapv (fn [filename] {:uberjar filename}) additional-uberjars)
       :is-pe-build (format "%s" (= (get-local-ezbake-var lein-project :build-type "foss") "pe"))
       :repo-name (format "%s" (get-local-ezbake-var lein-project :repo-target ""))
       :nonfinal-repo-name (format "%s" (get-local-ezbake-var lein-project :nonfinal-repo-target ""))})))

(schema/defn get-additional-uberjars
  "Returns the list of additional uberjar dependencies from the given lein project"
  [lein-project]
  (get-in lein-project [:lein-ezbake :additional-uberjars]))

(schema/defn resolve-dependency! :- File
  "Resolves a single dependency and returns a File pointing to its jar.

  This has the side effect of fetching the whole dependency tree for the given
  dependency, but we only care about the one jar"
  [[project-symbol version :as dependency-coordinates]
   repositories]
  (lein-main/info "Resolving dependency for " dependency-coordinates)
  (let [project {:dependencies [dependency-coordinates]
                 :repositories repositories}
        ; resolved-dependencies will be a list of jars, one for each dependency
        ; in the project, including the jar for this project
        resolved-dependencies (lein-classpath/resolve-managed-dependencies :dependencies nil project)
        jar-regex (re-pattern (format "%s-%s.jar" (name project-symbol) version))
        jar-file (first (filter #(re-find jar-regex (.getName %))
                                resolved-dependencies))]
    jar-file))

(schema/defn extract-project-from-jar! :- schema/Str
  "Extracts the project.clj file out of the given jar into the destination directory"
  [jar :- File
   destination-dir :- schema/Str]
  (fs/mkdirs destination-dir)
  (let [jar-file (JarFile. jar)
        jar-entry (.getJarEntry jar-file "project.clj")
        project-string (-> jar-file
                           ; Get an input string for project.clj
                           (.getInputStream jar-entry)
                           slurp)
        out-file-path (str destination-dir "/project.clj")]
    (spit out-file-path project-string)
    out-file-path))

(schema/defn build-uberjar-with-dependency-jar! :- schema/Str
  "Package up the dependencies referenced in the supplied project map and
  files within the supplied jar path into a new 'uberjar' file.

  Note: The :uberjar profile is implicitly activated for this task, and cannot
  be deactivated.

  This function is heavily copied from the implementation of
  leiningen.uberjar/uberjar, see:
  https://github.com/technomancy/leiningen/blob/2.7.1/src/leiningen/uberjar.clj#L143-L187.
  The main difference is that whereas the leiningen uberjar function builds a
  new jar file for the immediate project artifact,
  https://github.com/technomancy/leiningen/blob/2.7.1/src/leiningen/uberjar.clj#L167-L172,
  this function uses the jar file passed in as an argument to this function
  instead."
  [project jar]
  (let [scoped-profiles (set (project/pom-scope-profiles project :provided))
        default-profiles (set (project/expand-profile project :default))
        provided-profiles (remove
                           (set/difference default-profiles scoped-profiles)
                           (-> project meta :included-profiles))
        project (project/merge-profiles
                 (project/merge-profiles project [:uberjar]) provided-profiles)
        _ (pom/check-for-snapshot-deps project) ;; bail early if snapshot
        project (update-in project [:jar-inclusions]
                           concat (:uberjar-inclusions project))
        standalone-filename (jar/get-jar-filename project :standalone)]
    (with-open [out (-> standalone-filename
                        (FileOutputStream.)
                        (ZipOutputStream.))]
      (let [whitelisted (select-keys project project/whitelist-keys)
            project (-> (project/unmerge-profiles project [:default])
                        (merge whitelisted))
            deps (->> (lein-classpath/resolve-managed-dependencies
                       :dependencies :managed-dependencies project)
                      (filter #(.endsWith (.getName %) ".jar")))
            jars (cons (io/file jar) deps)]
        (uberjar/write-components project jars out)))
    (lein-main/info "Created" standalone-filename)
    standalone-filename))

(schema/defn build-uberjar-from-coordinates! :- UberjarInfo
  "Build an uberjar from maven coordinates and return a map containing a file
   pointing to the built uberjar and the lein project map that was used to
   create it"
  [repositories
   [project-symbol version :as dependency-coordinates]]
  (let [dependency-jar (resolve-dependency! dependency-coordinates repositories)
        destination-dir (format "%s/%s-%s"
                                additional-uberjar-checkouts-dir
                                (name project-symbol)
                                version)
        project-file (extract-project-from-jar! dependency-jar destination-dir)
        project-map (-> project-file
                        (project/read [:uberjar]))]
    (lein-main/info "Building uberjar for " dependency-coordinates)
    ; When the project.clj file is read by project/read above, it includes the
    ; file's location in the project map that it produces. This is how the
    ; uberjar/uberjar function knows where to create the new uberjar
    (let [uberjar-file (build-uberjar-with-dependency-jar!
                        project-map
                        dependency-jar)]
      {:uberjar uberjar-file
       :dependency-coordinates dependency-coordinates
       :lein-project project-map})))

(schema/defn build-additional-uberjars! :- [UberjarInfo]
  "Builds uberjars from projects specified in the :additional-uberjars section
  of the ezbake config and returns a list of information about the built uberjars.

  For dependency resolution, we use the same list of repositories (under the
  :repositories key in project.clj) as the lein project that ezbake is running
  inside of"
  [lein-project]
  (let [dependencies (get-additional-uberjars lein-project)
        build-fn (partial build-uberjar-from-coordinates! (:repositories lein-project))]
    ; mapv for side effects
    (mapv build-fn dependencies)))

(schema/defn copy-additional-uberjars!
  [uberjar-info :- [UberjarInfo]]
  (lein-main/info "Copying additional uberjars to staging directory")
  (doseq [jar-file (map :uberjar uberjar-info)]
    (let [destination-path (format "%s/%s" staging-dir (fs/base-name jar-file))]
      (fs/copy jar-file destination-path))))

(defmulti action
  (fn [action & args] action))

(defn munge-deps
  "Takes a map generated by deputils/generate-dependency-map and transforms
  it into a map where the keys are the dependencies themselves with sub
  keys of versions and sub-dependencies.

  Example input being:
  {[project-name project-version extra-info] {[dependency-name dependency-version] {}}}
  Which would be transformed into the following map:
  {project-name {'version' project-version 'dependencies' {dependency-name {'version' dependency-version}}}}"
  [deps]
  (into {} (concat (for [[dep dependencies] deps
                         :let [[project-name version] dep
                               munged-dependencies (munge-deps dependencies)
                               metadata (if (empty? munged-dependencies)
                                          {"version" version}
                                          {"version" version "dependencies" munged-dependencies})]]
                     {(str project-name) metadata}))))

(defn generate-build-metadata-files
  [lein-project]
  (let [dep-map (deputils/generate-dependency-map lein-project)
        deps (munge-deps dep-map)
        metadata {"packaging_type" {"ezbake" (get-ezbake-version lein-project)}
                  "version" (get-git-sha)
                  "build_time" (local-time/format-local-time (local-time/local-now) :basic-date-time)
                  "components" deps}
        json-string (json/generate-string metadata {:pretty true})]
    (io/make-parents (fs/file staging-dir "ext" "build_metadata.json"))
    (spit (fs/file staging-dir "ext" "build_metadata.json") json-string)))

(defn generate-dockerfile
  [lein-project additional-uberjars]
  (let [template (slurp (get-staging-template-file "Dockerfile.mustache"))
        docker-config (get-in lein-project [:lein-ezbake :vars :docker])
        uberjar-name (get lein-project :uberjar-name
                          (format "%s-%s-standalone.jar" (:name lein-project) (:version lein-project)))
        classpath (->> (cons uberjar-name additional-uberjars)
                       (map #(str "/src/" %))
                       (str/join ":"))]
    (->> (stencil/render-string template {:classpath classpath
                                          :expose-ports (if-let [ports (seq (:ports docker-config))]
                                                                (apply print-str "EXPOSE" ports))
                                          :base-image (get docker-config :base-image "openjdk:8-jre-alpine")})
         (spit (fs/file staging-dir "Dockerfile")))))

(defmethod action "manifest"
  [_ lein-project build-target]
  (generate-build-metadata-files lein-project))

(defmethod action "stage"
  ;; note that the `lein-project` arg gets shadowed a few lines down to add full
  ;; snapshot versions of dependencies
  [_ lein-project build-target]
  (let [deployed-version (if (and (deputils/snapshot-version? (:version lein-project))
                                  (not (System/getenv "EZBAKE_NODEPLOY")))
                           (deploy-snapshot lein-project)
                           (:version lein-project))
        reproducible? (not (System/getenv "EZBAKE_ALLOW_UNREPRODUCIBLE_BUILDS"))
        ;; Do not copy the contents of :resource-paths from the deps in this list.
        ;; Items should be symbols of the form `namespace/project`, e.g. `puppetlabs/puppetserver`
        exclude-resources-from (get-in lein-project [:lein-ezbake :exclude-resources-from])
        include-resources-dir (get-in lein-project [:lein-ezbake
                                                    :resources
                                                    :include-dir])
        lein-project (update lein-project :dependencies
                             #(deputils/expand-snapshot-versions
                                lein-project % {:reproducible? reproducible?}))]
    (let [template-dir (get-template-file build-target)
          uberjar-name (get lein-project :uberjar-name
                            (format "%s-%s-standalone.jar" (:name lein-project) (:version lein-project)))]
      (uberjar/uberjar lein-project)
      (fs/copy+ (format "%s/%s" "target" uberjar-name)
                (format "%s/%s" staging-dir uberjar-name))
      (cp-template-files template-dir)
      (cp-template-files (get-template-file "global")))
    (let [dependencies    (deputils/get-dependencies-with-jars lein-project)
          non-excluded-deps (remove
                             (fn [item] (some #(= (:project item) %) exclude-resources-from))
                             dependencies)
          config-files    (cp-shared-files non-excluded-deps get-config-files-in)
          config-files    (concat config-files  (cp-project-config-files lein-project))
          system-config-files (cp-system-config-files lein-project)
          _               (cp-shared-files non-excluded-deps get-cli-app-files-in)
          cli-app-files   (->> (str/join "/" [staging-dir "ext" "cli"])
                            fs/list-dir
                            (map #(relativize staging-dir %)))
          bin-files       (cp-shared-files non-excluded-deps get-bin-files-in)
          terminus-files  (cp-terminus-files non-excluded-deps build-target)
          upstream-ezbake-configs (get-upstream-ezbake-configs lein-project)
          additional-uberjar-info (build-additional-uberjars! lein-project)
          additional-uberjar-filenames (map #(fs/base-name (:uberjar %)) additional-uberjar-info)
          timestamp (get-timestamp-string)]
      (cp-shared-files non-excluded-deps get-cli-defaults-files-in)
      (cp-shared-files non-excluded-deps get-build-scripts-files-in)
      (cp-project-build-scripts lein-project)
      (when cli-app-files
        (cp-cli-wrapper-scripts (:name lein-project)))
      (cp-doc-files lein-project)
      (when additional-uberjar-info
        (copy-additional-uberjars! additional-uberjar-info))
      ;; if :include-dir is specified, copy directory's resource files into staging
      (when (some? include-resources-dir)
        (deputils/copy-files-to-dir (get-bin-files-in-directory include-resources-dir) staging-dir include-resources-dir)
        (deputils/copy-files-to-dir (get-build-scripts-files-in-directory include-resources-dir) staging-dir include-resources-dir)
        (deputils/copy-files-to-dir (get-config-files-in-directory include-resources-dir) staging-dir include-resources-dir)
        (deputils/copy-files-to-dir (get-cli-app-files-in-directory include-resources-dir) staging-dir include-resources-dir)
        (deputils/copy-files-to-dir (get-cli-defaults-files-in-directory include-resources-dir) staging-dir include-resources-dir)
        (deputils/copy-files-to-dir (get-terminus-files-in-directory include-resources-dir) staging-dir include-resources-dir))
      (generate-ezbake-config-file! lein-project
                                    build-target
                                    config-files
                                    system-config-files
                                    cli-app-files
                                    bin-files
                                    terminus-files
                                    upstream-ezbake-configs
                                    additional-uberjar-filenames
                                    timestamp)
      (let [project-w-deployed-version (assoc lein-project :version deployed-version)]
        (generate-project-data-yaml project-w-deployed-version build-target additional-uberjar-filenames)
        (generate-manifest-file project-w-deployed-version additional-uberjar-info))
      (generate-build-metadata-files lein-project)
      (generate-dockerfile lein-project additional-uberjar-filenames)
      (create-git-repo lein-project timestamp))))

(defmethod action "docker-build"
  [_ lein-project build-target]
  (action "stage" lein-project build-target)
  (let [image-name (get-in lein-project [:lein-ezbake :vars :docker :image-name] (:name lein-project))
        docker-call ["docker" "build" "." "-t" image-name]]
    (exec/lazy-sh docker-call {:dir staging-dir})))

(defmethod action "build"
  [_ lein-project build-target]
  (action "stage" lein-project build-target)
  (exec/exec "bundle" "install" "--path" ".bundle/gems" "--binstubs" ".bundle/bin" :dir staging-dir)
  (let [downstream-job nil
        rake-call ["bundle" "exec" "rake" "pl:jenkins:trigger_build_local_auth"]]
    (exec/lazy-sh rake-call {:dir staging-dir})))

(defmethod action "local-build"
  [_ lein-project build-target]
  (action "stage" lein-project build-target)
  (exec/exec "bundle" "install" "--path" ".bundle/gems" "--binstubs" ".bundle/bin" :dir staging-dir)
  (let [downstream-job nil
        rake-call ["bundle" "exec" "rake" "pl:local_build"]]
    (exec/lazy-sh rake-call {:dir staging-dir})))

(defmethod action :default
  [action & args]
  (lein-main/abort (str/join \newline ["Unrecognized option:" action])))

(defn init!
  []
  (clean)
  (fs/mkdirs staging-dir)
  (fs/mkdirs additional-uberjar-checkouts-dir)
  (fs/copy+ "./project.clj"
            (format "%s/%s" staging-dir "project.clj")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Validation

(defn validate-local-project-vars!
  "Throws IllegalArgumentException if lein ezbake project vars cannot be validated"
  [lein-project]
  (let [vars (get-in lein-project [:lein-ezbake :vars])]
    (when-let [error (schema/check LocalProjectVars vars)]
      (throw (IllegalArgumentException.
              (str "Invalid lein ezbake project vars for service, schema errors: "
                   error))))))

(defn validate!
  [lein-project]
  (validate-local-project-vars! lein-project))
