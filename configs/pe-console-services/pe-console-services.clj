(defproject puppetlabs.packages/pe-console-services "{{{pe-console-services-version}}}"
  :description "Release artifacts for console services"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier "{{{pe-classifier-version}}}"]
                 [puppetlabs/classifier-ui "{{{pe-classifier-ui-version}}}"]
                 [puppetlabs/pe-rbac-service "{{{pe-rbac-version}}}"]
                 [puppetlabs/rbac-ui "{{{pe-rbac-ui-version}}}"]
                 [puppetlabs/pe-activity-service "{{{pe-activity-service-version}}}"]
                 [puppetlabs/pe-trapperkeeper-proxy "{{{pe-trapperkeeper-proxy-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "1.1.0"]
                 ;; There is a bug in leiningen that forces us to
                 ;; explicitly reference nrepl if we want it to be
                 ;; included in the uberjar.
                 ;; https://github.com/technomancy/leiningen/issues/1762
                 [org.clojure/tools.nrepl "0.2.3"]]

  :uberjar-name "console-services-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "pe-console-services"
           :group "pe-console-services"
           :build-type "pe"
           :replaces-pkgs [{:package "pe-rubycas-server", :version "1.1.18"}]})
