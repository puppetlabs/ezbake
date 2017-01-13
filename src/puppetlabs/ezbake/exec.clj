(ns puppetlabs.ezbake.exec
  (:require [me.raynes.conch :as conch]
            [leiningen.core.main :as lein-main]
            [clojure.string :as str]
            [clojure.java.shell :as sh]))

(defn lazy-sh
  "Call given program and args in a thread, print each line of output from
  program as it is emitted until the subprocess exits.

  If subprocess exits with exit code zero, abort leiningen task with information
  about failed program call.
  If subprocess exits with non-zero exit code, abort the leiningen task."
  [p-args options]
  (binding [conch/*throw* false]
    (conch/let-programs [program (first p-args)]
      (let [args (rest p-args)
            print-fn (fn [m _] (lein-main/info m))
            options (merge options {:out print-fn
                                    :err print-fn
                                    :verbose true})
            result (apply program (flatten [args options]))
            exit-code (get-in result [:exit-code])]
        (when-not (= 0 @exit-code)
          (lein-main/abort
            (format "Subprocess command failed with non-zero exit code\n %s"
                    (str/join " " p-args))))
        (:proc result)))))

(defn exec
  "Exec given arguments as subprocess, if subprocess fails then call lein-main/abort."
  [& args]
  (let [result (apply sh/sh args)]
    (when (not= 0 (:exit result))
      (lein-main/abort
        (str "Failed to execute shell command:\n\t"
             (str/join " " args)
             "\n\nOutput:"
             (:out result)
             (:err result))))
    result))

