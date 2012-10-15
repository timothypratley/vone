(ns vone.views.service
  (:require [cheshire.custom :as custom]
            [noir.session :as session])
  (:use [vone.models.queries]
        [vone.helpers]
        [clojure.data.csv :only [write-csv]]
        [noir.core]
        [noir.response :only [redirect content-type status]]
        [slingshot.slingshot :only [try+]]))

(custom/add-encoder org.joda.time.DateTime
  (fn [d jsonGenerator]
    (.writeString jsonGenerator (readable-date d))))

(defn json
  "Wraps the response in the json content type
   and generates JSON from the content"
  [content]
  (content-type "application/json; charset=utf-8"
                (custom/generate-string content)))

(defn csv
  [filename content]
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

(defn with-401
  "Catch and respond on 401 exception"
  [service method & args]
  (if-not (session/get :username)
    (status 401 "Please login")
    (try+
       (service (apply method args))
       ;TODO: wish there was a nicer way to pass on 401
       (catch [:status 401] []
         (status 401 "Please login"))
       (catch Exception e
         (if (= "java.io.IOException: Authentication failure"
                (.getMessage e))
           (status 401 "Please login")
           (throw e))))))

(defmacro tss
  "Team Sprint Service
   connects a query function to output as json, csv and datasource"
  [query]
  `(do
     (defpage ~(str "/json/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint")]}
              (with-401 json
                        ~(symbol query)
                        ~(symbol "team") ~(symbol "sprint")))
     (defpage ~(str "/csv/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint")]}
              (with-401 (partial csv (str ~query \_ ~(symbol "team") \_ ~(symbol "sprint")))
                        ~(symbol query)
                        ~(symbol "team") ~(symbol "sprint")))
     (defpage ~(str "/ds/" query "/:team/:sprint")
              {:keys [~(symbol "team") ~(symbol "sprint") ~(symbol "tqx")]}
              (with-401 (partial datasource ~(symbol "tqx"))
                        ~(symbol query)
                        ~(symbol "team") ~(symbol "sprint")))))

;TODO: figure these out with reflection over team sprint queries public functions
(tss "burndown")
(tss "burndownComparison")
(tss "cumulative")
(tss "cumulativePrevious")
(tss "velocity")
(tss "estimates")
(tss "customers")
(tss "customersNext")
(tss "stories")
(tss "defects")
(tss "testSets")
(tss "splits")
(tss "participants")
(tss "feedback")

;TODO: expose csv versions
(defpage "/json/team-sprints" []
  (with-401 json team-sprints))

(defpage "/json/sprint-span/:sprint" {:keys [sprint]}
  (with-401 json sprint-span sprint))

(defpage "/csv/projections" []
  (with-401 (partial csv "projections.csv") projections))
(defpage "/json/projections" []
  (with-401 json projections))
(defpage "/ds/projections" {:keys [tqx]}
  (with-401 (partial datasource tqx) projections))

