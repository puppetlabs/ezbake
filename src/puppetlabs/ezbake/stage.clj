(ns puppetlabs.ezbake.stage
  (:require [me.raynes.fs :as fs]
            [leiningen.core.project :as project]))

(def template-dir "./template")
(def staging-dir "./target/staging")

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


(defn -main
  [& args]
  (clean)
  (cp-template-files)
  (let [project-file "./configs/jvm-puppet.clj"
        lein-project (project/read project-file)]
    (cp-project-file project-file)
    (generate-ezbake-config-file lein-project)))