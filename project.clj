(defproject vone "0.3.1"
  :description "VersionOne Reporting"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [ring-server "0.3.0"]
                 [hiccup "1.0.4"]
                 [lib-noir "0.7.1"]
                 [cheshire "5.2.0"]
                 [clj-http-lite "0.2.0"]
                 [slingshot "0.10.3"]
                 [clj-time "0.6.0"]
                 [org.clojure/data.csv "0.1.2"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}}
  :plugins [[appengine-magic "0.5.0"]
            [lein-midje "3.0.0"]
            [lein-ring "0.8.6"]
            [lein-ancient "0.4.4"]]
  :ring {:handler vone.app-servlet/app-handler})



