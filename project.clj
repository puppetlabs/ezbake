(defproject puppetlabs/lein-ezbake "2.5.0"
  :description "A system for building packages for trapperkeeper-based applications"
  :url "https://github.com/puppetlabs/ezbake"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[me.raynes/fs "1.4.6" :exclusions [org.clojure/clojure]]
                 [me.raynes/conch "0.8.0"]
                 [clj-time "0.6.0"]
                 [cheshire "5.7.1"]
                 [prismatic/schema "1.0.4"]
                 [puppetlabs/typesafe-config "0.1.3" :exclusions [org.clojure/clojure]]]

  :repositories [["releases" "https://artifactory.delivery.puppetlabs.net/artifactory/clojure-releases__local/"]
                 ["snapshots" "https://artifactory.delivery.puppetlabs.net/artifactory/clojure-snapshots__local/"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :scm {:name "git" :url "https://github.com/puppetlabs/ezbake"}

  :resource-paths ["resources/"]

  :profiles {:dev {:dependencies [[io.aviso/pretty "0.1.10"]]}}

  :eval-in-leiningen true)
