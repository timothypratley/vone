(ns vone.views.service
  (:use [vone.models.queries]
        [clojure.data.csv]
        [noir.core]
        [noir.response :only [redirect json]]))

(defn csv
  [content]
  (doto (java.io.StringWriter.)
    (write-csv content)
    (.toString)))

(defn reqId
  [tqx]
  (if tqx
    (if-let [match (re-find #"reqId:(\d+)" tqx)]
      (parseInt (second match)))))

(defn tabulate
  [content]
  {:cols (map #(hash-map
                 :id (str "Col" %1)
                 :label %2
                 :type "number")
              (iterate inc 1)
              (first content))
   :rows (map #(hash-map
                 :c (map (partial hash-map :v) %))
              (rest content))})
         
(defn datasource
  [tqx content]
  (json {:reqId (reqId tqx)
         :table (tabulate content)}))
    
;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/burndown"] {:keys [team sprint]}
  (redirect (str "/burndown/" team "/" sprint)))
(defpage "/csv/burndown/:team/:sprint" {:keys [team sprint]}
  (csv (burndown team sprint)))
(defpage "/burndown/:team/:sprint" {:keys [team sprint tqx]}
  (datasource tqx (burndown team sprint)))

;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/cumulative"] {:keys [team sprint]}
  (redirect (str "/cumulative/" team "/" sprint)))
(defpage "/csv/cumulative/:team/:sprint" {:keys [team sprint]}
  (csv (cumulative team sprint)))
(defpage "/cumulative/:team/:sprint" {:keys [team sprint tqx]}
  (datasource tqx (cumulative team sprint)))
