(defproject puppetlabs.packages/classifier "{{{classifier-version}}}"
  :description "Release artifacts for classifier"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier "{{{classifier-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.7.2"]]

  :uberjar-name "classifier-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "classifier"
           :group "classifier"
           :build-type "foss"})
