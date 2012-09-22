(ns vone.server
  (:require [noir.server :as server]))

(server/load-views "src/vone/views/")

(def handler (server/gen-handler {:ns 'vone}))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'vone})))

