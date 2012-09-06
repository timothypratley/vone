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

(defmacro ts [x]
  `(do
     (defpage ~(str "/csv/" x "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint")]}
              (csv (~(symbol x) ~(symbol "team")
                       ~(symbol "sprint")
                       (str ~x \_ ~(symbol "team") \_ ~(symbol "sprint")))))
     (defpage ~(str "/ds/" x "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint") ~(symbol "tqx")]}
              (datasource ~(symbol "tqx")
                          (~(symbol x) ~(symbol "team") ~(symbol "sprint"))))))
    
(ts "burndown")
(ts "burndownComparison")
(ts "cumulative")
(ts "cumulativePrevious")
; TODO: "string" "number"
(ts "velocity")
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

