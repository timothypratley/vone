(ns vone.services-tests
  (:require [clj-time.core :as time]
            [noir.response :as response]
            [vone.views.services :refer :all]
            [midje.sweet :refer :all]))


(facts "about private functions"
       (fact (#'vone.views.services/take-to 2 [1 2 3]) => [1 2])
       (fact (#'vone.views.services/take-after 2 [1 2 3]) => [3])
       (fact (#'vone.views.services/take-before 2 [1 2 3]) => [1])
       (fact (#'vone.views.services/roadmap-transform
              [["5.0.0" "CSX" "TC" :a 1]
               ["5.0.0" "CSX" "TC" :a 2]
               ["5.0.0" "CSX" "SP" :a 3]
               ["5.0.0" "CSX" "SP" :a 4]])
             => [["Project" "Customer" "Team" :a]
                 ["5.0.0" "CSX" "SP" 7.0]
                 ["5.0.0" "CSX" "TC" 3.0]]))


(defn data-rows [s]
  (and (seq s)
       (> (count s) 1)
       (every? string? (first s))                     ; headers are strings
       (every? seq s)))

(defn pairs [s]
  (and (data-rows s)
       (every? #(= (count %) 2) s)))   ; pairs


(facts "about services"
       (fact (#'vone.views.services/compress [:a :a :b :c :a :c :c :c]) => [:a :b :c :a :c])
       (fact (#'vone.views.services/compress [{:a 1} {:a 2} {:a 2}] :a) => [{:a 1} {:a 2}])
       (fact (#'vone.views.services/index [{:a :A, :b 2} {:a :B :b 3}] :a) => {:A {:a :A, :b 2} :B {:a :B, :b 3}})
       (fact (#'vone.views.services/map-difference {:a 1 :b 2} {:b 3}) => {:a 1})
       (fact (sprint-span "TC1313") => (just {"BeginDate" truthy, "EndDate" truthy}))
       (fact (names "StoryStatus") => seq)
       (fact (names "StoryStatus" (time/now)) => seq)
       (fact (team-sprints) => map?)
       (fact (current-sprints) => seq)
       (fact (velocity "TC Sharks" "TC1313") => pairs)
       (fact (#'vone.views.services/todo-on "TC Sharks" "TC1315" (time/now)) => number?)
       (fact (burndown "TC Sharks" "TC1313") => pairs)
       (fact (burndownComparison "TC Sharks" "TC1313") => data-rows)
       (fact (cumulative "TC Sharks" "TC1313") => data-rows)
       (fact (cumulativePrevious "TC Sharks" "TC1313") => data-rows)
       (fact (customers "TC Sharks" "TC1313") => pairs)
       (fact (customersNext "TC Sharks" "TC1313") => pairs)
       (fact (participants "TC Sharks" "TC1313") => data-rows)
       (fact (participation) => data-rows)
       (fact (fabel) => truthy)
       (fact (estimates "TC Sharks" "TC1313") => #(and (data-rows %) (not (zero? (last (last %))))))
       (fact (stories "TC Sharks" "TC1313") => data-rows)
       (fact (defects "TC Sharks" "TC1313") => data-rows)
       (fact (testSets "TC Sharks" "TC1313") => data-rows)
       (fact (splits "TC Sharks" "TC1313") => data-rows)
       (fact (roadmap) => data-rows)
       (fact (feedback "TC Sharks" "TC1214") => string?)
       (fact (members) => seq)
       (fact (effort) => seq)
       (fact (failedReview "TC Sharks" "TC1313") => pairs)
       (fact (#'vone.views.services/churn-data "TC Sharks" "TC1313") => seq)
       (fact (churnStories "TC Sharks" "TC1313") => data-rows)
       (fact (churnComparison "TC Sharks" "TC1313") => data-rows)
       (fact (defectRate "SP5.0.0") => data-rows)
       (fact (projects) => seq))



