(ns leiningen.ezbake
  (:require [puppetlabs.ezbake.core :as ezbake-core]
            [leiningen.ezbake-core :as lein-ezbake-core]))

(defn ezbake
  "Create staging directory and build OS packages for trapperkeeper-based
  applications."
  ([project action config-dir]
   ;; Implementation of ezbake-init requires at least one value here.
   (ezbake project action config-dir "bog=us"))

  ([project action config-dir & template-vars]
   ;; Rebind ezbake-core/resource-path using one of the given projects' resource
   ;; paths. The first in the list is typically "dev-resources" so we choose the
   ;; last, which is almost certain a user-specified path.
   ;;
   ;; TODO Establish ezbake configuration parameter that can be used to
   ;; specified the ezbake resource path independently.
   (binding [ezbake-core/resource-path (last (:resource-paths project))]
     (lein-ezbake-core/prepare-resource-dir project)
     (try
       (ezbake-core/ezbake-init action config-dir template-vars)
       (finally
          ;; this is required in order to make the threads started by sh/sh terminate,
          ;; and thus allow the jvm to exit
         (shutdown-agents))))))

