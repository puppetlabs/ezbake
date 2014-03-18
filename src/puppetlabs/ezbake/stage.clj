(ns puppetlabs.ezbake.stage
  (:import (java.io File))
  (:require [me.raynes.fs :as fs]
            [leiningen.core.project :as project]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clj-time.local :as local-time]
            [puppetlabs.ezbake.dependency-utils :as deputils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Constants

(def template-dir-prefix "./template")
(def staging-dir "./target/staging")
(def shared-config-prefix "ext/config/shared/")
(def docs-prefix "ext/docs/")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Shell Helpers

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; General Staging Helper functions

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

(defn generate-manifest-file
  [lein-project]
  (spit
    (fs/file staging-dir "ext" "ezbake.manifest")
    (format "
This package was built by the Puppet Labs packaging system.

Release package: %s/%s (%s)
Bundled packages: %s
"
            (:group lein-project)
            (:name lein-project)
            (:version lein-project)
            (deputils/generate-manifest-string lein-project))))

(defn rename-redhat-spec-file
  "The packaging framework expects for the redhat spec file to be
  named `<project-name>.spec`, but we have the file on disk as `ezbake.spec`, so
  we need to rename it after it's been copied to the staging dir."
  [lein-project]
  (fs/rename (fs/file staging-dir "ext" "redhat" "ezbake.spec.erb")
             (fs/file staging-dir "ext" "redhat" (format "%s.spec.erb"
                                                         (:name lein-project)))))

(defn get-out-dir-for-shared-config-file
  [dep jar-entry]
  (fs/file staging-dir
           (.getParent (File. (.getName jar-entry)))))

(defn cp-shared-config-files
  [lein-project]
  (deputils/cp-files-of-type lein-project "shared config"
                             shared-config-prefix get-out-dir-for-shared-config-file))

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
  (deputils/cp-files-of-type lein-project "doc"
                             docs-prefix get-out-dir-for-doc-file))

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
            (format "%s (%s)"
                    (:description lein-project)
                    (deputils/generate-manifest-string lein-project))
            (:uberjar-name lein-project))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Main

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
            config-files (cp-shared-config-files lein-project)]
        (cp-doc-files lein-project)
        (cp-project-file project-file)
        (rename-redhat-spec-file lein-project)
        (generate-ezbake-config-file lein-project config-files)
        (generate-project-data-yaml lein-project)
        (generate-manifest-file lein-project)
        (create-git-repo lein-project))
      (finally
        ;; this is required in order to make the threads started by sh/sh terminate,
        ;; and thus allow the jvm to exit
        (shutdown-agents)))))