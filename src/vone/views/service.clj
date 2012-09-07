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
  [tqx content]
  (json {:reqId (reqId tqx)
         :table (tabulate content)}))

(defmacro ts [query & columnTypes]
  `(do
     (defpage ~(str "/json/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint")]}
              (json (~(symbol query) ~(symbol "team") ~(symbol "sprint"))))
     (defpage ~(str "/csv/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint")]}
              (csv (~(symbol query) ~(symbol "team")
                       ~(symbol "sprint")
                       (str ~query \_ ~(symbol "team") \_ ~(symbol "sprint")))))
     (defpage ~(str "/ds/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint") ~(symbol "tqx")]}
              (datasource ~(symbol "tqx")
                          (~(symbol query) ~(symbol "team") ~(symbol "sprint"))))))
    
(ts "burndown")
(ts "burndownComparison")
(ts "cumulative")
(ts "cumulativePrevious")
(ts "velocity")
(ts "estimates")
(ts "customers")
(ts "customersNext")

(defpage "/team-sprints" []
  ;TODO: find a better way to propogate 401
  (try
    (json (team-sprints))
    ;(catch [:status 401] []
      ;(status 401 "Please login"))
    (catch Exception e
      (println "whoops " e)
      (status 401 "Bah"))))

