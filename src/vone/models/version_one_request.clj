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
  [x]
  (cond
    (and (vector? x) (string? (first x))) (first x)
    (vector? x) (let [s (map collapse x)]
                  (if (vector? (first s))   ;key-value pairs
                    (into {} s)
                    s))
    (map? x) (let [c (collapse (:content x))]
               (if-let [n (-> x :attrs :name)]
                 [n (if c
                      (or (parse-double c) (parse-date c) c)
                      0)]
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
        (println "xhr failed:" url)
        (throw e)))))

(defn xml-collapse
  "Converts XML into a map"
  [x]
  (try
    (-> (java.io.ByteArrayInputStream. (.getBytes x))
      xml/parse
      collapse)
    (catch Exception e
      (println x)
      (throw e))))

;public
(defn request-transform
  [query transform]
  (let [result (-> (xhr query)
                 :body
                 xml-collapse)]
    ;TODO: use a safety call pattern instead
	  (try
	    (transform result)
	    (catch Exception e
	      (println "transform failed:" result)))))

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
  [query fields]
  (try
	  (->> (xhr query)
	    :body
	    xml-collapse
	    (unmap fields))
    (catch Exception e
      ;TODO: treat 401 as an expected failure, no need to log
      (println query)
      (println fields)
      (throw e))))
