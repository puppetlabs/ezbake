(defproject puppetlabs.packages/pe-puppetdb "{{{pe-puppetdb-version}}}"
  :description "Release artifacts for pe-puppetdb"
  :pedantic? :abort
  :dependencies [[puppetlabs/puppetdb "{{{pe-puppetdb-version}}}"]
                 ;; This is intended to work around what we believe is a
                 ;; leiningen bug. Otherwise tools.nrepl gets stripped out by
                 ;; leiningen: https://github.com/technomancy/leiningen/issues/1762
                 [org.clojure/tools.nrepl "0.2.3"]]

  :uberjar-name "puppetdb-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :ezbake {:user "pe-puppetdb"
           :group "pe-puppetdb"
           :build-type "pe"
           :main-namespace "puppetlabs.puppetdb.main"
           :start-after ["pe-postgresql"]
           :create-varlib true})
