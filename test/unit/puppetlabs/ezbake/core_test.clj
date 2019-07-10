(ns unit.puppetlabs.ezbake.core-test
  (:require
   [clojure.data]
   [clojure.test :refer :all]
   [puppetlabs.ezbake.core :as core]))

(def dummy-project {:name "dummy"
                    :description "Dummy Project"
                    :version "1"})

(deftest trivial-make-template-map-behavior
  (testing "keyword bootstrap-source value with hyphens is ok"
    (let [expected {:numeric-uid-gid "nil"
                    :debian-prerm ""
                    :config-files ""
                    :main-namespace "puppetlabs.trapperkeeper.main"
                    :debian-interested-upgrade-triggers ()
                    :packaging-version "1"
                    :is-pe-build "false"
                    :reload-timeout "120"
                    :bootstrap-source "bootstrap-cfg"
                    :debian-interested-install-triggers ()
                    :group "dummy"
                    :java-args "-Xmx192m"
                    :terminus-map ()
                    :system-config-files ""
                    :redhat-postinst-upgrade-triggers ()
                    :open-file-limit "nil"
                    :logrotate-enabled true
                    :cli-app-files ""
                    :debian-deps ""
                    :redhat-deps ""
                    :redhat-post-start-action ""
                    :tk-args ""
                    :replaces-pkgs []
                    :start-before ""
                    :start-after ""
                    :redhat-build-deps ""
                    :debian-build-deps ""
                    :bin-files ""
                    :debian-activated-triggers ""
                    :additional-uberjars ""
                    :stop-timeout "60"
                    :create-dirs ""
                    :packaging-release "1"
                    :uberjar-name nil
                    :start-timeout "300"
                    :redhat-preinst ""
                    :redhat-postinst ""
                    :project "dummy"
                    :debian-postinst-install ""
                    :java-args-cli ""
                    :cli-defaults-file "ext/cli_defaults/cli-defaults.sh"
                    :debian-install ""
                    :redhat-postinst-install-triggers ()
                    :redhat-pre-start-action ""
                    :user "dummy"
                    :redhat-install ""
                    :redhat-postinst-install ""
                    :debian-post-start-action ""
                    :debian-pre-start-action ""
                    :debian-preinst ""
                    :real-name "dummy"
                    :debian-postinst ""}]
      ;; Using diff may make it easier (with some additional
      ;; pretty-printing) to spot small differences.
      (is (= [nil nil expected]
             (clojure.data/diff
              expected
              (core/make-template-map dummy-project "dummy-build-target"
                                      [] [] [] [] [] {} []
                                      (core/get-timestamp-string))))))))
