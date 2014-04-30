(def jvm-puppet-version (or (System/getenv "NEXUS_VERSION") "0.1.1"))

(defproject puppetlabs.packages/pe-jvm-puppet jvm-puppet-version
  :description "Release artifacts for pe-jvm-puppet"
  :pedantic? :abort
  :dependencies [[puppetlabs/jvm-puppet ~jvm-puppet-version]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.3.4"]]

  :nexus-version ~jvm-puppet-version
  :uberjar-name "jvm-puppet-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "pe-puppet"
            :group "pe-puppet"
            :build-type "pe"
            }
  )
