(defproject puppetlabs.packages/pe-puppetserver "{{{pe-puppet-server-version}}}"
  :description "Release artifacts for pe-puppetserver"
  :pedantic? :abort
  :dependencies [[puppetlabs/pe-puppet-server-extensions "{{{pe-puppet-server-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.7.4"]]

  :uberjar-name "jvm-puppet-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "pe-puppet"
            :group "pe-puppet"
            :build-type "pe"
            :java-args "-Xms2g -Xmx2g"
            }
  )
