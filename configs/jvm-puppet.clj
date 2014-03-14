(defproject puppetlabs/jvm-puppet-release "0.0.1-SNAPSHOT"
  :description "Release artifacts for jvm-puppet"
  :pedantic? :warn
  :dependencies [[puppetlabs/jvm-puppet "0.0.2-SNAPSHOT"]]

  :uberjar-name "jvm-puppet-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  )