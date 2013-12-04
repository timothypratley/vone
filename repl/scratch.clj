(ns vone.scratch
  (:require [clj-time.core :as time]
            [clojure.repl :refer :all]
            [vone.views.services :refer :all]
            [vone.helpers :refer :all]
            [vone.version-one-request :refer :all]))

(storyChurnHistory "D-19903" "TC Sharks" "TC1316")

(map second (#'vone.views.services/for-sprint "TC Sharks" "TC1313"
                                  (time/minus (time/now) (time/weeks 1))
                                  (time/now)
                                  #'vone.views.services/storys-on))
(#'vone.views.services/churn-data "TC Sharks" "TC1313")

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
(#'vone.views.services/for-sprint "TC Sharks" "TC1313" #'vone.views.services/todo-on)
(burndownComparison "TC Sharks" "TC1313")

(current-sprints)

(defn functions
  "Get the public functions of a namespace"
  [n]
  (filter fn? (ns-publics n)))

(functions 'vone.views.services)
(filter fn? (ns-publics 'vone.views.services))

(defn proutes
  "Returns service routes where arguments are passed in the url path."
  [rest-ns]
  (for [[service-name service] (functions rest-ns)]
    (println service-name)))

(proutes 'vone.views.services)


(for [a [1 2 3]
      b [:a :b :c]
      :let [a 1]]
  a)

(for [arglist (-> #'map meta :arglists)]
  (clojure.string/join "/" (map keyword arglist)))

(meta #'inc)


