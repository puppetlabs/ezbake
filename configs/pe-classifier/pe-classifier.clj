(def classifier-version "0.2.3")

(defproject puppetlabs.packages/pe-classifier "0.2.4-SNAPSHOT"
  :description "Release artifacts for classifier"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier ~classifier-version]]

  :uberjar-name "classifier-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "pe-classifier"
           :group "pe-classifier"
           :build-type "pe"})
