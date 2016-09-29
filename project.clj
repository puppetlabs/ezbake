(defproject puppetlabs/lein-ezbake "1.0.0-SNAPSHOT"
  :description "A system for building packages for trapperkeeper-based applications"
  :url "https://github.com/puppetlabs/ezbake"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [;; begin version conflict resolution dependencies
                 [org.clojure/tools.reader "1.0.0-beta1"]
                 ;; end version conflict resolution dependencies

                 [me.raynes/fs "1.4.6" :exclusions [org.clojure/clojure]]
                 [me.raynes/conch "0.8.0"]
                 [clj-time "0.6.0"]
                 [prismatic/schema "1.0.4"]

                 [puppetlabs/typesafe-config "0.1.5" :exclusions [org.clojure/clojure]]

                 ;; trapperkeeper pulls in core.cache via core.async.  Since
                 ;; lein pulls in its own (older) version of core.cache,
                 ;; running ezbake as a plugin to another project via lein
                 ;; produces a "Could not locate clojure/data/priority_map__init.class"
                 ;; error.  Seems related to
                 ;; https://github.com/technomancy/leiningen/issues/1563.  Excluding
                 ;; core.cache here appears to avoid the conflict without requiring
                 ;; consuming projects to do the same.
                 [puppetlabs/trapperkeeper "1.5.0" :exclusions [org.clojure/core.cache]]]

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :scm {:name "git" :url "https://github.com/puppetlabs/ezbake"}

  :resource-paths ["resources/"]

  :profiles {:dev {:dependencies [[io.aviso/pretty "0.1.10"]]}}

  :eval-in-leiningen true)
