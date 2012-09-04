(ns vone.fun-tests
  (:require [noir.response :as response])
  (:use [clojure.test]))

(deftest test-nil-json
         (is (not= nil (response/json {:foo nil}))))

