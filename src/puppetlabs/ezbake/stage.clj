(ns puppetlabs.ezbake.stage
  (:import (java.io File)
           (java.util.jar JarFile JarEntry))
  (:require [me.raynes.fs :as fs]
            [leiningen.core.project :as project]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [cemerick.pomegranate.aether :as aether]
            [clj-time.local :as local-time]))

(def template-dir-prefix "./template")
(def staging-dir "./target/staging")
(def shared-config-prefix "ext/config/shared/")
(def docs-prefix "ext/docs/")

;; NOTE: this is unfortunate, but I couldn't come up with a better solution.
;; we need to walk over all of the dependencies in the lein project file
;; to inspect their jars for content that we need to put into the packaging;
;; default config files, etc.  However, when leinengen parses the project file
;; it automatically adds these dependencies to the dependency list, and these
;; are not at all relevant to our task at hand.
(def exclude-dependencies #{'org.clojure/tools.nrepl
                            'clojure-complete/clojure-complete})

(defn find-maven-jar-file
  [coords lein-project]
  {:post [(instance? JarFile %)]}
  (-> (aether/resolve-artifacts
        :coordinates [coords]
        :repositories (:repositories lein-project))
      first
      meta
      :file
      (JarFile.)))

(defn exec
  [& args]
  (let [result (apply sh/sh args)]
    (when (not= 0 (:exit result))
      (throw (RuntimeException. (str
                                  "Failed to execute shell command:\n\t"
                                  (str/join " " args)
                                  "\n\nOutput:"
                                  (:out result)
                                  (:err result)))))))

(defn staging-dir-git-cmd
  [& args]
  (apply exec "git"
         (format "--git-dir=%s" (fs/file staging-dir ".git"))
         (format "--work-tree=%s" staging-dir)
         args))

(defn clean
  []
  (println "deleting staging directory:" staging-dir)
  (fs/delete-dir staging-dir))

(defn cp-template-files
  [template-dir]
  (println "copying template files from" (.toString template-dir) "to" staging-dir)
  (fs/copy-dir template-dir staging-dir))

(defn cp-project-file
  [project-file]
  (println "copying ezbake lein packaging project file" project-file)
  (fs/copy project-file (fs/file staging-dir "project.clj")))

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
  [lein-project config-files]
  (println "generating ezbake config file")
  (spit
    (fs/file staging-dir "ezbake.rb")
(format "
module EZBake
  Config = {
      :project => '%s',
      :uberjar_name => '%s',
      :config_files => [%s],
  }
end
"
        (:name lein-project)
        (:uberjar-name lein-project)
        (format "\"%s\"" (str/join "\",\"" config-files)))))

(defn generate-project-data-yaml
  [lein-project]
  (println "generating project_data.yaml file")
  (spit
    (fs/file staging-dir "ext" "project_data.yaml")
    (format "
---
project: '%s'
author: 'Puppet Labs'
email: 'info@puppetlabs.com'
homepage: 'https://github.com/puppetlabs/ezbake'
summary: '%s'
description: '%s'
version_file: 'version'
# files and gem_files are space separated lists
files: 'ext *.md %s version Rakefile Makefile.erb puppet'
templates:
  - ext/**/*.erb
  - Makefile.erb
tar_excludes: '.gitignore'
gem_files:
gem_require_path:
gem_test_files:
gem_executables:
gem_default_executables:
"
            (:name lein-project)
            (:description lein-project)
            (:description lein-project)
            (:uberjar-name lein-project))))

