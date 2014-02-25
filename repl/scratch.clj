(ns vone.scratch
  (:require [clj-time.core :as time]
            [clojure.repl :refer :all]
            [vone.services :refer :all]
            [vone.helpers :refer :all]
            [vone.version-one-request :refer :all]))

(vone-date (time/now))

(count (request-rows "/Data/Story"
                {:sel "Number"
                 :where "Team.Name='TC Sharks';AssetState!='Dead';ChangeDate>'2013-01-01T23:59:59';ChangeDate<'2014-01-01T23:59:59'"})
       )

(count (request-rows "/Data/Defect"
                {:sel "Number"
                 :where "Team.Name='TC Sharks';AssetState!='Dead';ChangeDate>'2013-01-01T23:59:59';ChangeDate<'2014-01-01T23:59:59'"})
       )


(count (request-rows "/Data/Story"
                {:sel "Number"
                 :where "Team.Name='TC Sharks';AssetState!='Dead';ChangeDate>'2012-01-01T23:59:59';ChangeDate<'2013-01-01T23:59:59'"})
       )

(count (request-rows "/Data/Defect"
                {:sel "Number"
                 :where "Team.Name='TC Sharks';AssetState!='Dead';ChangeDate>'2012-01-01T23:59:59';ChangeDate<'2013-01-01T23:59:59'"})
       )


(count (request-rows "/Data/PrimaryWorkitem"
                {:sel "Number"
                 :where "Team.Name='TC Sharks';AssetState!='Dead';SplitTo;ChangeDate>'2013-01-01T23:59:59';ChangeDate<'2014-01-01T23:59:59'"})
       )


(defectRate "TC5.00")

         (as-url "/Data/Actual"
                       {:sel "Member.Name,Value"
                        :where (str "Timebox.Name='TC1401';Team.Name='TC Sharks'")})

(#'vone.services/workitems "SP5.0.0")
(projects)
(filter number? (names "Scope"))
(names "StoryStatus")

(storyChurnHistory "D-19903" "TC Sharks" "TC1316")

(map second (#'vone.services/for-sprint "TC Sharks" "TC1313"
                                  (time/minus (time/now) (time/weeks 1))
                                  (time/now)
                                  #'vone.services/storys-on))
(#'vone.services/churn-data "TC Sharks" "TC1313")

(churnStories "TC Sharks" "TC1316")

(let [team "TC Sharks"
      sprint "TC1313"]
  (as-url "/Hist/PrimaryWorkitem"
          {:asof (tostr-date (time/now))
           :sel "Number,Name,Estimate,ChangedBy"
           :where (str "Timebox.Name='" sprint
                       "';Team.Name='" team
                       "';AssetState!='Dead'")}))

(churnStories "TC Sharks" "TC1316")
(cumulative "TC Sharks" "TC1313")

(team-sprints)

(sprint-span "TC1313")
(#'vone.services/for-sprint "TC Sharks" "TC1313" #'vone.services/todo-on)
(burndownComparison "TC Sharks" "TC1313")

(current-sprints)

(defn functions
  "Get the public functions of a namespace"
  [n]
  (filter fn? (ns-publics n)))

(functions 'vone.services)
(filter fn? (ns-publics 'vone.services))

(defn proutes
  "Returns service routes where arguments are passed in the url path."
  [rest-ns]
  (for [[service-name service] (functions rest-ns)]
    (println service-name)))

(proutes 'vone.services)


(for [a [1 2 3]
      b [:a :b :c]
      :let [a 1]]
  a)

(for [arglist (-> #'map meta :arglists)]
  (clojure.string/join "/" (map keyword arglist)))

(meta #'inc)


(members 2013)

(workitems "Tim Pratley")

