(ns puppetlabs.ezbake.stage
  (:require [me.raynes.fs :as fs]
            [leiningen.core.project :as project]
            [clojure.java.shell :as sh]))

(def template-dir "./template")
(def staging-dir "./target/staging")

(defn staging-dir-git-cmd [& args]
  (apply sh/sh "git"
         (format "--git-dir=%s" (fs/file staging-dir ".git"))
         (format "--work-tree=%s" staging-dir)
         args))

(defn clean
  []
  (println "deleting staging directory:" staging-dir)
  (fs/delete-dir staging-dir))

(defn cp-template-files
  []
  (println "copying template files from" template-dir "to" staging-dir)
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
  [lein-project]
  (println "generating ezbake config file")
  (spit
    (fs/file staging-dir "ezbake.rb")
(format "
module EZBake
  Config = {
      :project => '%s',
      :uberjar_name => '%s',
  }
end
"
        (:name lein-project)
        (:uberjar-name lein-project))
    ))


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
files: 'ext *.md %s version Rakefile Makefile puppet'
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

;; TODO: this is a horrible, horrible hack; I can't yet see a good way to
;; let the packaging library know what the version number is without faking
;; up a git tag; it seems like the packaging code is pretty well hard-coded
;; to try to pull this info from git.
(defn create-git-repo
  [lein-project]
  (println "Creating temporary git repo")
  (sh/sh "git" "init" staging-dir)
  (println "Adding all files to git repo")
  (staging-dir-git-cmd "add" "*")
  (println "Committing git repo")
  (staging-dir-git-cmd "commit" "-m" "'Temporary git repo to house packaging code'")
  (println "Tagging git repo at" (:version lein-project))
  (staging-dir-git-cmd "tag" "-a" (:version lein-project) "-m" "Tag for packaging code"))

(defn -main
  [& args]
  (clean)
  (cp-template-files)
  (let [project-file "./configs/jvm-puppet.clj"
        lein-project (project/read project-file)]
    (cp-project-file project-file)
    (generate-ezbake-config-file lein-project)
    (generate-project-data-yaml lein-project)
    (create-git-repo lein-project))
  ;; this is required in order to make the threads started by sh/sh terminate,
  ;; and thus allow the jvm to exit
  (shutdown-agents))