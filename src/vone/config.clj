(ns vone.config
  (:require [clojure.java.io :refer [reader resource]]
            [noir.session :as session]))

(def properties
  (with-open [^java.io.Reader reader (reader (resource "vone.properties"))]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} props))))




