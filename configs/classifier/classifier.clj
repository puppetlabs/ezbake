(defproject puppetlabs.packages/classifier "{{{classifier-version}}}"
  :description "Release artifacts for classifier"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier "{{{classifier-version}}}"]
                 [puppetlabs/pe-rbac-service "{{{pe-rbac-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.7.2"]
                 ;; There is a bug in leiningen that forces us to
                 ;; explicitly reference nrepl if we want it to be
                 ;; included in the uberjar.
                 ;; https://github.com/technomancy/leiningen/issues/1762
                 [org.clojure/tools.nrepl "0.2.3"]]

  :uberjar-name "classifier-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "classifier"
           :group "classifier"
           :build-type "foss"})
