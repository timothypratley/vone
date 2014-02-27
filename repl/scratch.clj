(ns vone.scratch
  (:require [clj-time.core :as time]
            [clj-diff.core :refer :all]
            [clojure.repl :refer :all]
            [clojure.data]
            [vone.services :refer :all]
            [vone.helpers :refer :all]
            [vone.version-one-request :refer :all]))

(openItems "TC5.0.1")

(as-url "/Data/PrimaryWorkitem"
        {:sel "Team.Name,Number,Name,Estimate,Status.Name,Priority.Name,Timebox.Name"
         :where (str "Scope.Name='TC5.0.1';AssetState='Active';Status.Name!='Accepted'")
         :sort "Team.Name,Number"})

(xhr )


(diff {"Owners" ["Tim"]} {"Owners" ["Tim" "Dwayne"]})
(clojure.data/diff {"Owners" ["Tim"]} {"Owners" ["Tim" "Dwayne"]})
(diff ["Tim"] ["Tim" "Dwayne"])
(clojure.data/diff ["Tim"] ["Tim" "Dwayne"])
(clojure.data/diff ["Tim"] [nil "Dwayne"])
(diff "The quick brown fox" "The brown ")
(diff "The quick brown fox" "The fun quick brown fox box")
(clojure.data/diff (seq "The quick brown fox") (seq "The slow brown fox"))
(type (clojure.set/difference (sorted-set 1 2 3) (sorted-set 2 3 4)))

(conj [1] 2)

(defn discont
  [s]
  (cons (first s)
        (map second (remove (fn [[a b]] (= b (inc a)))
                            (map vector s (rest s))))))

(defn runs
  "Splits a sequence into incremental runs"
  [[a & more]]
  (when a
    (let [run-helper (fn [[aggregate from previous] current]
                       (if (= previous (dec current))
                         [aggregate from current]
                         [(conj aggregate [from previous]) current current]))
          [aggregate from current] (reduce run-helper [[] a a] more)]
      (conj aggregate [from current]))))

(runs [4 5 6 7 8 16 17 18])

(runs [])

(defn bar [st1 st2]
  (let [{added :+ removed :-} (diff st1 st2)
        rems (into {} (for [[start end] (runs removed)]
                        [start (subs st1 start (inc end))]))
        adds (into {} (for [[start & more] added]
                        [(inc start) (apply str more)]))
        rs (set (keys rems))
        as (set (keys adds))
        cs (clojure.set/intersection rs as)
        all (sort (clojure.set/union rs as))]
    (for [k all]
      (cond (cs k) (str "Replaced: " (rems k) " With: " (adds k))
            (rs k) (str "Deleted: " (rems k))
            (as k) (str "Inserted: " (adds k))))))

(bar "The quick brown fox" "T slow brown fox box")

(foo [4 5 6 7 8 16 17 18])

(type (set [1]))
(type (sorted-set 1))

(diff {:a 1 :b 2} {:b 2 :a 1})
(diff (sort {:a 1 :b 2}) (sort {:b 2 :a 1}))

(clojure.data/diff #{:a :b :c} #{:b :c :d})

(str "hi" (seq ["1" "2"]))

(storyFullHistory "B-22668")


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

