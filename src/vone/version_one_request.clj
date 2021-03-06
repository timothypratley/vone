(ns vone.version-one-request
  (:require [vone.helpers :refer :all]
            [vone.config :refer :all]
            [clj-http.lite.client :as client]
            [clojure.xml :as xml]
            [ring.util.codec :as codec]
            [noir.session :as session]))


;TODO: with-connection-pool from clj-http might be faster

(def base-url (properties "base-url" "v1host"))

(defn collapse
  "Converts xml into a more condensed structure"
  [x not-found]
  (cond
    (and (vector? x) (string? (first x))) (first x)
    (vector? x) (let [s (map #(collapse % not-found) x)]
                  (if (vector? (first s))   ;key-value pairs
                    (into {} s)
                    s))
    (map? x) (let [c (collapse (:content x) not-found)]
               (if-let [n (-> x :attrs :name)]
                 [n (if c
                      (or (parse-double c) (parse-date c) (parse-date-full c) c)
                      not-found)]
                 c))
    :else x))

(defn as-url
  [query args]
  (str base-url query \? (codec/url-decode (client/generate-query-string args))))

(defn xhr
  "XmlHttpRequest from VersionOne"
  [query args]
  (let [url (str base-url query)
        params {:basic-auth [(or (try (session/get :username) (catch Exception e)) (properties "username" "none"))
                             (or (try (session/get :password) (catch Exception e)) (properties "password" "none"))]
                :query-params args}]
    (try
      (client/get url params)
      (catch Exception e
        ;TODO: treat 401 as an expected failure, no need to log
        ;... but do want to log other errors
        (println "xhr failed (" (first (params :basic-auth)) \@ (as-url query args) \))
        (throw e)))))

(defn clean-xml
  "Version One can return invalid characters converted to string &#x19; which is invalid XML"
  [x]
  (clojure.string/replace x #"&#x[0-9A-Fa-f]+;" ""))

(defn xml-collapse
  "Converts XML into a map"
  [x not-found]
  (-> (java.io.ByteArrayInputStream. (.getBytes (clean-xml x) "UTF8"))
      xml/parse
      (collapse not-found)))

(defn request
  "Makes an xml http request and collapses the body"
  ([query args]
   (request query args 0))
  ([query args not-found]
   (let [response (xhr query args)
         entities (xml-collapse (response :body) not-found)]
     entities)))

(defn unmap
  "Replaces key:value entities with the value
  for each of the fields in the order supplied"
  [fields col]
  (map #(map (partial get %) fields) col))

(defn request-rows
  "Get rows of values with columns in the order you specified"
  ([query args]
   (request-rows query args 0))
  ([query args not-found]
   (unmap (clojure.string/split (args :sel) #",")
          (request query args not-found))))
