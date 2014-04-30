(def jvm-puppet-version (or (System/getenv "NEXUS_VERSION") "0.1.1"))

(defproject puppetlabs.packages/jvm-puppet jvm-puppet-version
  :description "Release artifacts for jvm-puppet"
  :pedantic? :abort
  :dependencies [[puppetlabs/jvm-puppet ~jvm-puppet-version]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.5.1"]]

  :nexus-version ~jvm-puppet-version
  :uberjar-name "jvm-puppet-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "puppet"
            :group "puppet"
            :build-type "foss"
            }
  )
