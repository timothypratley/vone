(ns vone.query-tests
  (:require [noir.response :as response])
  (:use [vone.models.queries]
        [vone.views.service]
        [clojure.test]))

(deftest test-cumulative
  (testing "json works with lazy seq"
           (println
             (response/json {:foo (pmap inc (range 100))}))
           (println
             (response/json (datasource nil nil)))))

;(deftest test-xhr
  ;(testing "xhr"
    ;(with-redefs [noir.session/get {}]
           ;(println
             ;(xhr "http://invalid.com/url")))))

