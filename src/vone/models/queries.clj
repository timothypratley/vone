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
  (format/parse (format/formatter "yyyy-MM-dd") date))

(defn tostr-date
  [date]
  (format/unparse (format/formatter "yyyy-MM-dd'T23:59:59'") date))

(defn indexer [x] (get-in x [:attrs :name]))
(defn collapse
  "Converts xml into a more condensed structure"
  [x]
  (cond
    (and (vector? x) (string? (first x))) (first x)
    (vector? x) (let [s (map collapse x)]
                  (if (vector? (first s))   ;key-value pairs
                    (into {} s)
                    s))
    (map? x) (let [content (:content x)]
               (let [c (collapse content)]
                 (if-let [n (indexer x)]
                   [(keyword n)
                    (if (.endsWith n "Date")
                      (parse-date c)
                      c)]
                   c)))
    :else x))

(defn xhr
  "XmlHttpRequest from VersionOne"
  [query]
  (let [params {:basic-auth [(session/get :username)
                             (session/get :password)]}]
    (client/get (str base-url query) params)))

(defn xml2map
  "Converts XML into a map"
  [xml]
  (-> (java.io.ByteArrayInputStream. (.getBytes xml))
    xml/parse
    collapse))

(defn xhr2map
  "XmlHttpRequest from VersionOne into a map"
  [query]
  (try
    (let [response (xhr query)]
      ;TODO: is there a constant for 200?
      (if (= 200 (:status response))
        (xml2map (response :body)))) 
    (catch Exception e
      (println "Foo: " e)
      (throw e))))

(defn xhre
  [query extract]
  (let [response (xhr query)
        m (xml2map (response :body))]
    (try
      (extract m)
      (catch Exception e
        (println response)
        (println m)
        (throw)))))


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
                                 (inc-date-stream begin)))
        results (pmap (partial f team sprint) days)
        indexed (map cons (iterate inc 1) results)]
    indexed))

(defn parseDouble
  [s]
  (if s
    (Double/parseDouble s)
    0))

(defn parseInt
  [s]
  (if s
    (Integer/parseInt s)
    0))

(defn todo-on
  [team sprint date]
  (let [query (str "/Hist/Timebox?asof=" (tostr-date date)
                   "&where=Name='" sprint
                   "'&sel=Workitems[Team.Name='" team
                   "'].ToDo[AssetState!='Dead'].@Sum")]
    (xhre query (comp vector parseDouble first vals first))))

(defn burndown
  [team sprint]
  (cons ["Day" "ToDo"]
        (for-sprint team sprint todo-on)))

(defn cumulative-on-status-query
  [team sprint date status]
  (str "/Hist/Timebox?asof=" (tostr-date date)
       "&where=Name='" sprint
       "'&sel=Workitems:PrimaryWorkitem[Team.Name='" team
       "';Status.Name='" status
       "'].Estimate[AssetState!='Dead'].@Sum"))

(defn extract-double
  [m]
  (-> m
    first
    vals
    first
    parseDouble))

(defn cumulative-on-status
  [team sprint date status]
  (xhre (cumulative-on-status-query team sprint date status)
        extract-double))

(defn cumulative-on
  [names team sprint date]
  (pmap (partial cumulative-on-status team sprint date)
             (map codec/url-encode names)))

(defn cumulative
  [team sprint]
  (let [statuses (reverse (names "StoryStatus"))]
	  (cons (cons "Day" statuses)
	        (for-sprint team sprint (partial cumulative-on statuses)))))

(defn sum [m story]
  (update-in m [(:Parent.Name story)] (fnil + 0) (parseDouble (:Estimate story)))) 
(defn customers
  [team sprint]
  (cons ["Customer" "Story Points"]
	  ; TODO: where should url-encode happen?
	  (let [query (str "/Data/PrimaryWorkitem?where=Timebox.Name='" (codec/url-encode sprint)
	                   "';Team.Name='" (codec/url-encode team)
	                   "';AssetState!='Dead'&sel=Estimate,Parent.Name")]
	    (xhre query (partial reduce sum {})))))

(defn team-sprints-extract
  [m]
  (->> m
    (map #(clojure.set/rename-keys
            % {:Workitems:PrimaryWorkitem.Timebox.Name :Sprint}))
    (map #(update-in % [:Sprint] (partial apply sorted-set)))
    (map #(vector (:Name %) (:Sprint %)))
    (into {})))

(defn team-sprints
  []
  (xhre "/Data/Team?sel=Name,Workitems:PrimaryWorkitem.Timebox.Name&sort=Name"
        team-sprints-extract))

(defn velocity
  [team sprint]
  ;TODO
  (let [query (str "/Data/Timebox?sel=Estimate.@Sum&where=Name=" sprint)]
    (xhre query extract-double)))

