(ns vone.helpers
   (:require [clj-time.format :as format]))

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

(defn parse-double
  [s]
  (try
    (Double/parseDouble s)
    (catch Exception e
      nil)))

(defn tostr-date
  [date]
  (format/unparse (format/formatter "yyyy-MM-dd'T23:59:59'") date))
