(defproject puppetlabs.packages/jvm-puppet "0.1.1"
  :description "Release artifacts for jvm-puppet"
  :pedantic? :abort
  :dependencies [[puppetlabs/jvm-puppet "0.1.1"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.5.1"]]

  :uberjar-name "jvm-puppet-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "puppet"
            :group "puppet"
            :build-type "foss"
            }
  )
