(ns vone.views.service
  (:use [vone.models.queries]
        [clojure.data.csv]
        [clojure.string :as str]
        [noir.core]
        [noir.response :only [json redirect content-type status]]
        [slingshot.slingshot :only [try+]]))

(defn csv
  [content filename]
  (assoc-in 
    (content-type "text/csv"
      (str (doto (java.io.StringWriter.) (write-csv content))))
    [:headers "Content-Disposition"]
    (str "attachment;filename=" filename ".csv")))

(defn reqId
  [tqx]
  (if tqx
    (let [match (re-find #"reqId:(\d+)" tqx)]
      (parseInt (second match)))
    0))

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
  (csv (burndown team sprint) (str team "_" sprint)))
(defpage "/burndown/:team/:sprint" {:keys [team sprint tqx]}
  (try+
    (datasource tqx (burndown team sprint))
    (catch [:status 401] []
      (status 401 "Login"))))

;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/cumulative"] {:keys [team sprint]}
  (redirect (str "/cumulative/" team "/" sprint)))
(defpage "/csv/cumulative/:team/:sprint" {:keys [team sprint]}
  (csv (cumulative team sprint) (str team "_" sprint)))
(defpage "/cumulative/:team/:sprint" {:keys [team sprint tqx]}
  (try+
    (datasource tqx (cumulative team sprint))
    (catch [:status 401] []
      (status 401 "Login"))))

(defpage "/teams" []
  (try+
    (json (sort-by str/upper-case (names "Team")))
    (catch [:status 401] []
      (status 401 "Please Login"))))
