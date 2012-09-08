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

(defn column-type
  [s]
  (if (number? s)
    "number"
    "string"))

(defn reqId
  [tqx]
  (if tqx
    (let [match (re-find #"reqId:(\d+)" tqx)]
      (parse-int (second match)))
    0))

(defn tabulate
  [content]
  {:cols (map #(hash-map :label %1 :type %2)
              (first content)
              (map column-type (second content)))
   :rows (map #(hash-map
                 :c (map (partial hash-map :v) %))
              (rest content))})

(defn datasource
  "Google charts datasource"
  [tqx content]
  (json {:reqId (reqId tqx)
         :table (tabulate content)}))

(defmacro tss
  "Team Sprint Service
   connects a query function to output as json, csv and datasource"
  [query]
  `(do
     (defpage ~(str "/json/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint")]}
              (try+
                (json (~(symbol query) ~(symbol "team") ~(symbol "sprint")))
                (catch [:status 401] []
                  (status 401 "Please login"))))
     (defpage ~(str "/csv/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint")]}
              (try+
                (csv (~(symbol query) ~(symbol "team") ~(symbol "sprint")
                  (str ~query \_ ~(symbol "team") \_ ~(symbol "sprint"))))
                (catch [:status 401] []
                  (status 401 "Please login"))))
     (defpage ~(str "/ds/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint") ~(symbol "tqx")]}
              (try+
                (datasource ~(symbol "tqx")
                  (~(symbol query) ~(symbol "team") ~(symbol "sprint")))
                (catch [:status 401] []
                  (status 401 "Please login"))))))
    
(tss "burndown")
(tss "burndownComparison")
(tss "cumulative")
(tss "cumulativePrevious")
(tss "velocity")
(tss "estimates")
(tss "customers")
(tss "customersNext")

(defpage "/team-sprints" []
  (try+
    (json (team-sprints))
    (catch [:status 401] []
      (status 401 "Please login"))))
