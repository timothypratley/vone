(ns vone.data
  (:use [vone.models.queries])
  (:require [clojure.java.io :as io]))

(defn read-clj
  [filename]
  (binding [*read-eval* false]
    (with-open [r (io/reader filename)]
      (read (java.io.PushbackReader. r)))))

(def rankings (ref (read-clj "rankings.clj")))
(def roles (ref (read-clj "roles.clj")))

(defn save-data
  []
  (with-open [w (io/writer "rankings.clj")]
    (.write w @rankings)))

(defn read-data
  []
  (dosync
    (ref-set rankings (read-clj "rankings.clj"))
    (ref-set roles (read-clj "roles.clj"))))

(defn get-rankings
  []
  (sort-by :name
    (map (fn [m]
           (let [member-name (first m)
                 data (second m)]
             (-> data
               (update-in [:points] (fnil two-dec 0))
               (assoc :score 
                      (two-dec
                        (+ (/ (or (:points data) 0) 20)
                           (- 10 (or (:tier data) 10)))))
               (assoc :name member-name))))
         @rankings)))