(defn generate-git-tag-from-version
  [lein-version]
  {:pre [(string? lein-version)]
   :post [(string? %)]}
  (if (.endsWith lein-version "-SNAPSHOT")
    (-> (format "%s-%s"
                lein-version
                (local-time/format-local-time (local-time/local-now) :date-hour-minute))
        ;; git tags cannot contain colons
        (str/replace ":" ""))
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

(defn rename-redhat-spec-file
  "The packaging framework expects for the redhat spec file to be
  named `<project-name>.spec`, but we have the file on disk as `ezbake.spec`, so
  we need to rename it after it's been copied to the staging dir."
  [lein-project]
  (fs/rename (fs/file staging-dir "ext" "redhat" "ezbake.spec.erb")
             (fs/file staging-dir "ext" "redhat" (format "%s.spec.erb"
                                                        (:name lein-project)))))
(defn include-dep?
  [dep]
  (let [artifact-id (first dep)]
    (not (contains? exclude-dependencies artifact-id))))


(defn find-shared-config-files
  [jar-file]
  {:pre [(instance? JarFile jar-file)]
   :post [(every? #(instance? JarEntry %) %)]}
  (filter
    #(and (.startsWith (.getName %) shared-config-prefix)
          (not= shared-config-prefix (.getName %)))
    (enumeration-seq (.entries jar-file))))

(defn cp-shared-config-file
  [jar-file conf-file]
  (println "Copying shared config file:" (.getName conf-file))
  (let [rel-file (File. (.getName conf-file))
        rel-dir (.getParent rel-file)]
    (fs/mkdirs (fs/file staging-dir rel-dir))
    (spit (fs/file staging-dir rel-file)
          (slurp (.getInputStream jar-file conf-file)))
    conf-file))

(defn cp-shared-config-files-for-dep
  [lein-project dep]
  (println "Checking for shared config files in dependency:" dep)
  (let [jar-file (find-maven-jar-file dep lein-project)
        shared-config-files (find-shared-config-files jar-file)]
    (mapv (partial cp-shared-config-file jar-file) shared-config-files)))

(defn cp-shared-config-files
  [lein-project]
  (let [deps (filter include-dep? (:dependencies lein-project))]
    (vec (mapcat (partial cp-shared-config-files-for-dep lein-project) deps))))

;; TODO: these cp-doc-files and cp-shared-config-files functions have a lot of
;; overlap, should probably refactor.
(defn find-doc-files
  [jar-file]
  {:pre [(instance? JarFile jar-file)]
   :post [(every? #(instance? JarEntry %) %)]}
  (filter
    #(and (.startsWith (.getName %) docs-prefix)
          (not= docs-prefix (.getName %)))
    (enumeration-seq (.entries jar-file))))

(defn cp-doc-file
  [proj-name jar-file doc-file]
  (println "Copying doc file:" (.getName doc-file))
  (let [orig-file (File. (.getName doc-file))
        ;; This is a bit complex; we want to put the doc files into the staging
        ;; dir under `ext/docs/<project-name>/<original-dir-structure-in-project`.
        ;; To build this path we need to find <original-dir-structure-in-project>
        ;; and then remove the first two elements (which will be "ext/docs").
        rel-dir  (->> orig-file
                     .getParentFile
                     .toPath
                     .iterator
                     iterator-seq
                     (drop 2)
                     (mapv #(.toString %))
                     (str/join "/")
                     (File.))
        out-dir  (fs/file staging-dir "ext" "docs" proj-name rel-dir)
        out-file (fs/file out-dir (.getName orig-file))]
    (fs/mkdirs out-dir)
    (spit out-file
          (slurp (.getInputStream jar-file doc-file)))
    out-file))

(defn cp-doc-files-for-dep
  [lein-project dep]
  (println "Checking for doc files in dependency:" dep)
  (let [jar-file (find-maven-jar-file dep lein-project)
        doc-files (find-doc-files jar-file)
        proj-name (name (first dep))]
    {(keyword proj-name) (mapv (partial cp-doc-file proj-name jar-file) doc-files)}))

(defn cp-doc-files
  [lein-project]
  (let [deps (filter include-dep? (:dependencies lein-project))]
    (apply merge (mapv (partial cp-doc-files-for-dep lein-project) deps))))

(defn -main
  [& args]
  ;; TODO: these will be configurable and allow us to build other projects besides
  ;; just jvm-puppet, and choose between foss and pe templates
  (let [template-dir (fs/file template-dir-prefix "foss")
        project-file "./configs/jvm-puppet.clj"]
    (try
      (clean)
      (cp-template-files template-dir)
      (let [lein-project (project/read project-file)
            config-files (cp-shared-config-files lein-project)
            doc-files    (cp-doc-files lein-project)]
        (cp-project-file project-file)
        (rename-redhat-spec-file lein-project)
        (generate-ezbake-config-file lein-project config-files)
        (generate-project-data-yaml lein-project)
        (create-git-repo lein-project))
      (finally
        ;; this is required in order to make the threads started by sh/sh terminate,
        ;; and thus allow the jvm to exit
        (shutdown-agents)))))