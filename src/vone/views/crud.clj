(ns vone.views.crud
  (:require [noir.session :as session]
            [noir.response :as response])
  (:use [noir.core]
        [vone.data]
        [vone.models.queries]
        [hiccup.core]
        [hiccup.form-helpers]
        [hiccup.page-helpers]))

(defpage "/read" []
         (html (read-data)))

(defpage "/save" []
         (html (save-data)))

(defpage "/merge" []
         (html 
           (dosync 
             ;TODO: refactor
             (alter rankings
                    (fn mm [orig]
                      (reduce
                        (fn reducer [m x]
                          (let [n (get x "Name")]
                            (update-in m [n] (partial merge x))))
                        orig
                    (members))))
             (alter rankings
                    (fn alterer [orig]
                      (reduce
                        (fn reducer [m wi]
                          (let [member (first wi)
                                points (second wi)]
                            (assoc-in m [member :points] points)))
                        orig
                        (workitems)))))))

