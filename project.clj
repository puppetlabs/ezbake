(defn deploy-info
  [url]
  { :url url
    :username :env/nexus_jenkins_username
    :password :env/nexus_jenkins_password
    :sign-releases false })

(defproject puppetlabs/lein-ezbake "0.2.11-SNAPSHOT"
  :description "A system for building packages for trapperkeeper-based applications"
  :dependencies [[me.raynes/fs "1.4.6" :exclusions [org.clojure/clojure]]
                 [me.raynes/conch "0.8.0"]
                 [clj-time "0.6.0"]
                 [puppetlabs/typesafe-config "0.1.3" :exclusions [org.clojure/clojure]]]

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :deploy-repositories [["releases" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/releases/")]
                        ["snapshots" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/")]]
  :resource-paths ["resources/"]

  :profiles {:dev {:dependencies [[io.aviso/pretty "0.1.10"]]}}

  :eval-in-leiningen true)
