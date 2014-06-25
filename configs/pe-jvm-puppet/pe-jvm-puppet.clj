(defproject puppetlabs.packages/pe-jvm-puppet "{{{pe-jvm-puppet-version}}}"
  :description "Release artifacts for pe-jvm-puppet"
  :pedantic? :abort
  :dependencies [[puppetlabs/pe-jvm-puppet-extensions "{{{pe-jvm-puppet-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.5.2"]]

  :uberjar-name "jvm-puppet-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "pe-puppet"
            :group "pe-puppet"
            :build-type "pe"
            :java-args "-Xmx1g"
            }
  )
