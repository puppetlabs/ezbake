(defproject puppetlabs.packages/pe-classifier "{{{pe-classifier-version}}}"
  :description "Release artifacts for classifier"
  :pedantic? :abort
  :dependencies [[puppetlabs/classifier "{{{pe-classifier-version}}}"]
                 [puppetlabs/classifier-ui "{{{classifier-ui-version}}}"]
                 [puppetlabs/pe-rbac-service "{{{pe-rbac-service-version}}}"]
                 [puppetlabs/rbac-ui "{{{rbac-ui-version}}}"]
                 ;[puppetlabs/pe-activity-service "{{{pe-activity-service-version}}}"]
                 ;[puppetlabs/pe-trapperkeeper-proxy "{{{pe-trapperkeeper-proxy-version}}}"]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "0.7.0"]]

  :uberjar-name "classifier-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :main puppetlabs.trapperkeeper.main

  :ezbake {:user "pe-classifier"
           :group "pe-classifier"
           :build-type "pe"})
