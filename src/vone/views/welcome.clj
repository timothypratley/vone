(ns vone.views.welcome
  (:require [vone.views.common :as common]
            [noir.session :as session]
            [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-time.core :as time]
            [clojure.pprint :as pprint])
  (:use [noir.core]
        [hiccup.core]
        [hiccup.form-helpers]
        [hiccup.page-helpers]))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to vone"]))

(defpage "/login" []
  (common/layout
    (form-to [:post "/login"]
             [:div (label "username" "Username")
              (text-field "username")]
             [:div (label "password" "Password")
              (password-field "password")]
             (submit-button "VersionOne Login"))))



;TODO: move to another file?
(def data-url "http://www3.v1host.com/Tideworks/VersionOne/rest-1.v1/Data")
(def getit (comp first :content first :content))
(defn getid [asset]
  (-> asset
    :attrs
    :id
    (.substring 5)
    (Integer/parseInt)))

(defn m [team sprint d]
  (let [date-str (.format (java.text.SimpleDateFormat. "yyyy-MM-dd'T23:59:59'") d)]
    (str "/Timebox/" 475626 "/Workitems[Team.Name='" team "'].ToDo.@Sum?asof=" date-str)))

(defpage [:post "/login"] {:keys [username password]}
  (println "login" username)
  (let [s (client/get (str data-url "/Team?sel=Name")
                      {:as :stream
                       :basic-auth [username password]})
        x (xml/parse (s :body))
        z (zip/xml-zip x)
        teams (map (fn [asset] [(getid asset) (getit asset)])
                   (x :content))]
    (println (pprint/pprint teams))
    (doseq [d (Days/between start end)]
      (m))
    
    (session/put! :username username)
    (session/put! :password password)
    (common/layout
      (form-to [:post "/burndown"]
               (label "sprint" "Sprint")
               (text-field "sprint")
               (label "team" "Team")))))

(defpage [:post "/logout"] []
         (println "logout")
         (session/clear!))
