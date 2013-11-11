(ns vone.query-tests
  (:require [noir.response :as response])
  (:use [vone.models.queries]
        [clojure.test]))

(deftest test-cumulative
  (testing "json works with lazy seq"
           (println
             (response/json {:foo (pmap inc (range 100))}))))

;(deftest test-xhr
  ;(testing "xhr"
    ;(with-redefs [noir.session/get {}]
           ;(println
             ;(xhr "http://invalid.com/url")))))

(deftest test-roadmap-transform
  (testing "roadmap transforms work"
           (println
             (#'vone.models.queries/roadmap-transform
               [["CSX" "TC" 1 1]
                ["CSX" "TC" 1 2]
                ["CSX" "SP" 1 3]
                ["CSX" "SP" 1 4]]))))





