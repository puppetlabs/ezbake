(ns leiningen.ezbake-core
  (:import (java.util.jar JarEntry JarFile))
  (:require [clojure.java.io :as io]
            [puppetlabs.ezbake.core :as core]
            [puppetlabs.ezbake.dependency-utils :as deputils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Constants

(def project-resource-path
  "META-INF/leiningen/puppetlabs/lein-ezbake/project.clj")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Jar Helpers

(defn- get-jar-file
  "Return JarFile of the lein-ezbake project."
  []
  (->> (io/resource project-resource-path)
       .getPath
       (re-find #":(.*)!")
       second
       JarFile.))

(defn- copy-jar-resources
  [project]
  (let [jar-file (get-jar-file)
        jar-entries (deputils/find-files-in-dir-in-jar
                      jar-file
                      core/resource-prefix)]
    (deputils/cp-files-from-jar jar-entries jar-file core/resource-path)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Consumable API

(defn prepare-resource-dir
  "Prepare a local resource directory for use by ezbake core when populating the
  packaging staging directory.

  TODO: Add configuration option to clone a git repository containing ezbake
  resources instead of pulling them from the lein-ezbake jar."
  [project]
  (copy-jar-resources project))
