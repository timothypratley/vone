(ns vone.models.version-one-request
  (:use [vone.helpers])
  (:require [clj-http.client :as client]
            [clojure.xml :as xml]
            [ring.util.codec :as codec]
            [noir.session :as session]))

;TODO: use with-connection-pool from clj-http

(def base-url "http://www3.v1host.com/Tideworks/VersionOne/rest-1.v1")

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
                      (or (parse-double c) (parse-date c) c)
                      not-found)]
                 c))
    :else x))

(defn xhr
  "XmlHttpRequest from VersionOne"
  [query]
  (let [url (str base-url query)
        params {:basic-auth [(session/get :username)
                             (session/get :password)]}]
    (try
      (client/get url params)
      (catch Exception e
        (println "xhr failed (" (session/get :username) \: e \) url)
        (throw e)))))

(defn xml-collapse
  "Converts XML into a map"
  [x not-found]
  (try
    (-> (java.io.ByteArrayInputStream. (.getBytes x "UTF8"))
      xml/parse
      (collapse not-found))
    (catch Exception e
      (println x)
      (throw e))))

;public
(defn request-transform
  ([query transform]
   (request-transform query transform 0))
  ([query transform not-found]
   (let [result (-> (xhr query)
                  :body
                  (xml-collapse not-found))]
     ;TODO: use a safety call pattern instead
	   (try
	     (transform result)
	     (catch Exception e
	       (println "transform failed:" result)
           (throw e))))))

(defn unmap
  [fields m]
  (cond
    (map? m) (map m (map codec/url-decode fields))
    (coll? m) (map (partial unmap fields) m)
    :else m))

;public
(defn request-flat
  "Makes an xml http request, collapses it,
   and replaces key:value entities with the value
   for each of the fields in the order supplied"
  ([query fields]
   (request-flat query fields 0))
  ([query fields not-found]
   (try
     (unmap fields
            (xml-collapse (:body (xhr query)) not-found))
     (catch Exception e
       ;TODO: treat 401 as an expected failure, no need to log
       (println query)
       (println fields)
       (throw e)))))

