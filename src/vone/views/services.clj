(ns vone.views.services
  (:require [vone.version-one-request :refer :all]
            [vone.helpers :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [hiccup.core :refer [html]]))


;TODO: cache answers

(defn- two-dec
  [d]
  (/ (Math/round (* 100.0 d)) 100.0))

(defn sprint-span
  [sprint]
  (first (request "/Data/Timebox"
                  {:sel "BeginDate,EndDate"
                   :where (str "Name='" sprint \')})))

(defn names
  "Retrieves a sorted seqence of asset names"
  ([asset]
   (flatten (request-rows (str "/Data/" asset)
                          {:sel "Name"})))
  ([asset asof]
   (flatten (request-rows (str "/Hist/" asset)
                          {:asof (vone-date asof)
                           :sel "Name"
                           :where "Description;AssetState!='Dead'"
                           :sort "Order"}))))

(defn- index-sprints
  "Creates a hashmap of sorted sets"
  [pairs]
  (reduce (fn [m [a b]]
            (assoc m a (apply sorted-set-by #(compare %2 %1) b)))
          {}
          pairs))

;TODO: could very well calculate the velocity here also!
(defn team-sprints
  "Retrieves all teams and the sprints they have particpated in"
  []
  (index-sprints
   (request-rows "/Data/Team"
                 {:sel "Name,Workitems:PrimaryWorkitem.Timebox.Name"
                  :sort "Name"})))

(defn current-sprints
  "Retrieves current sprints"
  []
  (flatten (request-rows "/Data/Timebox"
                         {:sel "Name"
                          :where (str "EndDate>='" (time/today)
                                      "';BeginDate<'" (time/today)
                                      "';AssetState!='Dead'")
                          :sort "Name"})))

;TODO: this is getting called multiple times, it doesn't need to be
(defn- velocity-all
  [team]
  (let [sum-story-points (str "Workitems:PrimaryWorkitem[Team.Name='" team
                              "';AssetState!='Dead'].Estimate.@Sum")]
    (request-rows "/Data/Timebox"
                  {:sel (str "Name," sum-story-points)
                   :where (str sum-story-points ">'0'")
                   :sort "EndDate"})))

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
          (take-last 5 (take c sprints)))))

(defn- sprints
  "Get the name of all sprints that a particular team has done work in"
  [team]
  (map first (velocity-all team)))

(defn- days-for
  [begin end]
  (take-while #(not (time/after? % (coerce/to-date-time end)))
              (remove weekend? (inc-date-stream begin))))

(defn- for-period
  [begin end f]
  (let [days (days-for begin end)]
    ;google app engine does not allow pmap,
    ;so use regular map when deploying there
    (pmap f days)))

(defn- for-sprint
  "Queries f for each sprint day"
  ([team sprint f]
   (let [span (sprint-span sprint)
         begin (time/plus (span "BeginDate") (time/days 1))
         end (time/minus (span "EndDate") (time/days 1))]
     ;(println "TEAM:" team " SPRINT:" sprint " START:" begin "  END:" end)
     (for-sprint team sprint begin end f)))
  ([team sprint begin end f]
   (for-period begin (min-date (time/today) end) (partial f team sprint))))

(defn- singular
  "Get the value from a collection of one map with only one key"
  [col]
  (-> col first vals first))

(defn- todo-on
  "Get the todo hours on a particular day"
  [team sprint asof]
  (singular (request "/Hist/Timebox"
                     {:asof (vone-date asof)
                      :sel (str "Workitems[Team.Name='" team "';AssetState!='Dead'].ToDo.@Sum")
                      :where (str "Name='" sprint \')})))

;TODO: There is no need to get a burndown if we have the comparison? would have to manage data clientside though.
(defn burndown
  "Gets a table of todo hours per day"
  [team sprint]
  (cons ["Date" "ToDo"]
        (map vector
             (iterate inc 1)
             (for-sprint team sprint todo-on))))

(defn burndownComparison
  "Gets a table of the past 4 sprints burndowns"
  [team sprint]
  (let [recent-sprints (->> (sprints team)
                            (take-to sprint)
                            (take-last 4)
                            reverse)
        sprint-todos (pmap #(for-sprint team % todo-on) recent-sprints)]
    (if (not-empty recent-sprints)
      (cons (cons "Date" recent-sprints)
            (apply map vector
                   (iterate inc 1)
                   sprint-todos)))))

(defn- map-add
  [m k v]
  (update-in m [k] (fnil + 0) v))

(defn- sum-by-key [m entity]
  (let [k (first entity)
        ;TODO: is there a nicer way to deal with empty values?
        k (if (number? k) "None" k)]
    (map-add m k (second entity))))

(defn- summize
  "Given a collection of pairs,
  sums up seconds by making keys of first,
  returning a map of the totals"
  [c]
  (reduce sum-by-key (sorted-map) c))

(defn- cumulative-on
  [team sprint asof]
  (summize (request-rows "/Hist/PrimaryWorkitem"
                         {:asof (vone-date asof)
                          :sel "Status.Name,Estimate"
                          :where (str "Timebox.Name='" sprint "';Team.Name='" team "';AssetState!='Dead'")})))

(defn cumulative
  "Gets a table of the cumulative flow (story points by status per day)"
  [team sprint]
  (let [span (sprint-span sprint)
        end (time/minus (span "EndDate") (time/days 1))
        status-counts (for-sprint team sprint cumulative-on)
        statuses (reverse (names "StoryStatus" end))
        statuses (concat statuses
                         (clojure.set/difference
                          (set (keys (apply merge status-counts)))
                          (set statuses)))
        get-status-points (fn [m] (map #(get m % 0) statuses))
        days (iterate inc 1)]
    (cons (cons "Date" statuses)
          (map cons
               days
               (map get-status-points status-counts)))))

(defn cumulativePrevious
  "Gets a table of the previous sprint cumulative flow"
  [team sprint]
  (if-let [previous (last (take-before sprint (sprints team)))]
    (cumulative team previous)))

(defn customers
  "Gets a table of story points per customer"
  [team sprint]
  (cons ["Customer" "Story Points"]
        (summize (request-rows "/Data/PrimaryWorkitem"
                               {:sel "Parent.Name,Estimate"
                                :where (str "Timebox.Name='" sprint "';Team.Name='" team "';AssetState!='Dead'")}))))

(defn customersNext
  "Gets a table of story points per customer for the next sprint"
  [team sprint]
  (if-let [next-sprint (first (take-after sprint (sprints team)))]
    (customers team next-sprint)))

;TODO: query team instead, don't get people not in the team
(defn- participants-all
  [team sprint]
  (set (remove number? (flatten (request-rows "/Data/Workitem"
                                              {:sel "Owners.Name"
                                               :where (str "Timebox.Name='" sprint "';Team.Name='" team "';AssetState!='Dead'")})))))

(defn- map-add-estimates
  [m estimate-owners]
  (let [estimate (first estimate-owners)
        owners (second estimate-owners)]
    ; TODO: nil is replaced by 0 when there are no owners
    (if (number? owners)
      m
      (reduce #(map-add %1 %2 estimate) m owners))))

(defn- map-add-hours
  [m owner-hours]
  (let [owner (first owner-hours)
        hours (second owner-hours)]
    (map-add m owner hours)))

;TODO: query team instead, don't get people not in the team
(defn participants
  "Retrieves a table of effort recorded per participant"
  [team sprint]
  (let [workitems (request-rows "/Data/PrimaryWorkitem"
                                {:sel "Estimate,Owners.Name"
                                 :where (str "Timebox.Name='" sprint "';Team.Name='" team "';AssetState!='Dead'")})
        points (reduce map-add-estimates {} workitems)
        actuals (request-rows "/Data/Actual"
                              {:sel "Member.Name,Value"
                               :where (str "Timebox.Name='" sprint "';Team.Name='" team \')})
        hours (reduce map-add-hours {} actuals)]
    (cons ["Member" "Points" "Hours"]
          (for [member (apply sorted-set (concat (keys points) (keys hours)))]
            [member (get points member 0) (two-dec (get hours member 0))]))))

(defn- map-add-sprint
  [m estimate-owners]
  (let [estimate (first estimate-owners)
        owners (second estimate-owners)
        sprint (last estimate-owners)]
    ; TODO: nil is replaced by 0 when there are no owners
    (if (or (number? owners) (number? sprint))
      m
      (reduce #(update-in %1 [%2 sprint] (fnil + 0) estimate)
              m owners))))

(defn participation
  "Table of story points per member per sprint"
  []
  (let [points (reduce map-add-sprint {} (request-rows "/Data/PrimaryWorkitem"
                                                       {:sel "Estimate,Owners.Name,Timebox.Name"
                                                        :where "AssetState!='Dead'"}))]
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
  (let [stories (request "/Data/Story"
                         {:sel "Number,Name,Estimate,Team.Name,Owners.Name,Description,Parent.Name"
                          :where "Timebox.State.Code='ACTV';AssetState!='Dead'"})]
    (for [s stories
          :let [errors (check-story s)]
          :when errors]
      [(s "Team.Name") (s "Number") (apply vector errors)])))

; TODO: limit query to a sprint
(defn- estimates-all
  [team]
  (let [t (str "[Team.Name='" team "';AssetState!='Dead']")
        sum-point-estimates (str "Workitems:PrimaryWorkitem" t ".Estimate.@Sum")
        tt (str "[Reference!='';Team.Name='" team "';AssetState!='Dead']")
        sum-changeorders (str "Workitems:PrimaryWorkitem" tt ".Estimate.@Sum")
        count-stories (str "Workitems:Story" t ".@Count")
        count-defects (str "Workitems:Defect" t ".@Count")
        count-test-sets (str "Workitems:TestSet" t ".@Count")
        count-test-cases (str "Workitems:Test" t ".@Count")
        sum-hour-estimates (str "Workitems" t ".DetailEstimate.@Sum")
        t (str "[Team.Name='" team "']")
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
                capacity]]
    (request-rows "/Data/Timebox"
                  {:sel (apply str (interpose \, fields))
                   :where (str sum-point-estimates ">'0'")
                   :sort "EndDate"})))

(defn- with-capacity
  [team estimates]
  (for [stats estimates]
    (if (number? (last stats))
      ;replace capacity with hoursXdaysXpeople for sprint
      (concat (drop-last stats)
              [(* 5 14 (count (participants-all
                               team (first stats))))])
      stats)))

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
          (map vector (map first s) (cumulative-sum (map second s)))))

(defn- workitems
  ([]
   (let [horizon (time/years 1)
         start (time/minus (time/today) horizon)
         result (request-rows "/Data/PrimaryWorkitems"
                              {:sel "Owners.Name,Estimate"
                               :where (str "Owners[AssetState!='Dead'].@Count>'0';ChangeDate>'"
                                           (vone-date start) "';AssetState='Closed'")})]
     (map #(list (first %) (two-dec (second %)))
          (reduce (fn [m owners-estimate]
                    (let [names (first owners-estimate)
                          estimate (second owners-estimate)]
                      (reduce #(update-in %1 [%2] (fnil + 0) estimate) m names)))
                  (sorted-map) result))))
  ([member asset-type]
   (let [horizon (time/years 1)
         since (time/minus (time/today) horizon)]
     (cons ["ChangeDate" "Estimate"]
           (accumulate (request-rows (str "/Data/" asset-type)
                                     {:sel "ChangeDate,Estimate"
                                      :where (str "ChangeDate>'" since
                                                  "';Owners[Name='" member
                                                  "'].@Count>'0';AssetState='Closed'")
                                      :sort "ChangeDate"})))))
  ([team sprint asset-type plural]
   (cons [plural]
         (request-rows (str "/Data/" asset-type)
                       {:sel "Name"
                        :where (str "Timebox.Name='" sprint
                                    "';Team.Name='" team
                                    "';-SplitTo;AssetState!='Dead'")
                        :sort "Name"}))))

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
        (request-rows "/Data/PrimaryWorkitem"
                      {:sel "Name"
                       :where (str "Timebox.Name='" sprint
                                   "';Team.Name='" team
                                   "';SplitTo;AssetState!='Dead'")
                       :sort "Name"})))

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
    (cons (apply vector "Project" "Customer" "Team" (map readable-date header))
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
  (let [horizon (time/months 4)
        start (time/minus (time/today) horizon)
        end (time/plus (time/today) horizon)
        stories (request-rows "/Data/PrimaryWorkitem"
                              {:sel "Scope.Name,Parent.Name,Team.Name,Timebox.EndDate,Estimate"
                               :where (str "Estimate;Team.Name"
                                           ";Timebox.EndDate>'" (vone-date start)
                                           "';Timebox.EndDate<'" (vone-date end) \')}
                              "None")]
    (roadmap-transform stories)))

(defn feedback
  "Get the feedback for a retrospective"
  [team sprint]
  (let [query (str "/Data/Retrospective?sel=Summary"
                   "&where=Team.Name='" team
                   "';Timebox.Name='" sprint \')]
    (singular (request "/Data/Retrospective"
                       {:sel "Summary"
                        :where (str "Team.Name='" team
                                    "';Timebox.Name='" sprint \')}))))

(defn- transform-members
  [s]
  (map #(clojure.set/rename-keys % {"DefaultRole.Name" :role
                                    "DefaultRole.Order" :tier
                                    "MemberLabels.Name" :team})
       s))

(defn members
  "Retrieve a memberlist with roles"
  []
  (transform-members (request "/Data/Member"
                              {:sel "Name,DefaultRole.Name,DefaultRole.Order,MemberLabels.Name"
                               :where "AssetState!='Dead'"})))

(defn- transform-effort
  [s]
  (map #(clojure.set/rename-keys % {"Value" :hours}) s))

(defn effort
  []
  (transform-effort (request "/Data/Actual"
                             {:sel "Member.Name,Value"})))

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

(defn- failed-review-all
  [team]
  (count-failed (request "/Hist/PrimaryWorkitem"
                         {:sel "Timebox.Name,Number,Status.Name"
                          :where (str "Team.Name='" team "';AssetState!='Dead';Timebox.Name;Status.Name")
                          :sort "Timebox.Name,Number,ChangeDate"})))

(defn failedReview
  "A table of the count of failed review events for a team over the last 5 sprints"
  [team sprint]
  (let [sprints (failed-review-all team)
        c (count-to sprint (map first sprints))]
    (cons ["Sprint" "Failed Review"]
          (take-last 5 (take c sprints)))))

(defn- count-added-after-start
  [s]
  (->> s
       compress
       (filter #(= "Failed Review" (get % "Status.Name")))
       (group-by #(get % "Timebox.Name"))
       (map (fn [[k v]]
              [k (count v)]))
       sort))


(defn- storys-on
  [team sprint asof]
  (group-by second
            (map #(cons asof %)
                 (request-rows "/Hist/PrimaryWorkitem"
                               {:asof (vone-date asof)
                                :sel "Number,Name,Estimate,ChangedBy.Name"
                                :where (str "Timebox.Name='" sprint
                                            "';Team.Name='" team
                                            "';AssetState!='Dead'")}))))

(defn- churn-data
  [team sprint]
  (let [span (sprint-span sprint)
        begin (time/plus (span "BeginDate") (time/days 1))
        end (span "EndDate")]
    (for-sprint team sprint begin end storys-on)))

(defn- diff [a b]
  (reduce dissoc a (keys b)))

(defn- collect [as bs]
  (let [diffs (map first (vals (reduce merge (map diff as bs))))
        link #(html [:a {:href (str base-url "/assetdetail.v1?Number=" %)} %])]
    (map #(update-in % [1] link) (map vec diffs))))

(defn churnStories
  "The story names added to a sprint, and removed from a sprint"
  [team sprint]
  (let [stories (churn-data team sprint)
        added (collect (rest stories) stories)
        removed (collect stories (rest stories))
        ; TODO refactor insert
        a (map #(cons (first %) (cons "Added" (rest %))) added)
        r (map #(cons (time/plus (first %) (time/days 1)) (cons "Removed" (rest %))) removed)]
    (cons ["Date" "Action" "Story" "Title" "Points" "By"]
          (map #(update-in (vec %) [0] readable-date)
               (sort-by first (concat a r))))))

(defn churn
  "The count of stories added to and removed from a sprint"
  [team sprint]
  (let [stories (churn-data team sprint)
        added (collect (rest stories) stories)
        removed (collect stories (rest stories))
        a (count added)
        r (count removed)]
    [sprint a r]))

(defn churnComparison
  "Gets a table of the past 4 sprints' churn"
  [team sprint]
  (let [s (->> (sprints team)
               (take-to sprint)
               (take-last 5))]
    (cons ["Sprint" "Added" "Removed"]
          (map #(churn team %) s))))

(defn- defect-rate-on
  [scope asof]
  (let [count-open "Workitems:Defect[AssetState!='Dead';AssetState!='Closed';Status.Name!='Accepted';Status.Name!='QA Complete'].@Count"
        count-total "Workitems:Defect[AssetState!='Dead'].@Count"]
    (first (request-rows "/Data/Scope"
                         {:asof (vone-date asof)
                          :sel (str count-open \, count-total)
                          :where (str "AssetState!='Dead';Name='" scope \')}))))

(defn defectRate
  "Open and total defects for the last 3 months for a project"
  ([scope]
   (let [horizon (time/months 3)
         end (time/now)
         begin (time/minus end horizon)]
     (defectRate scope begin end)))
  ([scope begin end]
   (cons ["Day" "Open" "Total"]
         (map cons
              (days-for begin end)
              (for-period begin end (partial defect-rate-on scope))))))

(defn projects
  "Retrieves projects with at least one open defect, and ten closed defects"
  []
  (flatten (request-rows "/Data/Scope"
                         {:sel "Name"
                          :where (str "AssetState!='Dead'"
                                      ";Workitems:Defect[AssetState!='Dead';AssetState!='Closed';Status.Name!='Accepted';Status.Name!='QA Complete'].@Count>'10'"
                                      ";Workitems:Defect[AssetState!='Dead'].@Count>'100'")
                          :sort "Name"})))


