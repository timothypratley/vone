(ns vone.helpers
   (:require [clj-diff.core]
             [clj-time.format :as format]
             [clj-time.core :as time]
             [clj-time.coerce :as coerce]
             [clj-time.local :as local]))


(defmacro spy
  [& body]
  `(let [x# ~@body]
     (printf "=> %s = %s\n" (first '~body) x#)
     x#))

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
(defn min-date
  [dsa, dsb]
  (coerce/to-local-date
   (if (time/before? (coerce/to-date-time dsa) (coerce/to-date-time dsb))
     dsa
     dsb)))

(defn parse-date
  [date]
  (try
    (format/parse (format/formatter "yyyy-MM-dd") date)
    (catch Exception e
      nil)))

(defn parse-date-full
  [date]
  (try
    (format/parse (format/formatter "yyyy-MM-dd'T'HH:mm:ss.SSS") date)
    (catch Exception e
      nil)))

;(parse-date "2013-05-28T10:49:28.943")

(defn parse-double
  [s]
  (try
    (Double/parseDouble s)
    (catch Exception e
      nil)))

(defn tostr-ds-date
  "converts a joda time into a javascript zero based month date"
  [date]
  (str "Date(" (time/year date) "," (dec (time/month date)) "," (time/day date) ")"))

(defn readable-date
  [date]
  (try
    (format/unparse (if (= (time/year date) (time/year (time/now)))
                      (format/formatter "MMM dd")
                      (format/formatter "MMM dd yyyy"))
                    date)
    (catch Exception e
      date)))

(defn basic-date
  [date]
  (format/unparse (format/formatters :basic-date) date))

(defn vone-date
  [date]
  (str (coerce/to-local-date date) "T23:59:59"))

(defn runs
  "Splits a sequence into incremental runs"
  [[a & more]]
  (when a
    (let [run-helper (fn [[aggregate from previous] current]
                       (if (= previous (dec current))
                         [aggregate from current]
                         [(conj aggregate [from previous]) current current]))
          [aggregate from current] (reduce run-helper [[] a a] more)]
      (conj aggregate [from current]))))
;(runs [4 5 6 7 8 16 17 18]) => [[4 8] [16 18]]

(defn strdiff [st1 st2]
  (let [{added :+ removed :-} (clj-diff.core/diff st1 st2)
        rems (into {} (for [[start end] (runs removed)]
                        [start (subs st1 start (inc end))]))
        adds (into {} (for [[start & more] added]
                        [(inc start) (apply str more)]))
        rs (set (keys rems))
        as (set (keys adds))
        cs (clojure.set/intersection rs as)
        all (sort (clojure.set/union rs as))]
    (for [k all]
      (cond (cs k) (str "Replaced [" (rems k) "] With [" (adds k) \])
            (rs k) (str "Deleted [" (rems k) \])
            (as k) (str "Inserted [" (adds k) \])))))
;(strdiff "The quick brown fox" "T slow brown fox box") => ["Deleted: he" "Replaced: quick With: slow" "Inserted: ox b"]

(defn exdiff
  [before after]
  (let [before-keys (set (keys before))
        after-keys (set (keys after))
        both-keys (clojure.set/intersection before-keys after-keys)
        added-keys (clojure.set/difference after-keys before-keys)
        removed-keys (clojure.set/difference before-keys after-keys)
        changed-keys (remove #(= (before %) (after %)) both-keys)
        added (for [k added-keys] [k (after k)])
        changed (for [k changed-keys] [k (before k) (after k)])]
    [removed-keys added changed]))
;(fact (#'vone.views.services/exdiff {:a 1 :b 2 :c 3} {:b 2 :c 4 :d 5}) => [#{:a} [[:d 5]] [:c 3 4]])
