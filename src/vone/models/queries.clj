(ns vone.models.queries
  (:use [vone.models.version-one-request]
        [clojure.pprint]
        [vone.helpers])
  (:require [ring.util.codec :as codec]
            [clj-time.core :as time]))

;TODO: cache answers

(defn- ff
  "format fields for a query string"
  [fields]
  (apply str (interpose \, fields)))

(defn sprint-span
  [sprint]
  (let [query (str "/Data/Timebox?sel=BeginDate,EndDate&where=Name='" sprint "'")]
    (request-transform query first)))

(defn- names
  "Retrieves a sorted seqence of asset names"
  [asset asof]
  (let [ query (str "/Hist/" asset
                   "?asof=" (tostr-date asof)
                   "&sel=Name&where=Description;AssetState!='Dead'&sort=Order")]
    (map first (request-flat query ["Name"]))))

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
        n "Name"
        fields [n sprint]
        query (str "/Data/Team?sel=" (ff fields) "&sort=Name")]
    (request-transform query (partial setify n sprint))))

;TODO: this is getting called multiple times, it doesn't need to be
(defn- velocity-all
  [team]
  (let [sum-story-points (str "Workitems:PrimaryWorkitem[Team.Name='" (codec/url-encode team)
                              "';AssetState!='Dead'].Estimate.@Sum")
        fields ["Name" sum-story-points]
        query (str "/Data/Timebox?sel=" (ff fields)
                   "&where=" sum-story-points ">'0'&sort=EndDate")]
    (request-flat query fields)))

(def not-pos? (complement pos?))
(def not-neg? (complement neg?))
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

(defn- for-sprint
  "Queries f for each sprint day"
  ([team sprint f]
   (let [span (sprint-span (codec/url-encode sprint)) 
         begin (time/plus (span "BeginDate") (time/days 1))
         end (span "EndDate")]
     (for-sprint team sprint begin end f)))
  ([team sprint begin end f]
   (let [team (codec/url-encode team)
         sprint (codec/url-encode sprint) 
         days (take-while #(time/before? % end)
                          (filter (complement weekend?)
                                  (inc-date-stream begin)))]
     (pmap (partial f team sprint) days))))

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


; TODO: limit query to a sprint
(defn- estimates-all
  [team]
  (let [t (str "[Team.Name='" (codec/url-encode team) "';AssetState!='Dead']")
        sum-point-estimates (str "Workitems:PrimaryWorkitem" t ".Estimate.@Sum")
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
                count-stories
                count-defects
                count-test-sets
                count-test-cases
                sum-hour-estimates
                sum-hour-actuals
                capacity]
        query (str "/Data/Timebox?sel=" (ff fields)
                   "&where=" sum-point-estimates ">'0'&sort=EndDate")]
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

(defn- two-dec
  [d]
  (/ (Math/round (* 100.0 d)) 100.0))

(defn- ratio
  [a b]
  (if (zero? b)
    0
    (two-dec (/ a b))))

(defn- with-ratios
  [estimates]
  (for [stats estimates]
    (let [estimation (nth stats 6)
          done (nth stats 7)
          capacity (nth stats 8)
          accuracy (ratio estimation done)
          efficiency (ratio done capacity)]
      (concat stats [accuracy efficiency]))))

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
               "Stories"
               "Defects"
               "Test Sets"
               "Tests"
               "Estimated"
               "Done"
               "Capacity"
               "Accuracy (Estimate/Done)"
               "Efficiency (Done/Capacity)"]
              estimates)))))

(defn- workitems
  [team sprint asset-type plural]
  (cons [plural]
        (let [fields ["Name"]
              query (str "/Data/" asset-type
                         "?sel=" (ff fields)
                         "&where=Timebox.Name='" (codec/url-encode sprint)
                         "';Team.Name='" (codec/url-encode team)
                         "';AssetState!='Dead'&sort=Name")]
          (request-flat query fields))))

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

(defn participants
  "Get the participants in a sprint"
  [team sprint]
  ;TODO: don't really need a separate function for this
  [(sort (participants-all team sprint)) (sort (participants-all team sprint))])

(defn- nest [data criteria]
  (if (empty? criteria)
    data
    (into {} (for [[k v] (group-by #(nth % (first criteria)) data)]
      (hash-map k (nest v (rest criteria)))))))

(defn- group
  [m k v]
  (update-in m [k] (fnil conj []) v))

(defn projections-transform
  [m]
  (let [header (sort (set (map #(nth % 2) m)))
        sum-by (fn [m row]
                 (map-add m (vec (take 3 row)) (last row)))
        result (reduce sum-by {} m)
        result (map #(conj (first %) (two-dec (second %))) result)
        nested (nest result [0 1 2])]
    ; it is actually smaller to send down the full matrix,
    ; as the sparse matrix dates take up a lot of text
    (cons (apply vector "Customer" "Team" header)
      (apply concat
        (for [[customer teams] nested]
          (for [[team dates] teams]
            (apply vector customer team
                   (map (comp (fnil last [0]) last dates) header))))))))

(defn projections
  "Get the projected work"
  []
  (let [fields ["Parent.Name" "Team.Name" "Timebox.EndDate" "Estimate"]
        horizon (time/months 4)
        now (time/now)
        query (str "/Data/PrimaryWorkitem?sel=" (ff fields)
                   "&where=Estimate;Team.Name"
                   ";Timebox.EndDate<'" (tostr-date (time/plus now horizon))
                   "';Timebox.EndDate>'" (tostr-date (time/minus now horizon))
                   \')
        result (request-flat query fields "None")]
    (projections-transform result)))

(defn feedback
  "Get the feedback for a retrospective"
  [team sprint]
  (let [query (str "/Data/Retrospective?sel=Summary"
                   "&where=Team.Name='" (codec/url-encode team)
                   "';Timebox.Name='" (codec/url-encode sprint) "'")]
    (request-transform query singular)))

