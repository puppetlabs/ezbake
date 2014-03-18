(ns puppetlabs.ezbake.dependency-utils
  (:import (java.util.jar JarFile JarEntry)
           (java.io File))
  (:require [cemerick.pomegranate.aether :as aether]
            [me.raynes.fs :as fs]
            [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Internal, non-API functions

;; NOTE: this is unfortunate, but I couldn't come up with a better solution.
;; we need to walk over all of the dependencies in the lein project file
;; to inspect their jars for content that we need to put into the packaging;
;; default config files, etc.  However, when leinengen parses the project file
;; it automatically adds these dependencies to the dependency list, and these
;; are not at all relevant to our task at hand.
(def exclude-dependencies #{'org.clojure/tools.nrepl
                            'clojure-complete/clojure-complete})

(defn include-dep?
  [dep]
  (let [artifact-id (first dep)]
    (not (contains? exclude-dependencies artifact-id))))

(defn get-relevant-deps
  [lein-project]
  (filter include-dep? (:dependencies lein-project)))

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

(defn find-files-in-jar
  [jar-file prefix]
  {:pre [(instance? JarFile jar-file)]
   :post [(every? #(instance? JarEntry %) %)]}
  (filter #(and (.startsWith (.getName %) prefix)
                (not= prefix (.getName %)))
          (enumeration-seq (.entries jar-file))))

(defn cp-file-from-jar
  "Given the following:

  * file-type-desc: a human readable description of the type of files we're copying,
                    used in logging output
  * jar-file: JarFile we're copying files from
  * out-dir-fn: a function that accepts a lein dependency and a JarEntry,
                and returns a File representing the directory that we should
                copy the jar entry to
  * dep: a single entry from the leinengen dependencies list
  * jar-entry: the JarEntry that we are copying out of the JarFile

  copies the file from the jar to the directory returned by the out-dir-fn,
  and returns the absolute path for that file."
  [file-type-desc jar-file out-dir-fn dep jar-entry]
  {:pre [(string? file-type-desc)
         (instance? JarFile jar-file)
         (ifn? out-dir-fn)
         (vector? dep)
         (instance? JarEntry jar-entry)]
   :post [(instance? File %)]}
  (println "Copying" file-type-desc "file:" (.getName jar-entry))
  (let [file-name (.getName (File. (.getName jar-entry)))
        out-dir   (out-dir-fn dep jar-entry)
        out-file  (fs/file out-dir file-name)]
    (fs/mkdirs (fs/file out-dir))
    (spit out-file
          (slurp (.getInputStream jar-file jar-entry)))
    out-file))

(defn cp-files-for-dep
  [file-type-desc file-prefix out-dir-fn lein-project dep]
  (println "Checking for" file-type-desc "files in dependency:" dep)
  (let [jar-file    (find-maven-jar-file dep lein-project)
        jar-entries (find-files-in-jar jar-file file-prefix)]
    (mapv (partial cp-file-from-jar file-type-desc jar-file out-dir-fn dep) jar-entries)))

(defn get-manifest-string
  [dep]
  (format "%s %s" (name (first dep)) (second dep)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn generate-manifest-string
  [lein-project]
  (let [deps (get-relevant-deps lein-project)]
    (str/join "," (map get-manifest-string deps))))

(defn cp-files-of-type
  [lein-project file-type-desc file-prefix out-dir-fn]
  (let [deps (get-relevant-deps lein-project)]
    (vec (mapcat
           (partial cp-files-for-dep
                    file-type-desc
                    file-prefix
                    out-dir-fn
                    lein-project)
           deps))))