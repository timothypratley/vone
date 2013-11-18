(ns vone.config
  (:require [clojure.java.io]
            [noir.session :as session]))

(def properties
  (with-open [^java.io.Reader reader (clojure.java.io/reader "vone.properties")]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} props))))



