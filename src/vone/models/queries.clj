(ns vone.models.queries
  (:use [clojure.pprint])
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.client :as client]
            [clojure.xml :as xml]
            [ring.util.codec :as codec]
            [noir.session :as session]))

(def base-url "http://www3.v1host.com/Tideworks/VersionOne/rest-1.v1")

; http://biesnecker.com/infinite-lazy-seqs-clojure-joda-time.html
;; basic functions to increment or decrement a date
(defn inc-date [#^org.joda.time.DateTime ds] (.plusDays ds 1))
(defn dec-date [#^org.joda.time.DateTime ds] (.minusDays ds 1))
;; generate infinite streams of LocalDate objects starting with start-ds
(defn inc-date-stream [#^org.joda.time.DateTime start-ds] (iterate inc-date start-ds))
(defn dec-date-stream [#^org.joda.time.DateTime start-ds] (iterate dec-date start-ds))
(defn weekend? [#^org.joda.time.DateTime ds] (> (.get (.dayOfWeek ds)) 5))

(defn parse-date
  [date]
  (format/parse (format/formatter "yyyy-MM-dd") date))

(defn tostr-date
  [date]
  (format/unparse (format/formatter "yyyy-MM-dd'T23:59:59'") date))

(defn collapse-attr
  [m attr]
  (assoc m
         (keyword (-> attr :attrs :name))
         (-> attr :content first)))

(defn collapse-asset
  [m asset]
  (conj m (reduce collapse-attr {} (asset :content))))

(defn collapse
  "Converts VersionOne xml into a more condensed structure"
  [x]
  (reduce collapse-asset [] (x :content)))

(defn xhr
  "XmlHttpRequest from VersionOne into a map"
  [query]
  (let [params {:as :stream
                :basic-auth [(session/get :username) (session/get :password)]}
        response (client/get (str base-url query) params)]
    (-> response
      :body
      xml/parse
      collapse)))

(defn names
  "Retrieves a sorted seqence of asset names"
  [asset]
  ;TODO: filtering empty descriptions because there is a story status "Incomplete"!?!?
  (let [query (str "/Data/" asset
                   "?sel=Name&where=Description;AssetState!='Dead'&sort=Order")]
    (map :Name (xhr query))))

(defn sprint-span
  [sprint]
  (let [query (str "/Data/Timebox?sel=BeginDate,EndDate&where=Name='" sprint "'")]
    (first (xhr query))))

(defn for-sprint
  "Queries f for each sprint day"
  [team sprint f]
  (let [team (codec/url-encode team)
        sprint (codec/url-encode sprint) 
        span (sprint-span sprint)
        begin (parse-date (span :BeginDate))
        end (parse-date (span :EndDate))]
    (pmap #(apply vector %1 (f team sprint %2))
          (iterate inc 1)
          (take-while #(time/before? % end)
            (filter (complement weekend?) (inc-date-stream begin))))))

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
    (-> (xhr query)
      first
      vals
      first
      parseDouble
      vector)))

(defn burndown
  [team sprint]
  (cons ["Day" "ToDo"]
        (for-sprint team sprint todo-on)))

(defn cumulative-on-status
  [team sprint date status]
  (let [query (str "/Hist/Timebox?asof=" (tostr-date date)
                   "&where=Name='" sprint
                   "'&sel=Workitems:PrimaryWorkitem[Team.Name='" team
                   "';Status.Name='" status
                   "'].Estimate[AssetState!='Dead'].@Sum")]
    (-> (xhr query)
      first
      vals
      first
      parseDouble)))

(defn cumulative-on
  [names team sprint date]
  (vec (pmap (partial cumulative-on-status team sprint date)
             (map codec/url-encode names))))

(defn cumulative
  [team sprint]
  (let [statuses (reverse (names "StoryStatus"))]
	  (cons (cons "Day" statuses)
	        (for-sprint team sprint (partial cumulative-on statuses)))))
