(ns puppetlabs.ezbake.stage
  (:import (java.io File InputStreamReader))
  (:require [me.raynes.fs :as fs]
            [leiningen.core.project :as project]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clj-time.local :as local-time]
            [stencil.core :as stencil]
            [puppetlabs.ezbake.dependency-utils :as deputils]
            [puppetlabs.config.typesafe :as ts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Constants

(def template-dir-prefix "./template")
(def staging-dir "./target/staging")
(def shared-config-prefix "ext/config/shared/")
(def docs-prefix "ext/docs/")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Shell / Filesystem Helpers

(defn exec
  [& args]
  (let [result (apply sh/sh args)]
    (when (not= 0 (:exit result))
      (throw (RuntimeException. (str
                                  "Failed to execute shell command:\n\t"
                                  (str/join " " args)
                                  "\n\nOutput:"
                                  (:out result)
                                  (:err result)))))
    result))

(defn staging-dir-git-cmd
  [& args]
  (apply exec "git"
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

(defn get-ezbake-sha
  "Get the commit SHA of the current ezbake working copy, plus an asterisk if the
  working tree is dirty."
  []
  (let [sha     (str/trim (:out (exec "git" "rev-parse" "HEAD")))
        dirty?  (not= "" (str/trim (:out (exec "git" "diff" "--shortstat"))))]
    (str sha (if dirty? "*" ""))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; General Staging Helper functions

(defn clean
  []
  (println "deleting staging directory:" staging-dir)
  (fs/delete-dir staging-dir))

(defn relativize
  "Convert an absolute File to a relative File"
  [base-path absolute-file]
  (let [base-file (if (instance? File base-path)
                    base-path
                    (File. base-path))]
    (-> base-file
        .toURI
        (.relativize (.toURI absolute-file))
        .getPath
        (File.))))

(defn quoted-list
  [l]
  (if (empty? l) "" (format "\"%s\"" (str/join "\",\"" l))))

(defn cp-template-files
  [template-dir]
  (println "copying template files from" (.toString template-dir) "to" staging-dir)
  (fs/copy-dir template-dir staging-dir))

(defn cp-shared-files
  []
  (let [template-dir (fs/file template-dir-prefix "global")]
    (println "copying template files from" (.toString template-dir) "to" staging-dir)
    (doseq [f (fs/glob (fs/file template-dir) "*")]
      (if (fs/directory? f)
        (fs/copy-dir f staging-dir)
        (fs/copy+ f (format "%s/%s" staging-dir (fs/base-name f)))))))

(defn cp-project-file
  [project-file]
  (println "copying ezbake lein packaging project file" project-file)
  (fs/copy project-file (fs/file staging-dir "project.clj")))

(defn generate-manifest-file
  [lein-project]
  (spit
    (fs/file staging-dir "ext" "ezbake.manifest")
    (format "
This package was built by the Puppet Labs packaging system.

EZBake version: %s
Release package: %s/%s (%s)
Bundled packages: %s
"
            (get-ezbake-sha)
            (:group lein-project)
            (:name lein-project)
            (:version lein-project)
            (deputils/generate-manifest-string lein-project))))

;; TODO: The following four functions will not be needed if RE-1533 is resolved
(defn rename-redhat-spec-file
  "The packaging framework expects for the redhat spec file to be
  named `<project-name>.spec`, but we have the file on disk as `ezbake.spec`, so
  we need to rename it after it's been copied to the staging dir."
  [lein-project]
  (fs/rename (fs/file staging-dir "ext" "redhat" "ezbake.spec.erb")
             (fs/file staging-dir "ext" "redhat" (format "%s.spec.erb"
                                                         (:name lein-project)))))

(defn rename-redhat-systemd-file
  "The redhat packaging expects for the redhat spec file to be
  named `<project-name>.spec`, but we have the file on disk as `ezbake.spec`, so
  we need to rename it after it's been copied to the staging dir."
  [lein-project]
  (fs/rename (fs/file staging-dir "ext" "redhat" "ezbake.service.erb")
             (fs/file staging-dir "ext" "redhat" (format "%s.service.erb"
                                                         (:name lein-project)))))

(defn rename-debian-init-file
  "In order for debian to automatically populate the correct pre and post
  scripts for service startup, it expects to find a file named
  `<project-name>.init` to install as the init script. This function renames
  ezbake.init.erb to match that convention."
  [lein-project]
  (fs/rename (fs/file staging-dir "ext" "debian" "ezbake.init.erb")
             (fs/file staging-dir "ext" "debian" (format "%s.init.erb"
                                                         (:name lein-project)))))

(defn rename-debian-default-file
  "In order for debian to automatically populate the correct pre and post
  scripts for service startup, it expects to find a file named
  `<project-name>.default` to install as the defaults file. This function
  renames ezbake.default.erb to match that convention."
  [lein-project]
  (fs/rename (fs/file staging-dir "ext" "debian" "ezbake.default.erb")
             (fs/file staging-dir "ext" "debian" (format "%s.default.erb"
                                                         (:name lein-project)))))

(defn get-out-dir-for-shared-config-file
  [dep jar-entry]
  (let [rel-path (relativize shared-config-prefix (File. (.getName jar-entry)))]
    (if-let [parent (.getParent rel-path)]
      (fs/file staging-dir "ext" "config" "conf.d" parent)
      (fs/file staging-dir "ext" "config" "conf.d"))))

(defn cp-shared-config-files
  [lein-project]
  (mapv (partial relativize staging-dir)
        (deputils/cp-files-of-type lein-project "shared config"
                                   shared-config-prefix
                                   get-out-dir-for-shared-config-file)))

(defn cp-project-config-file
  [project-config-dir config-file]
  (let [out-file (fs/file staging-dir "ext" "config"
                          (relativize project-config-dir config-file))
        out-dir (.getParent out-file)]
    (fs/mkdirs out-dir)
    (fs/copy config-file out-file)
    (relativize staging-dir out-file)))

(defn cp-project-config-files
  [project config-files]
  (let [project-config-dir    (fs/file "." "configs" project "config")
        project-config-files  (find-files-recursively project-config-dir)
        rel-files             (for [config-file project-config-files]
                                (cp-project-config-file project-config-dir config-file))]
    (concat config-files rel-files)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Upstream EZBake config handling

(defn get-local-ezbake-var
  "Get the value of a variable from the local ezbake config (inside of the
  ezbake lein project file."
  [lein-project key default]
  (get-in lein-project [:ezbake key]
          default))

(defn get-ezbake-vars
  [lein-project build-target]
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

(defn get-deps
  [upstream-ezbake-configs build-target os]
  {:pre [(map? upstream-ezbake-configs)
         (string? build-target)
         (contains? #{:redhat :debian} os)]}
  (set (mapcat #(get-in % [:ezbake (keyword build-target) os :dependencies])
               (vals upstream-ezbake-configs))))

(defn get-preinst
  [ezbake-vars upstream-ezbake-configs build-target os]
  {:pre [(map? ezbake-vars)
         (map? upstream-ezbake-configs)
         (string? build-target)
         (contains? #{:redhat :debian} os)]}
  (map (partial interpolate-ezbake-config ezbake-vars)
       (mapcat #(get-in % [:ezbake (keyword build-target) os :preinst])
               (vals upstream-ezbake-configs))))

(defn add-ezbake-config-to-map
  [acc [proj-name config-stream]]
  (assoc acc proj-name (ts/reader->map config-stream)))

(defn get-upstream-ezbake-configs
  [lein-project]
  (let [upstream-config-streams (deputils/file-file-in-jars lein-project "ext/ezbake.conf")]
    (reduce add-ezbake-config-to-map {} upstream-config-streams)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Ephemeral Git Repo functions

(defn get-timestamp-string
  []
  (-> (local-time/format-local-time (local-time/local-now) :date-hour-minute)
      ;; packaging system expects for there to be no colons or dashes after
      ;; the 'x.y.z-' version string prefix
      (str/replace ":" "")
      (str/replace "-" ".")))

(defn generate-git-tag-from-version
  [lein-version]
  {:pre [(string? lein-version)]
   :post [(string? %)]}
  (if (.endsWith lein-version "-SNAPSHOT")
    (format "%s.%s"
            (str/replace lein-version "-" ".")
            (get-timestamp-string))
    lein-version))


;; TODO: this is a horrible, horrible hack; I can't yet see a good way to
;; let the packaging library know what the version number is without faking
;; up a git tag; it seems like the packaging code is pretty well hard-coded
;; to try to pull this info from git.
(defn create-git-repo
  [lein-project]
  (println "Creating temporary git repo")
  (exec "git" "init" staging-dir)
  (println "Adding all files to git repo")
  (staging-dir-git-cmd "add" "*")
  (println "Committing git repo")
  (staging-dir-git-cmd "commit" "-m" "'Temporary git repo to house packaging code'")
  (let [git-tag (generate-git-tag-from-version (:version lein-project))]
    (println "Tagging git repo at" git-tag)
    (staging-dir-git-cmd "tag" "-a" git-tag "-m" "Tag for packaging code")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; File templates

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
(defn generate-ezbake-config-file
  [lein-project build-target config-files]
  (println "generating ezbake config file")
  (let [upstream-ezbake-configs (get-upstream-ezbake-configs lein-project)
        ezbake-vars             (get-ezbake-vars lein-project build-target)]
    (spit
      (fs/file staging-dir "ezbake.rb")
      (stencil/render-string
        (slurp "./staging-templates/ezbake.rb.mustache")
        {:project         (:name lein-project)
         :user            (get-local-ezbake-var lein-project :user
                                                (:name lein-project))
         :group           (get-local-ezbake-var lein-project :group
                                                (:name lein-project))
         :uberjar-name    (:uberjar-name lein-project)
         :config-files    (quoted-list config-files)
         :deb-deps        (quoted-list (get-deps upstream-ezbake-configs build-target :debian))
         :deb-preinst     (quoted-list (get-preinst ezbake-vars upstream-ezbake-configs build-target :debian))
         :redhat-deps     (quoted-list (get-deps upstream-ezbake-configs build-target :redhat))
         :redhat-preinst  (quoted-list (get-preinst ezbake-vars upstream-ezbake-configs build-target :redhat))}))))

(defn generate-project-data-yaml
  [lein-project build-target]
  (println "generating project_data.yaml file")
  (spit
    (fs/file staging-dir "ext" "project_data.yaml")
    (stencil/render-string
      (slurp "./staging-templates/project_data.yaml.mustache")
      {:project       (:name lein-project)
       :summary       (:description lein-project)
       :description   (format "%s (%s)"
                              (:description lein-project)
                              (deputils/generate-manifest-string lein-project))
       :uberjar-name  (:uberjar-name lein-project)
       :is-pe-build   (format "%s" (= (get-local-ezbake-var lein-project :build-type "foss") "pe"))})))

(defn usage
  []
  (str/join \newline ["EZBake can be used to generate native packages suitable for"
                      "consumption or an artifact ready for packaging"
                      ""
                      "Usage: lein run action"
                      ""
                      "Actions:"
                      "  stage <project-name>      Generate and stage ezbake artifacts"
                      "  build <project-name>      Build native packages from staged artifacts"
                      ""]))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defmulti ezbake-action
  (fn [action & params] action))

(defmethod ezbake-action "stage"
  [_ project project-file lein-project build-target template-dir]
  (clean)
  (cp-template-files template-dir)
  (cp-shared-files)
  (let [config-files (cp-shared-config-files lein-project)
        config-files (cp-project-config-files project config-files)]
    (cp-doc-files lein-project)
    (cp-project-file project-file)
    (rename-redhat-spec-file lein-project)
    (rename-redhat-systemd-file lein-project)
    (rename-debian-init-file lein-project)
    (rename-debian-default-file lein-project)
    (generate-ezbake-config-file lein-project build-target config-files)
    (generate-project-data-yaml lein-project build-target)
    (generate-manifest-file lein-project)
    (create-git-repo lein-project)))

; TODO: make PE_VER either command line or config file driven
(defmethod ezbake-action "build"
  [_ project project-file lein-project build-target template-dir]
  (ezbake-action "stage" project project-file lein-project build-target template-dir)
  (exec "rake" "package:bootstrap" :dir staging-dir)
  (if (= build-target "foss")
    (println (:out (exec "rake" "pl:jenkins:uber_build" :dir staging-dir)))
    (println (:out (exec "rake" "pe:jenkins:uber_build" "PE_VER=3.3" :dir staging-dir)))))

(defmethod ezbake-action :default
  [action & params]
  (exit 1 (str/join \newline ["Unrecognized option:" action "" (usage)])))

(defn ezbake-init
  [action project]
  (let [project-file (.toString (fs/file "./configs" project (str project ".clj")))
        lein-project (project/read project-file)
        build-target (get-local-ezbake-var lein-project :build-type "foss")
        template-dir (fs/file template-dir-prefix build-target)]
    (ezbake-action action project project-file lein-project build-target template-dir)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Main

(defn -main
  [& args]
  (if (= (count args) 2)
    (let [action  (first args)
          project (second args)]
      (try
        (ezbake-init action project)
        (finally
          ;; this is required in order to make the threads started by sh/sh terminate,
          ;; and thus allow the jvm to exit
          (shutdown-agents))))
    (println "Incorrect # of arguments. Expected 2, received:" (count args) "\n\n" (usage))))
