(ns vone.views.welcome
  (:require [noir.session :as session]
            [noir.response :as response])
  (:use [noir.core]
        [hiccup.core]
        [hiccup.form-helpers]
        [hiccup.page-helpers]))

(defpage "/" []
  (html5
    [:head
     [:title "vone"]
     (include-css "/css/reset.css")]
    [:body
     [:div.ng-view "Loading..."]
     (include-js "https://www.google.com/jsapi")
     (include-js "/js/angular-1.0.1.min.js")
     (include-js "/js/angular-resource-1.0.1.min.js")
     (include-js "/js/controllers.js")
     (include-js "/js/vone.js")]))

(defpage "/login" []
  (html
    (form-to [:post "/login"]
             [:div (label "username" "Username") (text-field "username")]
             [:div (label "password" "Password") (password-field "password")]
             (submit-button "VersionOne Login"))))

(defpage [:post "/login"] {:keys [username password]}
  (println "login" username)
  (session/put! :username username)
  (session/put! :password password)
  (response/redirect "/burndown"))

(defpage [:post "/logout"] []
         (println "logout")
         (session/clear!))


(defpage "/about" []
         (html
           [:h1 "VersionOne Reporting"]
           [:ul
            [:li (link-to "/#/login" "Login")]
            [:li (link-to "/#/retro" "Retrospective")]]))

(defpage "/burndown" []
  (html
    (form-to [:post "/burndown"]
             [:div (label "sprint" "Sprint") (text-field "sprint")]
             [:div (label "team" "Team") (text-field "team")]
             (submit-button "Get Burndown"))))

(defpage "/cumulative" []
  (html
    (form-to [:post "/cumulative"]
             [:div (label "sprint" "Sprint") (text-field "sprint")]
             [:div (label "team" "Team") (text-field "team")]
             (submit-button "Get Cumulative Flow"))))

(defpage "/retro" []
  (html
    [:div [:select :ng-model="team" :ng-options="t in teams"
           [:option {:value ""} "-- choose team --"]]
          [:select :ng-model="sprint" :ng-options="s in sprints"
           [:option {:value ""} "-- choose sprint --"]]]
    [:div {:chart "Area"
           :source "/burndown/TC+Sharks/TC1211"
           :title "Burndown - Total ToDo Remaining"
           :vtitle "ToDo Hours"
           :htitle "Day"
           :isstacked "false"   ;TODO: using a bool the value is lost
           :areaopacity 0.0}]
    [:div {:chart "Area"
           :source "/burndown/TC+Sharks/TC1210"
           :title "Burndown Comparison"
           :vtitle "ToDo Hours"
           :htitle "Day"
           :isstacked "false"
           :areaopacity 0.0}]
    [:div {:chart "Area"
           :source "/cumulative/TC+Sharks/TC1211"
           :title "Cumulative Flow - Story Status Over Time"
           :vAxis {:title "Story Points"}
           :hAxis {:title "Day"}
           :isStacked "true"
           :areaOpacity 1.0}]
    [:div {:chart "Area"
           :source "/cumulative/TC+Sharks/TC1210"
           :title "Previous Cumulative Flow"
           :vtitle "Story Points"
           :htitle "Day"
           :isStacked "true"
           :areaOpacity 1.0}]
    [:div#stories {:style "width:800; height:400"}]
    [:div {:chart "Pie"
           :source "/customers/TC+Sharks/TC1211"
           :title "Customer Focus"}]))

