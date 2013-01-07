(ns vone.data
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
  (reverse (sort-by :score
    (map (fn [m]
           (let [member-name (first m)
                 data (second m)
                 points (:points data)
                 role (@roles (:role data))
                 total (+ (/ (or points 0) 20) (or (:tier role) 1))]
             (println data)
             {:name member-name
              :score total
              :role (:name role)
              :points points}))
         @rankings))))

