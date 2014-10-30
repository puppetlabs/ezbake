(defproject puppetlabs.packages/pe-puppetserver "{{{pe-puppet-server-version}}}"
  :description "Release artifacts for pe-puppetserver"
  :pedantic? :abort
  :dependencies [[puppetlabs/pe-puppet-server-extensions "{{{pe-puppet-server-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.9.0"]]

  :uberjar-name "puppet-server-release.jar"

  ;; JRuby bundles the (un-exploded) BouncyCastle .jars.
  ;; We don't want them in our uberjar,
  ;; since we define our own dependency on BouncyCastle.
  :uberjar-exclusions [#"META-INF/jruby.home/lib/ruby/shared/org/bouncycastle"]

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "pe-puppet"
            :group "pe-puppet"
            :build-type "pe"
            :java-args "-Xms2g -Xmx2g -XX:MaxPermSize=256m"
            }
  )
