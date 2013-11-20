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

(facts "about services"
       (fact (sprint-span "TC1313") => truthy)
       (fact (sprint-span "TC1313") => truthy)
       (fact (names "StoryStatus") => truthy)
       (fact (names "StoryStatus" (time/now)) => truthy)
       (fact (team-sprints) => truthy)
       (fact (current-sprints) => truthy)
       (fact (velocity "TC Sharks" "TC1313") => truthy)
       (fact (#'vone.views.services/todo-on "TC Sharks" "TC1315" (time/now)) => truthy)
       (fact (burndown "TC Sharks" "TC1313") => truthy)
       (fact (burndownComparison "TC Sharks" "TC1313") => truthy)
       (fact (cumulative "TC Sharks" "TC1313") => truthy)
       (fact (cumulativePrevious "TC Sharks" "TC1313") => truthy)
       (fact (customers "TC Sharks" "TC1313") => truthy)
       (fact (customersNext "TC Sharks" "TC1313") => truthy)
       (fact (participants "TC Sharks" "TC1313") => truthy)
       (fact (participation) => truthy)
       (fact (fabel) => truthy)
       (fact (estimates "TC Sharks" "TC1313") => truthy)
       (fact (stories "TC Sharks" "TC1313") => truthy)
       (fact (defects "TC Sharks" "TC1313") => truthy)
       (fact (testSets "TC Sharks" "TC1313") => truthy)
       (fact (splits "TC Sharks" "TC1313") => truthy)
       (fact (roadmap) => truthy)
       (fact (feedback "TC Sharks" "TC1214") => truthy)
       (fact (members) => truthy)
       (fact (effort) => truthy)
       (fact (failedReview "TC Sharks" "TC1313") => truthy)
       (fact (#'vone.views.services/churn-data "TC Sharks" "TC1313") => truthy)
       (fact (churnStories "TC Sharks" "TC1313") => truthy)
       (fact (churnComparison "TC Sharks" "TC1313") => truthy)
       (fact (defectRate "SP 5.0.0") => truthy)
       (fact (projects) => truthy))


