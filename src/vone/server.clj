(ns vone.server
  (:require [noir.server :as server]
            [vone.views.welcome]
            [vone.views.service]))

(def handler (server/gen-handler {:ns 'vone
                                  :base-url "/vone"}))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'vone})))

