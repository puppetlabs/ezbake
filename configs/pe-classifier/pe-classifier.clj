(def classifier-version "0.2.2")

(defproject puppetlabs.packages/pe-classifier "0.2.3-SNAPSHOT"
  :description "Release artifacts for classifier"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier ~classifier-version :exclusions [puppetlabs/trapperkeeper-webserver-jetty9]]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.3.4"]]

  :uberjar-name "classifier-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "pe-classifier"
           :group "pe-classifier"
           :build-type "pe"})
