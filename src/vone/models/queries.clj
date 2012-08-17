(ns vone.models.queries
  (:use [clojure.pprint])
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-http.client :as client]
            [clojure.xml :as xml]
            [ring.util.codec :as codec]))

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
  [query username password]
  (println "DEBUG QUERY: " query)
  (let [params {:as :stream
                :basic-auth [username password]}
        response (client/get (str base-url query) params)]
    (-> response
      :body
      xml/parse
      collapse)))

(defn names
  "Retrieves a sorted seqence of asset names"
  [username password asset]
  ;TODO: filtering empty descriptions because there is a story status "Incomplete"!?!?
  (let [query (str "/Data/" asset
                   "?sel=Name&where=Description;AssetState!='Dead'&sort=Order")]
    (map :Name (xhr query username password))))

(defn sprint-span
  [username password sprint]
  (let [query (str "/Data/Timebox?sel=BeginDate,EndDate&where=Name='" sprint "'")]
    (first (xhr query username password))))

(defn for-sprint
  "Queries f for each sprint day"
  [username password team sprint f]
  (let [team (codec/url-encode team)
        sprint (codec/url-encode sprint) 
        span (sprint-span username password sprint)
        begin (parse-date (span :BeginDate))
        end (parse-date (span :EndDate))]
    (map (partial f username password team sprint)
         (take-while #(time/before? % end)
           (filter (complement weekend?) (inc-date-stream begin))))))

(defn parseDouble
  [s]
  (if (nil? s)
    0
    (Double/parseDouble s)))

(defn todo-on
  [username password team sprint date]
  (let [query (str "/Hist/Timebox?asof=" (tostr-date date)
                   "&where=Name='" sprint
                   "'&sel=Workitems[Team.Name='" team
                   "'].ToDo[AssetState!='Dead'].@Sum")]
    (-> (xhr query username password)
      first
      vals
      first
      parseDouble)))

(defn cumulative-on-status
  [username password team sprint date status]
  (let [query (str "/Hist/Timebox?asof=" (tostr-date date)
                   "&where=Name='" sprint
                   "'&sel=Workitems:PrimaryWorkitem[Team.Name='" team
                   "';Status.Name='" status
                   "'].Estimate[AssetState!='Dead'].@Sum")]
    (-> (xhr query username password)
      first
      vals
      first
      parseDouble)))

(defn cumulative-on
  [names username password team sprint date]
  (vec (map (partial cumulative-on-status username password team sprint date)
            (map codec/url-encode names))))
