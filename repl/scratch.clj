(ns vone.scratch
  (:require [clojure.repl :refer :all]))



(for [a [1 2 3]
      b [:a :b :c]
      :let [a 1]]
  a)

(for [arglist (-> #'map meta :arglists)]
  (clojure.string/join "/" (map keyword arglist)))


(println (name #'inc))
(meta #'inc)
(resolve nil)
