(defproject puppetlabs.packages/puppetserver "{{{puppet-server-version}}}"
  :description "Release artifacts for puppet-server"
  :pedantic? :abort
  :dependencies [[puppetlabs/puppet-server "{{{puppet-server-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "1.1.0"]
                 ;; There is a bug in leiningen that forces us to
                 ;; explicitly reference nrepl if we want it to be
                 ;; included in the uberjar.
                 ;; https://github.com/technomancy/leiningen/issues/1762
                 [org.clojure/tools.nrepl "0.2.3"]]

  :uberjar-name "puppet-server-release.jar"

  ;; JRuby bundles the (un-exploded) BouncyCastle .jars.
  ;; We don't want them in our uberjar,
  ;; since we define our own dependency on BouncyCastle.
  :uberjar-exclusions [#"META-INF/jruby.home/lib/ruby/shared/org/bouncycastle"]

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]
  :main puppetlabs.trapperkeeper.main

  :ezbake { :user "puppet"
            :group "puppet"
            :start-timeout "120"
            :build-type "foss"
            :java-args "-Xms2g -Xmx2g -XX:MaxPermSize=256m"
            }
  )
