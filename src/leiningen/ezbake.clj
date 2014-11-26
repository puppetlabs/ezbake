(ns leiningen.ezbake
  (:require [puppetlabs.ezbake.core :as ezbake-core]
            [leiningen.ezbake-core :as lein-ezbake-core]))

(defn ezbake
  "Create staging directory and build OS packages for trapperkeeper-based
  applications."
  ([project action]
   ;; Rebind ezbake-core/resource-path using one of the given project's resource
   ;; paths. The first in the list is typically "dev-resources". This value can
   ;; be over written in the :resources-path
   (let [ezbake-map (:lein-ezbake project)
         resource-path (:resource-path ezbake-map)]
     (binding [ezbake-core/resource-path
               (if resource-path
                 resource-path
                 (first (:resource-paths project)))]
       (lein-ezbake-core/prepare-resource-dir project)
       (try
         (ezbake-core/ezbake-init project action)
         (finally
            ;; this is required in order to make the threads started by sh/sh terminate,
            ;; and thus allow the jvm to exit
           (shutdown-agents)))))))

