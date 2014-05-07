(defproject puppetlabs.packages/pe-classifier "0.3.1-SNAPSHOT"
  :description "Release artifacts for classifier"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier "0.3.1"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.5.1"]]

  :uberjar-name "classifier-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "pe-classifier"
           :group "pe-classifier"
           :build-type "pe"})
