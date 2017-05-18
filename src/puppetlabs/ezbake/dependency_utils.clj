(ns puppetlabs.ezbake.dependency-utils
  (:import (java.util.jar JarFile JarEntry)
           (java.io File))
  (:require [cemerick.pomegranate.aether :as aether]
            [me.raynes.fs :as fs]
            [clojure.string :as str]
            [leiningen.core.main :as lein-main]
            [leiningen.core.classpath :as classpath]))

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
  (-> (filter include-dep? (:dependencies lein-project))
      (classpath/merge-versions-from-managed-coords
       (:managed-dependencies lein-project))))

(defn snapshot-version?
  [version]
  ;; dependencies managed by lein-parent have nil versions so guard against that
  (boolean (and version (re-find #"-SNAPSHOT$" version))))

(defn get-full-snapshot-version
  ([lein-project coords] (get-full-snapshot-version lein-project coords {}))
  ([lein-project coords options]
   (let [reproducible? (get options :reproducible? true)
         full-version (-> (aether/resolve-artifacts
                            :coordinates [coords]
                            :repositories (:repositories lein-project)
                            :mirrors (:mirrors lein-project)
                            :local-repo (:local-repo lein-project))
                        first
                        meta
                        :result
                        .getArtifact
                        .getVersion)
         deployed-snapshot? (not (re-find #"-SNAPSHOT$" full-version))]
     (if (or (not reproducible?) deployed-snapshot?)
       full-version
       (throw
         (IllegalStateException.
           (format
             (str "Could not find a deployed artifact for %s. Undeployed snapshot"
                  " dependencies cannot be used, since that leads to"
                  " unreproducible builds. Please deploy a snapshot artifact"
                  " matching %s to the snapshots repository.")
             (pr-str coords) (second coords))))))))

(defn expand-snapshot-version
  ([lein-project coords] (expand-snapshot-version lein-project coords {}))
  ([lein-project coords options]
   ;; For cases where the -SNAPSHOT is expanded to a full version, the version
   ;; portion of the coordinate (second element in the vector) is replaced but
   ;; all other qualifying metadata in the coordinate, e.g., any :exclusions
   ;; which might be present, is preserved.
   ;;
   ;; For example, if the original coordinate has:
   ;;
   ;;   ['foo "0.0.1-SNAPSHOT" :exclusions ['bar]]
   ;;
   ;; .. then the result after expansion might have:
   ;;
   ;;   ['foo "0.0.1-20170501.100101-123" :exclusions ['bar]]
   (assoc coords 1 (get-full-snapshot-version lein-project coords options))))

(defn expand-snapshot-versions
  ([lein-project dependencies] (expand-snapshot-versions lein-project dependencies {}))
  ([lein-project dependencies options]
   (let [expand-if-snapshot (fn [[_ version :as coords]]
                              (if (snapshot-version? version)
                                (expand-snapshot-version lein-project coords options)
                                coords))]
     ;; throw artifact resolution errors ASAP
     (doall (map expand-if-snapshot dependencies)))))

(defn find-maven-jar-file
  [coords lein-project]
  {:post [(instance? JarFile %)]}
  (-> (aether/resolve-artifacts
        :coordinates [coords]
        :repositories (:repositories lein-project)
        :local-repo (:local-repo lein-project)
        :mirrors (:mirrors lein-project))
      first
      meta
      :file
      (JarFile.)))

(defn find-files-in-dir-in-jar
  [jar-file prefix]
  {:pre [(instance? JarFile jar-file)]
   :post [(every? #(instance? JarEntry %) %)]}
  (filter #(and (.startsWith (.getName %) prefix)
                (not (.isDirectory %)))
          (enumeration-seq (.entries jar-file))))

(defn find-file-in-jar
  [jar-file file-path]
  {:pre [(instance? JarFile jar-file)
         (string? file-path)]
   :post [((some-fn nil? #(instance? JarEntry %)) %)]}
  (.getJarEntry jar-file file-path))

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
  (lein-main/info "Copying" file-type-desc "file:" (.getName jar-entry))
  (let [file-name (.getName (File. (.getName jar-entry)))
        out-dir   (out-dir-fn dep jar-entry)
        out-file  (fs/file out-dir file-name)]
    (fs/mkdirs (fs/file out-dir))
    (spit out-file
          (slurp (.getInputStream jar-file jar-entry)))
    out-file))

(defn cp-files-for-dep
  [file-type-desc file-prefix out-dir-fn lein-project dep]
  (lein-main/info "Checking for" file-type-desc "files in dependency:" dep)
  (let [jar-file    (find-maven-jar-file dep lein-project)
        jar-entries (find-files-in-dir-in-jar jar-file file-prefix)]
    (mapv (partial cp-file-from-jar file-type-desc jar-file out-dir-fn dep) jar-entries)))

(defn get-manifest-string
  [dep]
  (format "%s %s" (str (first dep)) (second dep)))

(defn add-dep-hierarchy-to-string!
  [deps-map sb depth]
  (doseq [d (keys deps-map)]
    (dotimes [_ depth]
      (.append sb "   "))
    (.append sb d)
    (.append sb "\n")
    (when (deps-map d)
      (add-dep-hierarchy-to-string!
        (deps-map d)
        sb
        (inc depth)))))

(defn add-stream-from-jar-to-map
  [lein-project file-path acc dep]
  (let [jar-file (find-maven-jar-file dep lein-project)]
    (if-let [jar-entry (find-file-in-jar jar-file file-path)]
      (let [project-name (name (first dep))]
        (assoc acc project-name (.getInputStream jar-file jar-entry)))
      acc)))

(defn get-stream-from-project-jar
  [lein-project file-path]
  (let [project-jar-file (fs/file (:target-path lein-project)
                                  (str
                                   (:name lein-project)
                                   "-"
                                   (:version lein-project)
                                   ".jar"))]
    (if (fs/exists? project-jar-file)
      (let [project-jar (JarFile. project-jar-file)]
        (if-let [project-jar-entry (find-file-in-jar project-jar
                                                     file-path)]
          (.getInputStream project-jar project-jar-entry)))
      (lein-main/abort
       (format "Unable to find project jar file: '%s'" project-jar-file)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn get-dependencies-with-jars
  "Get a list of maps representing the dependencies in a lein project. Each
  map has :project, :version, and :jar keys."
  [lein-project]
  (let [dependencies (get-relevant-deps lein-project)]
    (for [[project version :as dep] dependencies]
      {:project project
       :version version
       :jar (find-maven-jar-file dep lein-project)})))

(defn cp-files-from-jar
  "Given a list of jar entries, a jar, and a destination dir, copy the files
  out of the jar. This will recreate the directory structure in the jar in the
  destination.

  e.g.
  (copy-files-from-jar [#<JarFileEntry test/file.txt>] my-jar \"destination\")
  will create \"destination/test/file.txt\". "
  [files jar destination]
  {:pre [(every? #(instance? JarEntry %) files)
         (instance? JarFile jar)
         (string? destination)]}
  (doseq [file files]
    (let [filename (.getName file)
          destname (fs/file destination filename)
          destdir (.getParent destname)]
      (fs/mkdirs destdir)
      (spit destname (slurp (.getInputStream jar file))))))

(defn file-file-in-jars
  "Given a lein project file and a path to a file that may exist in the upstream
  jars, return a map whose keys are the project names whose jar contained the
  specified file, and whose values are InputStreams for reading the contents of
  those files."
  [lein-project file-path]
  (let [deps (get-relevant-deps lein-project)
        upstream-jars (reduce (partial add-stream-from-jar-to-map
                                       lein-project
                                       file-path)
                              {}
                              deps)]
    (if-let [stream-from-project (get-stream-from-project-jar lein-project
                                                              file-path)]
      (assoc upstream-jars (:name lein-project) stream-from-project)
      upstream-jars)))

(defn generate-manifest-string
  [lein-project]
  (let [deps (concat [[(symbol (:group lein-project) (:name lein-project))
                       (:version lein-project)]]
               (get-relevant-deps lein-project))]
    (str/join "," (map get-manifest-string deps))))

(defn generate-dependency-tree-string
  [lein-project]
  (let [sb (StringBuilder.)]
    (-> (aether/dependency-hierarchy
          (:dependencies lein-project)
          (aether/resolve-dependencies
            :coordinates (:dependencies lein-project)
            :managed-coordinates (:managed-dependencies lein-project)
            :repositories (:repositories lein-project)
            :local-repo (:local-repo lein-project)
            :mirrors (:mirrors lein-project)))
        (add-dep-hierarchy-to-string! sb 0))
    (.toString sb)))

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
