(ns puppetlabs.ezbake.build
  (:require [me.raynes.fs :as fs]
            [leiningen.core.project :as project]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [puppetlabs.ezbake.stage :as stage]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Main

(defn -main
  [& args]
  ;; TODO: these will be configurable and allow us to build other projects besides
  ;; just jvm-puppet, and choose between foss and pe templates
  (let [build-target "foss"
        project      "jvm-puppet"

        template-dir (fs/file stage/template-dir-prefix build-target)
        project-file (.toString (fs/file "./configs" project (str project ".clj")))]
    (try
      (stage/stage-all-the-things build-target project template-dir project-file)
      (stage/exec "rake" "package:bootstrap" :dir stage/staging-dir)
      (println (:out (stage/exec "rake" "pl:jenkins:uber_build" :dir stage/staging-dir)))
      (finally
          ;; this is required in order to make the threads started by sh/sh terminate,
          ;; and thus allow the jvm to exit
          (shutdown-agents)))))
