(ns vone.models.queries
  (:use [vone.models.version-one-request]
        [clojure.pprint]
        [clojure.set]
        [vone.helpers])
  (:require [ring.util.codec :as codec]
            [clj-time.core :as time]))


;TODO: cache answers
;
(defn- two-dec
  [d]
  (/ (Math/round (* 100.0 d)) 100.0))

(defn- ratio
  [a b]
  (if (zero? b)
    0
    (two-dec (/ a b))))

(def ^:private lt (codec/url-encode "<"))
(def ^:private gt (codec/url-encode ">"))
(def ^:private gte (codec/url-encode ">="))

(defn- ff
  "format fields for a query string"
  [fields]
  (apply str (interpose \, fields)))

(defn sprint-span
  [sprint]
  (let [query (str "/Data/Timebox?sel=BeginDate,EndDate&where=Name='" sprint \')]
    (request-transform query first)))

(defn names
  "Retrieves a sorted seqence of asset names"
  ([asset]
   (let [query (str "/Data/" asset "?sel=Name")]
     (map first (request-flat query ["Name"]))))
  ([asset asof]
   (let [query (str "/Hist/" asset
                    "?asof=" (tostr-date asof)
                    "&sel=Name&where=Description;AssetState!='Dead'&sort=Order")]
     (map first (request-flat query ["Name"])))))

(defn- setify
  "Creates sets from a collection entities (to remove duplicates)"
  [key-selector value-selector c]
  (into {} (for [entity c
                 :when (not (number? (get entity value-selector)))]
             [(get entity key-selector)
              (apply sorted-set-by #(compare %2 %1) ;descending
                   (get entity value-selector))])))

;TODO: could very well calculate the velocity here also!
(defn team-sprints
  "Retrieves all teams and the sprints they have particpated in"
  []
  (let [sprint "Workitems:PrimaryWorkitem.Timebox.Name"
        team "Name"
        fields [team sprint]
        query (str "/Data/Team?sel=" (ff fields) "&sort=Name")]
    (request-transform query (partial setify team sprint))))

(defn current-sprints
  "Retrieves current sprints"
  []
  (let [fields ["Name"]
        now (time/now)
        now-str (tostr-date now)
        query (str "/Data/Timebox?sel=" (ff fields)
                   "&where=EndDate" gte \' now-str \'
                   ";BeginDate" lt \' now-str \'
                   ";AssetState!='Dead'&sort=Name")]
    (flatten (request-flat query fields))))

;TODO: this is getting called multiple times, it doesn't need to be
(defn- velocity-all
  [team]
  (let [sum-story-points (str "Workitems:PrimaryWorkitem[Team.Name='" (codec/url-encode team)
                              "';AssetState!='Dead'].Estimate.@Sum")
        fields ["Name" sum-story-points]
        query (str "/Data/Timebox?sel=" (ff fields)
                   "&where=" sum-story-points gt "'0'&sort=EndDate")]
    (request-flat query fields)))

(def ^:private not-pos? (complement pos?))
(def ^:private not-neg? (complement neg?))
(defn- count-to
  [sprint sprints]
  (inc (.indexOf sprints sprint)))
(defn- take-before
  [sprint sprints]
  (take-while (partial not= sprint) sprints))
(defn- take-to
  [sprint sprints]
  (take (count-to sprint sprints) sprints))
(defn- take-after
  [sprint sprints]
  (drop 1 (drop-while (partial not= sprint) sprints)))

(defn velocity
  "The total story points done for the last 5 sprints"
  [team sprint]
  (let [sprints (velocity-all team)
        c (count-to sprint (map first sprints))]
    (cons ["Sprint" "Story Points"]
          (take-last 5
                     (take c sprints)))))

(defn- sprints
  "Get the name of all sprints that a particular team has done work in"
  [team]
  (map first (velocity-all team)))

(defn- for-period
  [begin end f]
  (let [days (take-while #(time/before? % end)
                         (filter (complement weekend?)
                                 (inc-date-stream begin)))]
     ;google app engine does not allow pmap,
     ;so use regular map when deploying there
     (pmap f days)))

(defn- for-sprint
  "Queries f for each sprint day"
  ([team sprint f]
   (let [span (sprint-span (codec/url-encode sprint))
         begin (time/plus (span "BeginDate") (time/days 1))
         end (span "EndDate")]
     (println "TEAM:" team " SPRINT:" sprint " START:" begin "  END:" end)
     (for-sprint team sprint begin end f)))
  ([team sprint begin end f]
   (let [team (codec/url-encode team)
         sprint (codec/url-encode sprint)]
     (for-period begin end (partial f team sprint)))))

(defn- singular
  "Get the value from a collection of one map with only one key"
  [col]
  (-> col first vals first))

(defn- todo-on
  "Get the todo hours on a particular day"
  [team sprint date]
  (let [query (str "/Hist/Timebox?asof=" (tostr-date date)
                   "&where=Name='" sprint
                   "'&sel=Workitems[Team.Name='" team
                   "';AssetState!='Dead'].ToDo.@Sum")]
    (request-transform query singular)))

;TODO: There is no need to get a burndown if we have the comparison? would have to manage data clientside though.
(defn burndown
  "Gets a table of todo hours per day"
  [team sprint]
  (cons ["Day" "ToDo"]
        (map list (iterate inc 1)
             (for-sprint team sprint todo-on))))

(defn burndownComparison
  "Gets a table of the past 4 sprints burndowns"
  [team sprint]
  (let [s (->> (sprints team)
            (take-to sprint)
            (take-last 4)
            reverse)]
    (if (not-empty s)
      (cons (cons "Day" s)
            (map cons (iterate inc 1)
                 (apply map list (map #(for-sprint team % todo-on) s)))))))

(defn- map-add
  [m k v]
  (update-in m [k] (fnil + 0) v))

(defn- summize
  "Given a collection of maps,
   sums up values in those maps retrieved with value-selector
   by making keys retrieved with key-selector,
   returning a map of the totals"
  [key-selector value-selector c]
  (letfn [(sum-by-key [m entity]
            (let [k (entity key-selector)
                  ;TODO: is there a nicer way to deal with empty values?
                  k (if (number? k) "None" k)]
               (map-add m k (entity value-selector))))]
    (reduce sum-by-key (sorted-map) c)))

(defn- cumulative-on
  [team sprint date]
  (let [query (str "/Hist/PrimaryWorkitem?asof=" (tostr-date date)
                   "&where=Timebox.Name='" sprint
                   "';Team.Name='" team
                   "';AssetState!='Dead'&sel=Status.Name,Estimate")]
    (request-transform query (partial summize "Status.Name" "Estimate"))))

(defn cumulative
  "Gets a table of the cumulative flow (story points by status per day)"
  [team sprint]
  (let [span (sprint-span (codec/url-encode sprint))
        begin (time/plus (span "BeginDate") (time/days 1))
        end (span "EndDate")
        results (for-sprint team sprint begin end cumulative-on)
        statuses (reverse (names "StoryStatus" end))
        statuses (concat statuses
                         (clojure.set/difference
                           (set (keys (apply merge results)))
                           (set statuses)))
        status-points (fn [m] (map #(get m % 0) statuses))]
    (cons (cons "Day" statuses)
          (map cons (iterate inc 1)
               (map status-points results)))))

(defn cumulativePrevious
  "Gets a table of the previous sprint cumulative flow"
  [team sprint]
  (if-let [previous (last (take-before sprint (sprints team)))]
    (cumulative team previous)))

(defn customers
  "Gets a table of story points per customer"
  [team sprint]
  (cons ["Customer" "Story Points"]
	  ; TODO: where should url-encode happen?
	  (let [query (str "/Data/PrimaryWorkitem?where=Timebox.Name='" (codec/url-encode sprint)
	                   "';Team.Name='" (codec/url-encode team)
	                   "';AssetState!='Dead'&sel=Estimate,Parent.Name")]
	    (request-transform query (partial summize "Parent.Name" "Estimate")))))

(defn customersNext
  "Gets a table of story points per customer for the next sprint"
  [team sprint]
  (if-let [next (first (take-after sprint (sprints team)))]
    (customers team next)))

;TODO: query team instead, don't get people not in the team
(defn- participants-all
  [team sprint]
  (let [fields ["Owners.Name"]
        query (str "/Data/Workitem?sel=" (ff fields)
                   "&where=Timebox.Name='" (codec/url-encode sprint)
                   "';Team.Name='" (codec/url-encode team)
                   "';AssetState!='Dead'")]
                  ;TODO: is there a nicer way to deal with empty values
                  ; rather than number?
    (set (remove number? (flatten (request-flat query fields))))))

(defn- map-add-estimates
  [m estimate-owners]
  (let [estimate (first estimate-owners)
        owners (second estimate-owners)]
    ; TODO: nil is replaced by 0 when there are no owners
    (if (not (number? owners))
      (reduce #(map-add %1 %2 estimate) m owners)
      m)))

(defn- map-add-hours
  [m owner-hours]
  (let [owner (first owner-hours)
        hours (second owner-hours)]
    (map-add m owner hours)))

;TODO: query team instead, don't get people not in the team
(defn participants
  "Retrieves a table of effort recorded per participant"
  [team sprint]
  (let [fields ["Estimate" "Owners.Name"]
        query (str "/Data/PrimaryWorkitem?sel=" (ff fields)
                   "&where=Timebox.Name='" (codec/url-encode sprint)
                   "';Team.Name='" (codec/url-encode team)
                   "';AssetState!='Dead'")
        points (reduce map-add-estimates {} (request-flat query fields))
        fields ["Member.Name" "Value"]
        query (str "/Data/Actual?sel=" (ff fields)
                   "&where=Timebox.Name='" (codec/url-encode sprint)
                   "';Team.Name='" (codec/url-encode team) \')
        hours (reduce map-add-hours {} (request-flat query fields))]
    (cons ["Member" "Points" "Hours"]
          (for [member (apply sorted-set (concat (keys points) (keys hours)))]
            [member (get points member 0) (two-dec (get hours member 0))]))))

(defn- map-add-sprint
  [m estimate-owners]
  (let [estimate (first estimate-owners)
        owners (second estimate-owners)
        sprint (last estimate-owners)]
    ; TODO: nil is replaced by 0 when there are no owners
    (if-not (or (number? owners) (number? sprint))
      (reduce #(update-in %1 [%2 sprint] (fnil + 0) estimate)
              m owners)
      m)))

(defn participation
  "Table of story points per member per sprint"
  []
  (let [fields ["Estimate" "Owners.Name" "Timebox.Name"]
        query (str "/Data/PrimaryWorkitem?sel=" (ff fields)
                   "&where=AssetState!='Dead'")
        points (reduce map-add-sprint {} (request-flat query fields))]
    (cons ["Member" "Sprint" "Points"]
          (apply concat
            (for [member (apply sorted-set (keys points))]
              (let [sprints (points member)]
                (for [sprint (apply sorted-set (keys sprints))]
                  [member sprint (get sprints sprint 0)])))))))

(defn- check-estimate
  [story]
  (let [e (story "Estimate")
        valid-estimate #{0.5 1.0 2.0 3.0 5.0 8.0}]
    (when-not (valid-estimate e)
      (str "Estimate " e " should be one of " (sort valid-estimate)))))

(defn- check-success-criteria
  [story]
  (let [d (story "Description")]
    (when-not (re-find #"(?i)success criteria" d)
      "Description must have success criteria")))

(defn- check-description-length
  [story]
  (let [d (story "Description")]
    (when (> 20 (.length d))
      "Description too short")))

(defn- check-owners
  [story]
  (let [o (story "Owners.Name")
        ;TODO: the evil nil
        o (if (number? o) [] o)]
    (when-not (= 2 (count o))
      (apply str "Should have 2 owners but has " (count o) " " o))))

(defn- check-story
  [story]
  (remove nil?
          (map #(%1 story)
               [check-estimate
                check-success-criteria
                check-description-length
                check-owners])))

;https://www3.v1host.com/Tideworks/story.mvc/Summary?oidToken=Story%3A
;+id
(defn fabel
  "Identifies stories that have invalid data"
  []
  (let [fields ["Number" "Name" "Estimate" "Team.Name"
                "Owners.Name" "Description"
                "Parent.Name"]
        query (str "/Data/Story?sel=" (ff fields)
                   "&where=Timebox.State.Code='ACTV'"
                   ";AssetState!='Dead'")
        result (request-transform query identity)]
    (for [s result
          :let [errors (check-story s)]
          :when errors]
      [(s "Team.Name") (s "Number") (apply vector errors)])))

; TODO: limit query to a sprint
(defn- estimates-all
  [team]
  (let [t (str "[Team.Name='" (codec/url-encode team) "';AssetState!='Dead']")
        sum-point-estimates (str "Workitems:PrimaryWorkitem" t ".Estimate.@Sum")
        tt (str "[Reference!='';Team.Name='" (codec/url-encode team) "';AssetState!='Dead']")
        sum-changeorders (str "Workitems:PrimaryWorkitem" tt ".Estimate.@Sum")
        count-stories (str "Workitems:Story" t ".@Count")
        count-defects (str "Workitems:Defect" t ".@Count")
        count-test-sets (str "Workitems:TestSet" t ".@Count")
        count-test-cases (str "Workitems:Test" t ".@Count")
        sum-hour-estimates (str "Workitems" t ".DetailEstimate.@Sum")
        t (str "[Team.Name='" (codec/url-encode team) "']")
        sum-hour-actuals (str "Actuals" t ".Value.@Sum")
        capacity (str "Capacities" t ".Value.@Sum")
        fields ["Name"
                sum-point-estimates
                sum-changeorders
                count-stories
                count-defects
                count-test-sets
                count-test-cases
                sum-hour-estimates
                sum-hour-actuals
                capacity]
        query (str "/Data/Timebox?sel=" (ff fields)
                   "&where=" sum-point-estimates gt "'0'&sort=EndDate")]
    (request-flat query fields)))

(defn- with-capacity
  [team estimates]
  (for [stats estimates]
    (if (zero? (last stats))
      ;replace capacity with hoursXdaysXpeople for sprint
      (concat (drop-last stats)
              [(* 5 14 (count (participants-all
                                team (first stats))))])
      stats)))

(defn- with-ratios
  [estimates]
  (for [stats estimates]
    (let [estimation (nth stats 6)
          done (nth stats 7)
          capacity (nth stats 8)
          accuracy (ratio estimation done)
          efficiency (ratio done capacity)
          points (nth stats 1)
          referenced (nth stats 2)
          rr (ratio referenced points)]
      (concat stats [accuracy efficiency rr]))))

(defn estimates
  "Gets a table of estimates and statistics"
  [team sprint]
  (let [estimates (estimates-all team)
        c (count-to sprint (map first estimates))
        estimates (take-last 5 (take c estimates))
        estimates (with-capacity team estimates)
        estimates (with-ratios estimates)]
    (if (not-empty estimates)
      (transpose
        (cons ["Sprint"
               "Points"
               "Referenced"
               "Stories"
               "Defects"
               "Test Sets"
               "Tests"
               "Estimated"
               "Done"
               "Capacity"
               "Accuracy (Estimate/Done)"
               "Efficiency (Done/Capacity)"
               "Referenced ratio"]
              estimates)))))

;from incanter
(defn- cumulative-sum
  " Returns a sequence of cumulative sum for the given collection. For instance
    The first value equals the first value of the argument, the second value is
    the sum of the first two arguments, the third is the sum of the first three
    arguments, etc.

    Examples:
      (use 'incanter.core)
      (cumulative-sum (range 100))
  "
  ([coll]
   (loop [in-coll (rest coll)
          cumu-sum [(first coll)]
          cumu-val (first coll)]
     (if (empty? in-coll)
       cumu-sum
       (let [cv (+ cumu-val (first in-coll))]
         (recur (rest in-coll) (conj cumu-sum cv) cv))))))

(defn- accumulate
  [s]
  (reduce #(assoc %1 (first %2) (second %2)) (sorted-map)
         (map list (map first s) (cumulative-sum (map second s)))))

(defn- workitems
  ([]
   (let [fields ["Owners.Name" "Estimate"]
         horizon (time/years 1)
         now (time/now)
         query (str "/Data/PrimaryWorkitem"
                    "?sel=" (ff fields)
                    "&where=Owners[AssetState!='Dead'].@Count"
                    gt "'0';ChangeDate"
                    gt \' (tostr-date (time/minus now horizon))
                    "';AssetState='Closed'")
         result (request-flat query fields)]
     (map #(list (first %) (two-dec (second %)))
          (reduce (fn [m owners-estimate]
                    (let [names (first owners-estimate)
                          estimate (second owners-estimate)]
                      (reduce #(update-in %1 [%2] (fnil + 0) estimate) m names)))
                  (sorted-map) result))))
  ([member asset-type]
   (let [fields ["ChangeDate" "Estimate"]
         horizon (time/years 1)
         now (time/now)
         query (str "/Data/" asset-type
                    "?sel=" (ff fields)
                    "&where=ChangeDate" gt \' (tostr-date (time/minus now horizon))
                    "';Owners[Name='" (codec/url-encode member)
                    "'].@Count" gt "'0';AssetState='Closed'&sort=ChangeDate")]
     (cons fields
           (accumulate (request-flat query fields)))))
  ([team sprint asset-type plural]
   (cons [plural]
         (let [fields ["Name"]
               query (str "/Data/" asset-type
                          "?sel=" (ff fields)
                          "&where=Timebox.Name='" (codec/url-encode sprint)
                          "';Team.Name='" (codec/url-encode team)
                          "';-SplitTo;AssetState!='Dead'&sort=Name")]
           (request-flat query fields)))))

(defn stories
  "Gets a table of stories"
  [team sprint]
  (workitems team sprint "Story" "Stories"))

(defn defects
  "Gets a table of defects"
  [team sprint]
  (workitems team sprint "Defect" "Defects"))

(defn testSets
  "Gets a table of testsets"
  [team sprint]
  (workitems team sprint "TestSet" "Test Sets"))

(defn splits
  "Gets a table of splits"
  [team sprint]
  (cons ["Splits"]
        (let [fields ["Name"]
              query (str "/Data/PrimaryWorkitem?sel=" (ff fields)
                         "&where=Timebox.Name='" (codec/url-encode sprint)
                         "';Team.Name='" (codec/url-encode team)
                         "';SplitTo;AssetState!='Dead'&sort=Name")]
          (request-flat query fields))))

(defn- nest [data criteria]
  (if (empty? criteria)
    data
    (into {} (for [[k v] (group-by #(nth % (first criteria)) data)]
      (hash-map k (nest v (rest criteria)))))))

(defn- group
  [m k v]
  (update-in m [k] (fnil conj []) v))

(defn- roadmap-transform
  [m]
  (let [header (sort (set (map #(nth % 3) m)))
        sum-by (fn [m row]
                 (map-add m (vec (take 4 row)) (last row)))
        result (reduce sum-by {} m)
        result (map #(conj (first %) (two-dec (second %))) result)
        projects (nest result [0 1 2 3])]
    ; sparse matrix dates take up a lot of text, so send full matrix
    (cons (apply vector "Project" "Customer" "Team" header)
      (apply concat
        (for [[project customers] projects]
          (apply concat
            (for [[customer teams] customers]
              (for [[team dates] teams]
                (apply vector project customer team
                       (map (comp (fnil last [0]) last dates) header))))))))))

(defn roadmap
  "Get the projected work"
  []
  (let [fields ["Scope.Name" "Parent.Name" "Team.Name" "Timebox.EndDate" "Estimate"]
        horizon (time/months 4)
        now (time/now)
        query (str "/Data/PrimaryWorkitem?sel=" (ff fields)
                   "&where=Estimate;Team.Name"
                   ";Timebox.EndDate" lt \' (tostr-date (time/plus now horizon))
                   "';Timebox.EndDate" gt \' (tostr-date (time/minus now horizon))
                   \')
        result (request-flat query fields "None")]
    (roadmap-transform result)))

(defn feedback
  "Get the feedback for a retrospective"
  [team sprint]
  (let [query (str "/Data/Retrospective?sel=Summary"
                   "&where=Team.Name='" (codec/url-encode team)
                   "';Timebox.Name='" (codec/url-encode sprint) \')]
    (request-transform query singular)))

(defn- transform-members
  [s]
  (map #(clojure.set/rename-keys % {"DefaultRole.Name" :role
                                    "DefaultRole.Order" :tier
                                    "MemberLabels.Name" :team})
       s))
(defn members
  "Retrieve a memberlist with roles"
  []
  (let [fields ["Name" "DefaultRole.Name" "DefaultRole.Order" "MemberLabels.Name"]
        query (str "/Data/Member?sel=" (ff fields)
                   ";AssetState!='Dead'")]
    (request-transform query transform-members)))

(defn transform-effort
  [s]
  (map #(clojure.set/rename-keys % {"Value" :hours}) s))
(defn effort
  []
  (let [fields ["Name" "Value"]
        query (str "/Data/Actual?sel=" (ff fields)
                   ";AssetState!='Dead'")]
    (request-transform query transform-effort)))

; TODO
#_(defn allocation
  [])

(defn- compress
  [xs]
  (reduce #(if (= (last %1) %2) %1 (conj %1 %2)) [] xs))
;(is (= (compress [:a :a :b :c :a :c :c :c]) [:a :b :c :a :c]))

(defn- count-failed
  [s]
  (->> s
       compress
       (filter #(= "Failed Review" (get % "Status.Name")))
       (group-by #(get % "Timebox.Name"))
       (map (fn [[k v]]
              [k (count v)]))
       sort))

;/Hist/PrimaryWorkitem?sel=Name,Status.Name,ChangeDate&where=Team.Name='TC+Sharks';Timebox.Name='TC1311';Status.Name&sort=Name,ChangeDate
(defn- failed-review-all
  [team]
  (let [query (str "/Hist/PrimaryWorkitem?sel=Timebox.Name,Number,Status.Name&Sort=Timebox.Name,Number,ChangeDate"
                   "&where=Team.Name='" (codec/url-encode team)
                   "';AssetState!='Dead';Timebox.Name;Status.Name")
        result (request-transform query count-failed)]
    result))

(defn failedReview
  "A table of the count of failed review events for a team over the last 5 sprints"
  [team sprint]
  (let [sprints (failed-review-all team)
        c (count-to sprint (map first sprints))]
    (cons ["Sprint" "Failed Review"]
          (take-last 5
                     (take c sprints)))))

(defn- count-added-after-start
  [s]
  (->> s
       compress
       (filter #(= "Failed Review" (get % "Status.Name")))
       (group-by #(get % "Timebox.Name"))
       (map (fn [[k v]]
              [k (count v)]))
       sort))


(defn- story-set-on
  [team sprint date]
  (let [query (str "/Hist/PrimaryWorkitem?sel=Number&asof=" (tostr-date date)
                   "&where=Timebox.Name='" sprint
                   "';Team.Name='" team
                   "';AssetState!='Dead'")]
    (set (flatten (request-flat query ["Number"])))))

(defn- churn-data
  [team sprint]
  (let [span (sprint-span (codec/url-encode sprint))
        begin (time/plus (span "BeginDate") (time/days 1))
        end (span "EndDate")]
    (for-sprint team sprint begin end story-set-on)))

(defn- get-story [story-number]
  (let [fields ["Estimate" "Number" "Name"]
        query (str "/Data/PrimaryWorkitem?sel=" (ff fields)
                   "&where=Number='" story-number "'")]
    (first (request-flat query fields))))

(defn- added [s]
  (map clojure.set/difference (rest s) s))

(defn- removed [s]
  (map clojure.set/difference s (rest s)))

(defn churnStories
  "The story names added to a sprint, and removed from a sprint"
  [team sprint]
  (let [results (churn-data team sprint)
        collect #(reduce clojure.set/union (% results))
        collect-get #(map get-story (collect %))
        a (map #(cons "Added" %) (collect-get added))
        r (map #(cons "Removed" %) (collect-get removed))]
    (println "a" a)
    (println "r" r)
    (cons ["Action" "Points" "Story" "Title"]
          (concat a r))))

(defn churn
  "The count of stories added to and removed from a sprint"
  [team sprint]
  (let [results (churn-data team sprint)
        sum #(reduce + (map count (% results)))
        a (sum added)
        r (sum removed)]
    [sprint a r]))

(defn churnComparison
  "Gets a table of the past 4 sprints' churn"
  [team sprint]
  (let [s (->> (sprints team)
            (take-to sprint)
            (take-last 5))]
    (cons ["Sprint" "Added" "Removed"]
          (map #(churn team %) s))))

;/Data/Scope?where=Scope.Name='TC5.00'&sel=Workitems:Defect.@Count
(defn- defect-rate-on
  [scope date]
  (let [count-open (str "Workitems:Defect[AssetState!='Dead';AssetState!='Closed';Status.Name!='Accepted';Status.Name!='"
                        (codec/url-encode "QA Complete") "'].@Count")
        count-total "Workitems:Defect[AssetState!='Dead'].@Count"
        fields [count-open count-total]
        query (str "/Data/Scope?asof=" (tostr-date date)
                   "&where=AssetState!='Dead';Name='" scope "'"
                   "&sel=" (ff fields))]
    (first (request-flat query fields 0))))

(defn defectRate
  "Open and total defects for the last 3 months for a project"
  ([scope]
   (let [now (time/now)
         horizon (time/months 3)]
     (defectRate scope (time/minus now horizon) now)))
  ([scope begin end]
   (let [counts (for-period begin end (partial defect-rate-on scope))]
     (cons ["Day" "Open" "Total"]
           (map cons (iterate inc 1) counts)))))



