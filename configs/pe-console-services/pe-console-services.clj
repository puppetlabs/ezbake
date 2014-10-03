(defproject puppetlabs.packages/pe-console-services "{{{pe-console-services-version}}}"
  :description "Release artifacts for console services"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier "{{{pe-classifier-version}}}"]
                 [puppetlabs/classifier-ui "{{{pe-classifier-ui-version}}}"]
                 [puppetlabs/pe-rbac-service "{{{pe-rbac-version}}}"]
                 [puppetlabs/rbac-ui "{{{pe-rbac-ui-version}}}"]
                 [puppetlabs/pe-activity-service "{{{pe-activity-service-version}}}"]
                 [puppetlabs/pe-trapperkeeper-proxy "{{{pe-trapperkeeper-proxy-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.7.7"]]

  :uberjar-name "console-services-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "pe-console-services"
           :group "pe-console-services"
           :build-type "pe"
           :replaces-pkgs [{:package "pe-rubycas-server", :version "1.1.18"}]})
