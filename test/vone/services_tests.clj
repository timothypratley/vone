(ns vone.services-tests
  (:require [noir.response :as response]
            [vone.views.services :refer :all]
            [midje.sweet :refer :all]))


(facts "about private functions"
       (fact (#'vone.views.services/take-to 2 [1 2 3]) => [1 2])
       (fact (#'vone.views.services/take-after 2 [1 2 3]) => [3])
       (fact (#'vone.views.services/take-before 2 [1 2 3]) => [1])
       (fact
           (println
             (#'vone.views.services/roadmap-transform
               [["CSX" "TC" 1 1]
                ["CSX" "TC" 1 2]
                ["CSX" "SP" 1 3]
                ["CSX" "SP" 1 4]]))))

(facts "about services"
       (fact (team-sprints) => truthy?)
       (fact (todo-on "TC Sharks" "TC1315" (time/now)) => truthy?)
       (fact (burndown "TC Sharks" "TC1313") => truthy?)
       (fact (burndownComparison "TC Sharks" "TC1313") => truthy?))






