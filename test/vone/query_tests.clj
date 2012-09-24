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

(deftest test-projections-transform
  (testing "projection transforms work"
           (println
             (projections-transform
               [["CSX" "TC" 1 1]
                ["CSX" "TC" 1 2]
                ["CSX" "SP" 1 3]
                ["CSX" "SP" 1 4]]))))

