(ns vone.views.service
  (:use [vone.models.queries]
        [clojure.data.csv :only [write-csv]]
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

(defn parse-int
  [s]
  (if s
    (Integer/parseInt s)
    0))

(defn reqId
  [tqx]
  (if tqx
    (let [match (re-find #"reqId:(\d+)" tqx)]
      (parse-int (second match)))
    0))

(defn tabulate
  [content]
  {:cols (map #(hash-map :label %1)
              (first content))
   :rows (map #(hash-map
                 :c (map (partial hash-map :v) %))
              (rest content))})
         
(defn datasource
  [tqx content & columnTypes]
  (let [t (tabulate content)
        ;TODO: wow this is a really ugly way to specify the column type,
        ;google chart wants it for pie charts
        table (if (empty? columnTypes)
                t
                (update-in t [:cols]
                           (fn [cols]
                             (map #(assoc %1 :type %2) cols columnTypes))))]
    (json {:reqId (reqId tqx)
           :table table})))
    
;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/burndown"] {:keys [team sprint]}
  (redirect (str "/burndown/" team "/" sprint)))
(defpage "/csv/burndown/:team/:sprint" {:keys [team sprint]}
  (csv (burndown team sprint) (str "burndown_" team "_" sprint)))
(defpage "/burndown/:team/:sprint" {:keys [team sprint tqx]}
  (try+
    (datasource tqx (burndown team sprint))
    (catch [:status 401] []
      (status 401 "Login"))))

;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/cumulative"] {:keys [team sprint]}
  (redirect (str "/cumulative/" team "/" sprint)))
(defpage "/csv/cumulative/:team/:sprint" {:keys [team sprint]}
  (csv (cumulative team sprint) (str "cumulative_" team "_" sprint)))
(defpage "/cumulative/:team/:sprint" {:keys [team sprint tqx]}
  (try+
    (let [content (cumulative team sprint)]
      (datasource tqx content))
    ; TODO: this doesn't work - make a wrapper for all services
    ; which need authentication
    (catch [:status 401] []
      (status 401 "Login"))))

(defpage "/teams" []
  (try+
    (json (sort-by clojure.string/upper-case (names "Team")))
    (catch [:status 401] []
      (status 401 "Please Login"))))

(defpage [:post "/customers"] {:keys [team sprint]}
  (redirect (str "/customers/" team sprint)))
(defpage "/csv/customers/:team/:sprint" {:keys [team sprint]}
  (csv (customers team sprint) (str "customers_" team "_" sprint)))
(defpage "/customers/:team/:sprint" {:keys [team sprint tqx]}
  (try+
    (datasource tqx (customers team sprint) "string" "number")
    (catch [:status 401] []
      (status 401 "Login"))))

(defpage "/team-sprints" []
         ;TODO: find a better way to propogate 401
 (try
  (try+
    (json (team-sprints))
    (catch [:status 401] []
      (status 401 "Please login")))
         (catch Exception e
           (println "whoops " e)
           (status 401 "Bah"))))

(defpage "/velocity/:team/:sprint" {:keys [team sprint tqx]}
         (datasource tqx (velocity team sprint) "string" "number"))
