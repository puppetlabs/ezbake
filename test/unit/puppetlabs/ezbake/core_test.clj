(ns unit.puppetlabs.ezbake.core-test
  (:require
   [clojure.data]
   [clojure.test :refer :all]
   [puppetlabs.ezbake.core :as core]))

(deftest conversion-to-ruby-literals
  (is (= "nil" (core/as-ruby-literal nil)))
  (is (= "true" (core/as-ruby-literal true)))
  (is (= "false" (core/as-ruby-literal false)))
  (is (= "-1" (core/as-ruby-literal -1)))
  (is (= "0" (core/as-ruby-literal 0)))
  ;; Just using the MAX_VALUES as a convenient way to make the right type...
  (is (= (str Byte/MAX_VALUE) (core/as-ruby-literal Byte/MAX_VALUE)))
  (is (= (str Short/MAX_VALUE) (core/as-ruby-literal Short/MAX_VALUE)))
  (is (= (str Long/MAX_VALUE) (core/as-ruby-literal Long/MAX_VALUE)))
  (is (= (str (bigint 1000000)) (core/as-ruby-literal (bigint 1000000))))
  (is (= (str BigInteger/TEN) (core/as-ruby-literal BigInteger/TEN)))
  (is (= "'foo'" (core/as-ruby-literal "foo")))
  (is (= "'foo \"$1\"'" (core/as-ruby-literal "foo \"$1\"")))
  (is (= "'foo \\'bar\\''" (core/as-ruby-literal "foo 'bar'")))
  (is (= "'foo \\\\bar'" (core/as-ruby-literal "foo \\bar")))
  (is (= "[]" (core/as-ruby-literal [])))
  (is (= "[nil, true, 0, 'foo', [0]]" (core/as-ruby-literal [nil true 0 "foo" [0]])))
  (is (= "[nil, true, 0, 'foo', [0]]" (core/as-ruby-literal '(nil true 0 "foo" [0]))))
  (is (= "[nil, true, 0, 'foo', [0]]" (core/as-ruby-literal (seq [nil true 0 "foo" [0]])))))

(def dummy-project {:name "dummy"
                    :description "Dummy Project"
                    :version "1"})

(deftest trivial-make-template-map-behavior
  (testing "keyword bootstrap-source value with hyphens is ok"
    (let [expected {:numeric-uid-gid "nil"
                    :debian-prerm "[]"
                    :config-files "[]"
                    :main-namespace "'puppetlabs.trapperkeeper.main'"
                    :debian-interested-upgrade-triggers ()
                    :packaging-version "'1'"
                    :is-pe-build "false"
                    :reload-timeout "'120'"
                    :bootstrap-source "'bootstrap-cfg'"
                    :debian-interested-install-triggers ()
                    :group "'dummy'"
                    :java-args "'-Xmx192m'"
                    :terminus-map ()
                    :system-config-files "[]"
                    :redhat-postinst-upgrade-triggers ()
                    :open-file-limit "nil"
                    :logrotate-enabled "true"
                    :cli-app-files "[]"
                    :debian-deps "[]"
                    :redhat-deps "[]"
                    :redhat-post-start-action "[]"
                    :tk-args "''"
                    :replaces-pkgs []
                    :start-before "[]"
                    :start-after "[]"
                    :redhat-build-deps "[]"
                    :debian-build-deps "[]"
                    :bin-files "[]"
                    :debian-activated-triggers "[]"
                    :additional-uberjars "[]"
                    :stop-timeout "'60'"
                    :create-dirs "[]"
                    :packaging-release "'1'"
                    :uberjar-name "nil"
                    :start-timeout "'300'"
                    :redhat-preinst "[]"
                    :redhat-postinst "[]"
                    :project "'dummy'"
                    :debian-postinst-install "[]"
                    :java-args-cli "''"
                    :cli-defaults-file "'ext/cli_defaults/cli-defaults.sh'"
                    :debian-install "[]"
                    :redhat-postinst-install-triggers []
                    :redhat-pre-start-action "[]"
                    :user "'dummy'"
                    :redhat-install "[]"
                    :redhat-postinst-install "[]"
                    :debian-post-start-action "[]"
                    :debian-pre-start-action "[]"
                    :debian-preinst "[]"
                    :real-name "'dummy'"
                    :debian-postinst "[]"}]
      ;; Using diff may make it easier (with some additional
      ;; pretty-printing) to spot small differences.
      (is (= [nil nil expected]
             (clojure.data/diff
              expected
              (core/make-template-map dummy-project "dummy-build-target"
                                      [] [] [] [] [] {} []
                                      (core/get-timestamp-string))))))))
