(defproject puppetlabs.packages/jvm-puppet "{{{jvm-puppet-version}}}"
  :description "Release artifacts for jvm-puppet"
  :pedantic? :abort
  :dependencies [[puppetlabs/jvm-puppet "{{{jvm-puppet-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.5.2"]]

  :uberjar-name "jvm-puppet-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "puppet"
            :group "puppet"
            :build-type "foss"
            :java-args "-Xms2g -Xmx2g"
            }
  )
