(ns vone.views.services
  (:require [clojure.core.memoize :as memo]
            [vone.version-one-request :refer :all]
            [vone.helpers :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [hiccup.core :refer [html]]))


;TODO: cache answers

(defn- two-dec
  [d]
  (/ (Math/round (* 100.0 d)) 100.0))

(defn- sprint-span-slow
  [sprint]
  (first (request "/Data/Timebox"
                  {:sel "BeginDate,EndDate"
                   :where (str "Name='" sprint \')})))
(def sprint-span (memo/ttl sprint-span-slow :ttl/threshold 60000))

(defn names
  "Retrieves a sorted seqence of asset names"
  ([asset]
   (flatten (request-rows (str "/Data/" asset)
                          {:sel "Name"
                           :where "AssetState!='Dead';AssetState!='Closed'"
                           :sort "Order"})))
  ([asset asof]
   (flatten (request-rows (str "/Hist/" asset)
                          {:asof (vone-date asof)
                           :sel "Name"
                           :where "Description;AssetState!='Dead';AssetState!='Closed'"
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
    (let [estimation (nth stats 7)
          done (nth stats 8)
          capacity (nth stats 9)
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
              "Billable (CHG)"
              "Stories"
              "Defects"
              "Test Sets"
              "Tests"
              "Estimated"
              "Done"
              "Capacity"
              "Accuracy (Estimate/Done)"
              "Efficiency (Done/Capacity)"
              "Billable (CHG) ratio"]
             estimates)))))

;from incanter
(defn- cumulative-sum
  " Returns a sequence of cumulative sum for the given collection. For instance
  The first value equals the first value of the argument, the second value is
  the sum of the first two arguments, the third is the sum of the first three
  arguments, etc.

  Example:
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
  (reduce (fn [acc [date cumulative]]
            (assoc acc date cumulative))
          (sorted-map)
          (map vector
               (map (comp tostr-ds-date first) s) ;dates
               (cumulative-sum (map second s)))))

(defn workitems
  ([]
   (let [horizon (time/years 1)
         start (time/minus (time/today) horizon)
         result (request-rows "/Data/PrimaryWorkitem"
                              {:sel "Owners.Name,Estimate"
                               :where (str "Owners[AssetState!='Dead'].@Count>'0';ChangeDate>'"
                                           (vone-date start) "';AssetState='Closed'")})]
     (map #(list (first %) (two-dec (second %)))
          (reduce (fn [m owners-estimate]
                    (let [names (first owners-estimate)
                          estimate (second owners-estimate)]
                      (reduce #(update-in %1 [%2] (fnil + 0) estimate) m names)))
                  (sorted-map) result))))
  ([member]
   (let [horizon (time/years 1)
         since (time/minus (time/today) horizon)]
     (cons ["Date" "Cumulative Story Points"]
           (accumulate (request-rows "/Data/PrimaryWorkitem"
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

(defn members
  "Retrieve a memberlist with roles and points"
  [^Integer year]
  (let [horizon (time/years 1)
        since (time/date-time year)
        to (time/plus since horizon)
        restrict (str "ChangeDate<'" (vone-date to) "';ChangeDate>'" (vone-date since) \')
        effort (request-rows "/Data/Actual"
                             {:sel "Member.Name,Value"
                              :where (str restrict)})
        entries (reduce (fn [acc [member value]]
                          (if (string? member)
                            (update-in acc [member] (fnil inc 0))
                            acc))
                        {} effort)
        hours (reduce (fn [acc [member value]]
                        (if (string? member)
                          (update-in acc [member] (fnil + 0) value)
                          acc))
                      {} effort)
        stories (request-rows "/Data/PrimaryWorkitem"
                          {:sel "Owners.Name,Estimate,CreatedBy.Name"
                           :where (str restrict ";Owners.@Count>'0';AssetState='Closed'")})
        stories (remove (comp number? first) stories)
        gather-points (fn [acc [owners estimate created-by]]
                        (reduce (fn gather-owner-points [acc owner]
                                  (update-in acc [owner] (fnil + 0) estimate))
                                acc owners))
        points (reduce gather-points {} stories)
        gather-stories (fn [acc [owners estimate created-by]]
                         (reduce (fn gather-owner-count [acc owner]
                                   (update-in acc [owner] (fnil inc 0)))
                                 acc owners))
        worked (reduce gather-stories {} stories)
        created (reduce (fn [acc [owners estimate created-by]]
                          (if (string? created-by)
                            (update-in acc [created-by] (fnil inc 0))
                            acc))
                        {} stories)
        details (request-rows "/Data/Member"
                              {:sel "Name,DefaultRole.Name,MemberLabels.Name"
                               :where "AssetState!='Dead';AssetState!='Closed'"
                               :sort "Name"})]
    (cons ["Member" "Links" "Owner Points" "Owner Count" "Created Count" "Time Entries" "Total Hours" "Role" "Group"]
          (for [[member role labels] details]
            [member
             (html [:a {:href (str "#/member/" member) :target "_blank"} "[chart]"])
             (when-let [p (points member)] (two-dec p))
             (when-let [w (worked member)] w)
             (when-let [c (created member)] c)
             (when-let [e (entries member)] e)
             (when-let [h (hours member)] (two-dec h))
             (clojure.string/replace (str role) "Role.Name'" "")
             (when (sequential? labels)
               (clojure.string/replace (clojure.string/join "," labels) " Member Group" ""))]))))

; TODO
#_(defn allocation
    [])

(defn- compress
  ([xs]
   (compress xs identity))
  ([xs by]
   (reduce (fn [aggregate new-value]
             (if (= (by new-value) (by (last aggregate)))
               aggregate
               (conj aggregate new-value)))
           [] xs)))
;(fact (compress [:a :a :b :c :a :c :c :c]) => [:a :b :c :a :c]))
;(fact (compress [{:a 1} {:a 2} {:a 2}] :a) => [{:a 1} {:a 2}])

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

(defn- nth= [a b n]
  (= (nth a n) (nth b n)))

(defn storyChurnHistory
  [story sprint team]
  (let [x (request-rows "/Hist/PrimaryWorkitem"
                        {:sel "ChangeDate,ChangedBy.Name,Timebox.Name,Team.Name,AssetState"
                         :where (str "Number='" story \')}
                        "None")
        y (compress x #(drop 2 %))
        what (fn [old new]
               (str
                (->> [(when-not (nth= old new 2)
                        (str "Assigned to sprint " (nth new 2)))
                      (when-not (nth= old new 3)
                        (str "Moved to team " (nth new 3)))
                      (when-not (nth= old new 4)
                        (if (#{128.0 64.0} (nth new 4))
                          "Undeleted"
                          "Deleted"))]
                     (remove nil?)
                     (clojure.string/join " and "))
                " by "
                (second new)
                " on "
                (readable-date (first new))))
        created (first y)
        init (str "Created by " (second created) " on " (readable-date (first created)))
        hist (clojure.string/join ", " (cons init (map what y (rest y))))]
    hist))

(defn- index
  [sm k]
  (reduce #(assoc %1 (%2 k) %2) {} sm))

(defn- stories-on
  [team sprint asof]
  (index
   (request "/Hist/PrimaryWorkitem"
            {:asof (vone-date asof)
             :sel "ChangeDate,ChangedBy.Name,Number,Name,Timebox.Name,Team.Name,Estimate,AssetState"
             :where (str "Timebox.Name='" sprint
                         "';Team.Name='" team
                         "';AssetState!='Dead'")}
            "None")
   "Number"))

(defn- story-changes-slow
  [begin end]
  (request "/Hist/PrimaryWorkitem"
           {:sel "ChangeDate,Number,Name,Timebox.Name,Team.Name,Estimate,ChangedBy.Name,AssetState"
            :where (str "ChangeDate>'" (vone-date begin) "';ChangeDate<'" (vone-date end) \')
            :sort "ChangeDate,Number"}
           ""))
(def ^:private story-changes (memo/ttl story-changes-slow :ttl/threshold 60000))

(defn- links
  [number]
  (html [:a {:href (str "#/history/" number) :target "_blank"} "[hist]"]
        [:a {:href (str base-url "/assetdetail.v1?Number=" number) :target "_blank"} "[v1]"]))

(defn churnStories
  "The story names added to a sprint, and removed from a sprint"
  [team sprint]
  (let [span (sprint-span sprint)
        begin (span "BeginDate")
        end (span "EndDate")
        initial (stories-on team sprint begin)
        changes (story-changes begin end)
        what (fn [[stories changes] story]
               (let [number (story "Number")
                     was (stories number)
                     in (and (= sprint (story "Timebox.Name"))
                             (= team (story "Team.Name"))
                             (#{64.0 128.0} (story "AssetState")))
                     row (map story ["ChangeDate" "Number" "Name" "Estimate" "ChangedBy.Name"])]
                 (cond (and was (not in))
                       [(dissoc stories number) (conj changes (cons "Removed" row))]

                       (and in (not was))
                       [(assoc stories number story) (conj changes (cons "Added" row))]

                       (and was in (not= (was "Estimate") (story "Estimate")))
                       [(assoc stories number story) (conj changes (cons "Re-estimated" row))]

                       :else
                       [stories changes])))
        events (second (reduce what [initial []] changes))]
    (cons ["Action" "Date" "Story" "Title" "Points" "By" "Links"]
          (reverse (map (fn [[a b c d e f]]
                          [a (readable-date b) c d e f (links c)])
                        events)))))

(defn- map-difference [a b]
  (reduce dissoc a (keys b)))

(defn- collect [as bs]
  (reduce merge (map map-difference as bs)))

(defn- churn-data
  [team sprint]
  (let [span (sprint-span sprint)
        begin (time/plus (span "BeginDate") (time/days 1))
        end (span "EndDate")]
    (for-sprint team sprint begin end stories-on)))

(defn churn
  "The count of stories added to and removed from a sprint"
  [team sprint]
  (let [stories (churn-data team sprint)
        added (collect (rest stories) stories)
        removed (collect stories (rest stories))]
    [sprint (count added) (count removed)]))

(defn churnComparison
  "Gets a table of the past 4 sprints' churn"
  [team sprint]
  (let [s (->> (sprints team)
               (take-to sprint)
               (take-last 5))]
    (cons ["Sprint" "Added" "Removed"]
          (pmap #(churn team %) s))))

(defn churnStoriesList
  [team sprint]
  (let [stories (churn-data team sprint)
        added (collect (rest stories) stories)
        removed (collect stories (rest stories))
        all (map key (set (concat added removed)))]
    all))

(defn- defect-rate-on
  [scope asof]
  (let [count-open "Workitems:Defect[AssetState!='Dead';AssetState!='Closed';Status.Name!='Accepted';Status.Name!='QA Complete'].@Count"
        count-total "Workitems:Defect[AssetState!='Dead'].@Count"]
    (first (request-rows "/Hist/Scope"
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
  "Retrieves a list of project names"
  []
  (sort (names "Scope")))

(defn- rename
  [s]
  (let [s (clojure.string/replace s #".Name" "")
        translate {"Name" "Title"
                   "Timebox" "Sprint"
                   "Parent" "Customer"
                   "Scope" "Project"
                   "Super" "Epic"}]
    (translate s s)))

(defn- asset-state
  [x]
  ({0.0 "Future"
    64.0 "Active"
    128.0 "Closed"
    200.0 "Template"
    208.0 "Converted to Epic"
    255.0 "Deleted"} x "Unknown"))

(defn storyFullHistory
  [story]
  (let [missing (Object.)
        remove-missing (fn [m]
                         (into {} (remove #(= missing (val %)) m)))
        history (->> (request "/Hist/PrimaryWorkitem"
                              {:sel (str "ChangedBy.Name,ChangeDate,Name,Estimate,Status.Name,Team.Name,Timebox.Name,"
                                         "AssetState,Scope.Name,Parent.Name,Description,"
                                         "Priority.Name,SplitFrom.Number,SplitTo.Number,Super.Name,Source.Name,Reference,Owners.Name,"
                                         "BlockingIssues.Number,Requests.Number,Order,"
                                         ; to get the real change for relations we would need to query them by story number and merge
                                         "Attachments.Name,Links.Name,ChangeSets.Reference,ChangeSets.Name")
                               :where (str "Number='" story \')}
                              missing)
                     (map remove-missing))
        title ((last history) "Name")
        meta-keys ["ChangeDate" "ChangedBy.Name"]
        meta-history (map #(map % meta-keys) history)
        delta-history (map #(apply dissoc % meta-keys) history)
        unwrap (fn [k v]
                 (if (sequential? v)
                  (clojure.string/join ", " (sort v))
                  (if (= "AssetState" k)
                    (asset-state v)
                    v)))
        edits (for [[removed-keys added changed] (map exdiff delta-history (rest delta-history))]
                (concat
                 (when (seq removed-keys)
                   [(str "Cleared " (unwrap nil (map rename removed-keys)))])
                 (map (fn [[k v]]
                        (str "Set " (rename k) " to " (unwrap k v)))
                      (sort added))
                 (map (fn [[k v1 v2]]
                        (if (= k "Description")
                           [:div
                            [:div "Changed Description:"]
                            [:div
                             [:div.col-md-6 v1]
                             [:div.col-md-6 v2]]]
                          (str "Changed " (rename k) " from " (unwrap k v1) " to " (unwrap k v2))))
                      (sort changed))))
        edits (cons [(if-let [split-from ((first history) "SplitFrom")]
                       (str "Split " story " from " split-from)
                       (str "Created " story))]
                    edits)
        fmt (fn [edit]
              (when (seq edit)
                (html [:ul.list-unstyled
                       (for [e edit]
                         [:li e])])))
        edits (map fmt edits)]
    (cons ["Date" title "By"]
          (reverse
           (remove #(nil? (second %))
                   (map (fn [[date by] edit]
                          [(readable-date date) edit by])
                        meta-history edits))))))

(defn openItems
  [project]
  (cons ["Team" "Story" "Title" "Points" "Status" "Priority" "Links"]
        (map (fn [[a b c d e f]] [a b c d e f (links b)])
             (request-rows "/Data/PrimaryWorkitem"
                           {:sel "Team.Name,Number,Name,Estimate,Status.Name,Priority.Name"
                            :where (str "Scope.Name='" project
                                        "';AssetState='Active';Status.Name!='Accepted'")
                            :sort "Team.Name,Number"}
                           "None"))))

;; might be interesting to get all rows and allow the user to break down by team/sprint/etc
#_(defn effort-allocation
  "Retrieves a table of effort recorded per team sprint"
  [team sprint]
  (let [actuals (request-rows "/Data/Actual"
                              {:sel "Value,Workitem.AssetType,Workitem.Parent.AssetType"
                               :where (str "Timebox.Name='" sprint "';Team.Name='" team \')})
        hours (reduce map-add-hours {} actuals)]
    (cons ["Member" "Points" "Hours"]
          (for [member (apply sorted-set (concat (keys points) (keys hours)))]
            [member (get points member 0) (two-dec (get hours member 0))]))))


(defn effortAllocation
  "Retrieves a table of effort recorded per team sprint"
  [team]
  (let [categories [["Story" ";Reference!=''"]
                    ["Story" ";Reference=''"]
                    ["Defect" nil]
                    ["TestSet" nil]]
        fields (cons "Name"
                     (for [[asset-type condition] categories]
                       (str "Workitems:" asset-type
                            "[Team.Name='" team "';AssetState!='Dead'"
                            condition "]" (when (not= "TestSet" asset-type) ".Children")
                            ".Actuals.Value.@Sum")))]
    (cons ["Sprint" "Billable" "Enhancement" "Defect" "Regression Testing"]
          (request-rows "/Data/Timebox"
                        {:sel (apply str (interpose \, fields))
                         :where (str "Workitems[Team.Name='" team "'].Children.Actuals.Value.@Sum>'0'"
                                     ";BeginDate<'" (time/today)
                                     "';BeginDate>'" (time/minus (time/today) (time/years 1)) \')
                         :sort "Name"}))))
