(defproject puppetlabs.packages/pe-console-services "{{{pe-console-services-verison}}}"
  :description "Release artifacts for console services"
  :pedantic? :abort
  :dependencies [[puppetlabs/rbac-ui "{{{pe-rbac-ui-version}}}"]
                 [puppetlabs/pe-rbac-service "{{{pe-rbac-version}}}"]
                 [puppetlabs/classifier-ui "{{{pe-classifier-ui-version}}}"]
                 [puppetlabs/classifier "{{{pe-classifier-version}}}"]
                 [puppetlabs/pe-activity-service "{{{pe-activity-service-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.5.1"]]

  :uberjar-name "console-services-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "pe-console-services"
           :group "pe-console-services"
           :build-type "pe"})
