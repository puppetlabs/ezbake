(defproject puppetlabs.packages/puppetdb "{{{puppetdb-version}}}"
  :description "Release artifacts for PuppetDB"
  :pedantic? :abort
  :dependencies [[puppetlabs/puppetdb "{{{puppetdb-version}}}"]
                 ;; This is intended to work around what we believe is a
                 ;; leiningen bug. Otherwise tools.nrepl gets stripped out by
                 ;; leiningen: https://github.com/technomancy/leiningen/issues/1762
                 [org.clojure/tools.nrepl "0.2.3"]]

  :uberjar-name "puppetdb-release.jar"

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots" {:url "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"
                               :update :always}]]

  :ezbake {:user "puppetdb"
           :group "puppetdb"
           :build-type "foss"
           :main-namespace "puppetlabs.puppetdb.main"
           :create-varlib true})
