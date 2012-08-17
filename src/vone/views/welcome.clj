(ns vone.views.welcome
  (:require [vone.views.common :as common]
            [noir.session :as session]
            [noir.response :as response])
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

(defpage "/burndown" []
  (common/layout
    (form-to [:post "/burndown"]
             [:div (label "sprint" "Sprint") (text-field "sprint")]
             [:div (label "team" "Team") (text-field "team")]
             (submit-button "Get Burndown"))))

(defpage "/cumulative" []
  (common/layout
    (form-to [:post "/cumulative"]
             [:div (label "sprint" "Sprint") (text-field "sprint")]
             [:div (label "team" "Team") (text-field "team")]
             (submit-button "Get Cumulative Flow"))))

(defpage "/retro/:team/:sprint" []
  (html5
    [:head
     [:title "vone"]
     (include-css "/css/reset.css")]
    [:body
     [:div#wrapper
      [:div#burndown {:style "width:400; height:300"}]
      [:div#burndowns {:style "width:400; height:300"}]
      [:div#cumulative {:style "width:400; height:300"}]
      [:div#previous_cumulative {:style "width:400; height:300"}]
      [:div#stories {:style "width:400; height:300"}]
      [:div#customers {:style "width:400; height:300"}]]
     (include-js "https://www.google.com/jsapi")
     (include-js "/js/charts.js")]))
  