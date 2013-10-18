(ns vone.routes
  (:require [clojure.edn]
            [compojure.core :refer :all]
            [noir.response :as response]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [vone.views.pages]
            [vone.models.queries]
            [noir.util.middleware :as nm]))

(defn- doc-str [m]
  (str (when-let [ns (:ns m)] (str (ns-name ns) "/")) (:name m) \newline
       (:arglists m) \newline
       (:doc m) \newline))

(defn parse
  ([s] (parse s nil))
  ([s t]
   (condp = t
     String s
     Integer (Integer/parseInt s)
     Boolean (Boolean/parseBoolean s)
     Double (Double/parseDouble s)
     (let [r (clojure.edn/read-string s)] (if (symbol? r) s r)))))

(defn err-parse [s arg]
  (let [t (:tag (meta arg))]
    (try
      [(parse s (resolve t)) nil]
      (catch Exception e
        [nil (str "Failed to parse " (name arg) (when t (str " as " t)))]))))

(defn call [f request]
  (try
    (let [params (request :params)
          request-arity (count (keys params))
          arglists (-> f meta :arglists)
          match #(and (if params
                        (empty %)
                        (every? params (map name %)))
                      (= (count params) (count %)))
          arglist (first (filter match arglists))
          args (map params (map name arglist))
          parsed (map err-parse args arglist)
          parse-errors (map last parsed)
          parse-vals (map first parsed)
          error (cond
                 (not-any? #(= request-arity (count %)) arglists) "Wrong number of arguments"
                 (nil? arglist) "Parameters do not match"
                 (some identity parse-errors) (clojure.string/join \newline parse-errors))]
      (if error
        (str error \newline (doc-str (meta f)))
        (apply f parse-vals)))))

(defn json [x] x)
(defn ds [x] x)
(defn csv [x] x)

(let [pages (for [[page-name page] (ns-publics 'vone.views.pages)]
              (GET (str "/" page-name) request
                   (page)))
      services (for [[service-name service] (ns-publics 'vone.models.queries)
                     fmt [json ds csv]]
                 (POST (str "/" fmt "/" service-name) request
                       (fmt (call service request))))]
  (def app-routes
    (apply routes
           (concat
            pages
            services
            [(route/resources "/")
             (GET "" [] (response/redirect "/vone/"))
             (route/not-found "Not Found")]))))

(def app
  (handler/site app-routes))
