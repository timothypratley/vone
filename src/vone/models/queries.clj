(ns vone.models.queries
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.client :as client]
            [clojure.xml :as xml]
            [ring.util.codec :as codec]
            [noir.session :as session]))

;TODO: cache answers, use with-connection-pool from clj-http

(def base-url "http://www3.v1host.com/Tideworks/VersionOne/rest-1.v1")

;TODO: move these to a helper
; http://biesnecker.com/infinite-lazy-seqs-clojure-joda-time.html
;; basic functions to increment or decrement a date
(defn inc-date
  [#^org.joda.time.DateTime ds]
  (.plusDays ds 1))
(defn dec-date
  [#^org.joda.time.DateTime ds]
  (.minusDays ds 1))
;; generate infinite streams of LocalDate objects starting with start-ds
(defn inc-date-stream
  [#^org.joda.time.DateTime start-ds]
  (iterate inc-date start-ds))
(defn dec-date-stream
  [#^org.joda.time.DateTime start-ds]
  (iterate dec-date start-ds))
(defn weekend?
  [#^org.joda.time.DateTime ds]
  (> (.get (.dayOfWeek ds)) 5))

(defn parse-date
  [date]
  (try
    (format/parse (format/formatter "yyyy-MM-dd") date)
    (catch Exception e
      nil)))
  

(defn tostr-date
  [date]
  (format/unparse (format/formatter "yyyy-MM-dd'T23:59:59'") date))

(defn parse-double
  [s]
  (try
    (Double/parseDouble s)
    (catch Exception e
      nil)))

(let [indexer (comp :name :attrs)]
  (defn collapse
	  "Converts xml into a more condensed structure"
	  [x]
	  (cond
	    (and (vector? x) (string? (first x))) (first x)
	    (vector? x) (let [s (map collapse x)]
	                  (if (vector? (first s))   ;key-value pairs
	                    (into {} s)
	                    s))
	    (map? x) (let [c (collapse (:content x))]
	               (if-let [n (indexer x)]
	                 [n (if c
                        (or (parse-double c) (parse-date c) c)
                        0)]
	                 c))
	    :else x)))

;TODO: maybe I can parse the query string to figure out the return fields
(comment "This would be awesome, but V1 does not preserve order in select"
  defn collapse
  "Converts xml into a more condensed structure"
  [x]
  (cond
    (and (vector? x) (string? (first x))) (first x)
    (vector? x) (map collapse x)
    (map? x) (if-let [c (collapse (:content x))]
               (or (parse-double c) (parse-date c) c)
               0)
    :else x))

(defn xhr
  "XmlHttpRequest from VersionOne"
  [query]
  (let [params {:basic-auth [(session/get :username)
                             (session/get :password)]}]
    (try
      (client/get (str base-url query) params)
      (catch Exception e
        (println (str base-url query))
        (throw e)))))

(defn xml2map
  "Converts XML into a map"
  [xml]
  (try
    (-> (java.io.ByteArrayInputStream. (.getBytes xml))
      xml/parse
      collapse)
    (catch Exception e
      (println xml)
      (throw e))))

(defn xhre
  [query extract]
    (let [response (xhr query)
          m (xml2map (response :body))]
      (try
        (extract m)
        (catch Exception e
          (println m)
          (throw e)))))

(defn names
  "Retrieves a sorted seqence of asset names"
  [asset]
  ;TODO: filtering empty descriptions because there is a story status
  ;"Incomplete"!?!?
  (let [query (str "/Data/" asset
                   "?sel=Name&where=Description;AssetState!='Dead'&sort=Order")]
    (xhre query (partial map :Name))))

;TODO: might not need this anymore?
(defn sprint-span
  [sprint]
  (let [query (str "/Data/Timebox?sel=BeginDate,EndDate&where=Name='" sprint "'")]
    (xhre query first)))

(defn team-sprints-extract
  "Creates a map from team name to sprints they have participated in"
  [teams]
  (into {}
        (for [team teams]
          [(:Name team)
           (apply sorted-set (:Workitems:PrimaryWorkitem.Timebox.Name team))])))

;TODO: could very well calculate the velocity here also!
(defn team-sprints
  "Retrieves all teams and the sprints they have particpated in"
  []
  (xhre "/Data/Team?sel=Name,Workitems:PrimaryWorkitem.Timebox.Name&sort=Name"
        team-sprints-extract))

;TODO: the field names are screwy, just access them by index?
(defn velocity-extract
  [velocity-field sprints]
  (for [sprint sprints]
    [(:Name sprint)
     ((keyword (codec/url-decode velocity-field)) sprint)]))

;TODO: this is getting called multiple times, it doesn't need to be
(defn velocity-all
  [team]
  (let [sum-story-points (str "Workitems:PrimaryWorkitem[Team.Name='" (codec/url-encode team)
                              "'].Estimate[AssetState!='Dead'].@Sum")
        query (str "/Data/Timebox?sel=Name," sum-story-points
                   "&where=" sum-story-points
                   ">'0'&sort=EndDate")]
    (xhre query (partial velocity-extract sum-story-points))))

(def not-pos? (complement pos?))
(def not-neg? (complement neg?))
(defn take-before
  [sprint sprints]
  (take-while #(neg? (compare % sprint)) sprints))
(defn take-to
  [sprint sprints]
  (take-while #(not-pos? (compare % sprint)) sprints))
(defn take-after
  [sprint sprints]
  (drop-while #(not-pos? (compare % sprint)) sprints))

(defn velocity
  [team sprint]
  (let [sprints (velocity-all team)]
    (cons ["Sprint" "Story Points"]
          (take-last 5
                     (take-while #(not-pos? (compare (first %) sprint))
                                 sprints)))))

(defn sprints
  [team]
  (map first (velocity-all team)))

(defn for-sprint
  "Queries f for each sprint day"
  [team sprint f]
  (let [team (codec/url-encode team)
        sprint (codec/url-encode sprint) 
        span (sprint-span sprint)
        begin (span :BeginDate)
        end (span :EndDate)
        days (take-while #(time/before? % end)
                         (filter (complement weekend?)
                                 (inc-date-stream begin)))]
    (pmap (partial f team sprint) days)))

(def single-extract (comp first vals first))

(defn todo-on
  [team sprint date]
  (let [query (str "/Hist/Timebox?asof=" (tostr-date date)
                   "&where=Name='" sprint
                   "'&sel=Workitems[Team.Name='" team
                   "'].ToDo[AssetState!='Dead'].@Sum")]
    (xhre query single-extract)))

(defn burndown
  [team sprint]
  (cons ["Day" "ToDo"]
        (map list (iterate inc 1)
             (for-sprint team sprint todo-on))))

(defn burndownComparison
  [team sprint]
  (let [sprints (take-last 4
                           (take-to sprint (sprints team)))]
    (cons (cons "Day" sprints)
          (map cons (iterate inc 1)
               (apply map list (map #(for-sprint team % todo-on) sprints))))))

(defn cumulative-on-status-query
  [team sprint date status]
  (str "/Hist/Timebox?asof=" (tostr-date date)
       "&where=Name='" sprint
       "'&sel=Workitems:PrimaryWorkitem[Team.Name='" team
       "';Status.Name='" status
       "'].Estimate[AssetState!='Dead'].@Sum"))

(defn cumulative-on-status
  [team sprint date status]
  (xhre (cumulative-on-status-query team sprint date status)
        single-extract))

(defn cumulative-on
  [names team sprint date]
  (pmap (partial cumulative-on-status team sprint date)
        (map codec/url-encode names)))

(defn cumulative
  [team sprint]
  (let [statuses (reverse (names "StoryStatus"))]
    (cons (cons "Day" statuses)
          (map cons (iterate inc 1)
               (for-sprint team sprint (partial cumulative-on statuses))))))

(defn cumulativePrevious
  [team sprint]
  (if-let [previous (last (take-before sprint (sprints team)))]
    (cumulative team previous)))

(defn sum [m story]
  (update-in m [(:Parent.Name story)] (fnil + 0) (:Estimate story))) 
(defn customers
  [team sprint]
  (cons ["Customer" "Story Points"]
	  ; TODO: where should url-encode happen?
	  (let [query (str "/Data/PrimaryWorkitem?where=Timebox.Name='" (codec/url-encode sprint)
	                   "';Team.Name='" (codec/url-encode team)
	                   "';AssetState!='Dead'&sel=Estimate,Parent.Name")]
	    (xhre query (partial reduce sum {})))))

(defn customersNext
  [team sprint]
  (if-let [next (first (take-after sprint (sprints team)))]
    (customers team next)))

(defn estimate-extract
  [m]
  (println "BOO " m)
  m)

; TODO: limit query to a sprint
(defn estimates-all
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
        query (str "/Data/Timebox?sel=Name," sum-point-estimates
                   "," count-stories
                   "," count-defects
                   "," count-test-sets
                   "," count-test-cases
                   "," sum-hour-estimates
                   "," sum-hour-actuals
                   "," capacity
                   "&where=" sum-point-estimates
                   ">'0'&sort=EndDate")]
    (xhre query estimate-extract)))

(defn estimates
  [team sprint]
  (estimates-all team))
