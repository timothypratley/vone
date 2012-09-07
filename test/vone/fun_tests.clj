(ns vone.fun-tests
  (:require [noir.response :as response])
  (:use [clojure.test]
        [vone.models.queries]))

(deftest test-nil-json
         (is (not= nil (response/json {:foo nil}))))

(deftest test-to
  (is (= (take-to "TC1211" ["TC1210" "TC1211" "TC1212"])
         ["TC1210" "TC1211"])))

(deftest test-after
  (is (= (take-after "TC1211" ["TC1210" "TC1211" "TC1212"])
         ["TC1212"])))

(deftest test-before
  (is (= (take-before "TC1211" ["TC1210" "TC1211" "TC1212"])
         ["TC1210"])))
