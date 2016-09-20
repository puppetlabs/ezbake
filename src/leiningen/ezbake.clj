(ns leiningen.ezbake
  (:require [puppetlabs.ezbake.core :as ezbake-core]
            [leiningen.ezbake-core :as lein-ezbake-core]))

(defn ezbake
  "Puppetlabs packaging repo interface for trapperkeeper projects.

Create staging directory and build OS packages for trapperkeeper-based
applications. Takes a single positional argument which is the ezbake \"action\"
as defined below.

Actions:
  stage      Generate and stage ezbake artifacts.
  build      Build native packages from staged artifacts.
  "
  ([project action]
   (let [resource-path (get-in project [:lein-ezbake :resources :dir])
         build-target  (ezbake-core/get-local-ezbake-var
                         project :build-type "foss")]
      ;; Rebind ezbake-core/resource-path using one of the given project's resource
      ;; paths. The first in the list is typically "dev-resources". This value can
      ;; be over written in the :resources-path
      (binding [ezbake-core/resource-path
                (if resource-path
                  resource-path
                  ezbake-core/resource-path)]
        (lein-ezbake-core/prepare-resource-dir project)
        (try
          (ezbake-core/validate! project)
          (ezbake-core/init!)
          (ezbake-core/action action project build-target)
          (finally
             ;; this is required in order to make the threads started by sh/sh terminate,
             ;; and thus allow the jvm to exit
            (shutdown-agents)))))))
