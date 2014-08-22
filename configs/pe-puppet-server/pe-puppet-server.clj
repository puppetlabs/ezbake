(defproject puppetlabs.packages/pe-jvm-puppet "{{{pe-puppet-server-version}}}"
  :description "Release artifacts for pe-puppet-server"
  :pedantic? :abort
  :dependencies [[puppetlabs/pe-jvm-puppet-extensions "{{{pe-puppet-server-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.6.1"]]

  :uberjar-name "puppet-server-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "pe-puppet"
            :group "pe-puppet"
            :build-type "pe"
            :java-args "-Xmx1g"
            }
  )
