(defproject puppetlabs.packages/puppetserver "{{{puppet-server-version}}}"
  :description "Release artifacts for puppet-server"
  :pedantic? :abort
  :dependencies [[puppetlabs/puppet-server "{{{puppet-server-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.8.1"]]

  :uberjar-name "puppet-server-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "puppet"
            :group "puppet"
            :build-type "foss"
            :java-args "-Xms2g -Xmx2g -XX:MaxPermSize=256m"
            }
  )
