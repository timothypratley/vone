(ns vone.json-tests
  (:require [cheshire.custom :as custom]
            [noir.response :as response])
  (:use [vone.helpers]
        [clojure.test]))


(deftest test-nil-json
         (is (not= nil (response/json {:foo nil}))))

(custom/add-encoder org.joda.time.DateTime
  (fn [d jsonGenerator]
    (.writeString jsonGenerator (readable-date d))))

(defn json
  "Wraps the response in the json content type
   and generates JSON from the content"
  [content]
  (response/content-type "application/json; charset=utf-8"
                (custom/generate-string content)))

(println (json (org.joda.time.DateTime.)))


