(ns vone.scratch
  (:require [clojure.repl :refer :all]
            [vone.views.services]))


(defn functions
  "Get the public functions of a namespace"
  [n]
  (filter fn? (ns-publics n)))

(functions 'vone.views.services)
(filter fn? (ns-publics 'vone.views.services))

(defn proutes
  "Returns service routes where arguments are passed in the url path."
  [rest-ns]
  (for [[service-name service] (functions rest-ns)]
    (println service-name)))

(proutes 'vone.views.services)


(for [a [1 2 3]
      b [:a :b :c]
      :let [a 1]]
  a)

(for [arglist (-> #'map meta :arglists)]
  (clojure.string/join "/" (map keyword arglist)))


(println (name #'inc))
(meta #'inc)
(resolve nil)


