(ns vone.helpers
   (:require [clj-time.format :as format]
             [clj-time.core :as time]))

(defn transpose
  [in]
  (let [c (count in)]
    (partition c (if (<= c 1)
                   in
                   (apply interleave in)))))

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
    (format/parse (format/formatter "yyyy-MM-dd") (.substring date 0 10))
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

(defn tostr-ds-date
  [date]
  (str "Date" (format/unparse (format/formatter "(yyyy,MM,dd)") date)))

(defn readable-date
  [date]
  (format/unparse (if (= (time/year date) (time/year (time/now)))
                    (format/formatter "MMM dd")
                    (format/formatter "MMM dd yyyy"))
                    date))

(defn basic-date
  [date]
  (format/unparse (format/formatters :basic-date) date))

