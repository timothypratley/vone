(ns vone.route-gen
  (:require [clojure.edn]
            [compojure.core :refer :all]
            [noir.response :refer [content-type status]]
            [noir.session :as session]
            [vone.helpers :refer [readable-date tostr-ds-date]]
            [clojure.data.csv :refer [write-csv]]
            [cheshire.custom :as custom]
            [slingshot.slingshot :refer [try+]]))

(custom/add-encoder org.joda.time.DateTime
  (fn [d jsonGenerator]
    (.writeString jsonGenerator (readable-date d))))

(defn- json
  "Wraps the response in the json content type
   and generates JSON from the content"
  [request content]
  (content-type "application/json; charset=utf-8"
                (custom/generate-string content)))

(defn- csv
  [request content]
  (assoc-in
    (content-type "text/csv"
      (str (doto (java.io.StringWriter.) (write-csv content))))
    [:headers "Content-Disposition"]
    (str "attachment;filename=" (clojure.string/join "_" (vals (dissoc (request :params) :tqx))) ".csv")))

(defn- parse-int
  [s]
  (if s
    (Integer/parseInt s)
    0))

(defn- column-type
  [s]
  ;(println "S:" (type s) s)
  (cond
    (number? s) "number"
    (instance? org.joda.time.DateTime s) "date"
    :else "string"))

(defn- reqId
  [tqx]
  (if tqx
    (let [match (re-find #"reqId:(\d+)" tqx)]
      (parse-int (second match)))
    0))

(defn- make-value
  [value]
  (hash-map :v (if (instance? org.joda.time.DateTime value)
                 (tostr-ds-date value)
                 value)))

(defn- tabulate
  [content]
  {:cols (map #(hash-map :label %1 :type %2)
              (first content)
              (map column-type (second content)))
   :rows (map #(hash-map
                 :c (map make-value %))
              (rest content))})

(defn- datasource
  "Google charts datasource"
  [request content]
  (json request
        {:reqId (reqId (get-in request [:params :tqx]))
         :table (tabulate content)}))

(defn- with-401
  "Catch and respond on 401 exception"
  [request fmt method args]
  (if-not (session/get :username)
    (status 401 "Please login")
    (try+
       (fmt request (apply method args))
       ;TODO: wish there was a nicer way to pass on 401
       (catch [:status 401] []
         (status 401 "Please login"))
       (catch Exception e
         (if (= "java.io.IOException: Authentication failure"
                (.getMessage e))
           (status 401 "Please login")
           (throw e))))))

(defn- doc-str [m]
  (str (:name m) \newline
       (:arglists m) \newline
       (:doc m) \newline))

(defn- parse
  ([s] (parse s nil))
  ([s t]
   (condp = t
     String s
     Integer (Integer/parseInt s)
     Boolean (Boolean/parseBoolean s)
     Double (Double/parseDouble s)
     (let [r (clojure.edn/read-string s)] (if (symbol? r) s r)))))

(defn- err-parse [s arg]
  (let [t (:tag (meta arg))]
    (try
      [(parse s (when t (resolve t))) nil]
      (catch Exception e
        [nil (str "Failed to parse " (name arg) (when t (str " as " t)) ": " e)]))))

(defn- call [f request fmt]
  (try
    (let [params (dissoc (request :params) :tqx)
          request-arity (count (keys params))
          arglists (-> f meta :arglists)
          match #(and (if params
                        (empty %)
                        (every? params (map keyword %)))
                      (= (count params) (count %)))
          arglist (first (filter match arglists))
          args (map params (map keyword arglist))
          parsed (map err-parse args arglist)
          parse-errors (map last parsed)
          parse-vals (map first parsed)
          error (cond
                 (not-any? #(= request-arity (count %)) arglists) "Wrong number of arguments"
                 (nil? arglist) "Parameters do not match"
                 (some identity parse-errors) (clojure.string/join \newline parse-errors))]
      (if error
        (status 400 (str error \newline (doc-str (meta f))))
        (with-401 request fmt f parse-vals)))))

(defn page-routes
  "Returns routes for pages defined in a namespace as public functions with no arguments"
  [page-ns]
  (for [[page-name page] (ns-publics page-ns)]
    (GET (str "/" page-name) request (page))))

(defn service-routes
  "Returns routes for services defined in a namespace as public functions.
  Services have their arguments checked and return helpful error messages."
  [service-ns]
  (for [[service-name service] (ns-publics service-ns)
        fmt [#'json #'datasource #'csv]
        :let [route (str "/" (-> fmt meta :name) "/" service-name)]]
    (do
      ;  (println route)
      (POST route request
            (call service request fmt)))))

(defn rest-routes
  "Returns routes per arity"
  [rest-ns]
  (for [[service-name service] (ns-publics rest-ns)
        fmt [#'json #'datasource #'csv]
        arglist (-> service meta :arglists)
        :let [route (str "/" (-> fmt meta :name)
                         "/" service-name (when (seq arglist) "/")
                         (clojure.string/join "/" (map keyword arglist)))]]
    (do
      (println route)
      (GET route request
           (call service request fmt)))))

