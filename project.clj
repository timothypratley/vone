(defproject vone "0.1.0-SNAPSHOT"
            :description "FIXME: write this!"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [noir "1.2.2"]
                           [cheshire "4.0.2"]
                           [clj-http-lite "0.1.0"]
                           [slingshot "0.10.3"]
                           [clj-time "0.4.4"]
                           [org.clojure/data.csv "0.1.2"]]
            :dev-dependencies [[appengine-magic "0.5.0"]
                               [lein-ring "0.7.5"]]
            :ring {:handler vone.server/handler}
            :main vone.server)

