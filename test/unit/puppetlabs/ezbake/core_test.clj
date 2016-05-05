(ns unit.puppetlabs.ezbake.core-test
  (:require [clojure.test :refer :all]
            [puppetlabs.ezbake.core :as core]))


(deftest bootstrap-validation-test
  (testing "Invalid :bootstrap-source setting causes error to be thrown")
  (let [dummy-project {:description "Dummy Project",
                       :lein-ezbake {:vars {:bootstrap-source :bad-value}}}]
    (is (thrown-with-msg?
         IllegalArgumentException
         #"Invalid value for setting ':bootstrap-source': \(not \(#\{:services-d :bootstrap-cfg\} :bad-value\)\)"
         (core/action "stage" dummy-project "dummy-build-target")))))
