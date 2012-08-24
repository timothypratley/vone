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
     [:meta {:name "viewport"
             :content "width=device-width"
             :initial-scale "1.0"}]
     [:link {:rel "icon"
             :href "/img/favicon.ico"
             :type "image/x-icon"}]
     [:link {:rel "shortcut"
             :href "/img/favicon.ico"
             :type "image/x-icon"}]
     (include-css "/css/bootstrap.min.css")
     (include-css "/css/bootstrap-responsive.min.css")]
    
    [:body {:authenticate "loginbox"}
     
		 [:header.navbar
      [:div.navbar-inner
       [:a.brand {:href "/"} [:strong "Vone"]]
       [:ul.nav
        [:li.divider-vertical]
        [:li (link-to "/" "Home")]
        [:li.divider-vertical]
        [:li (link-to "/#/retro" "Retrospective")]
        [:li.divider-vertical]
        [:li (link-to "/#/proj" "Projection")]
        [:li.divider-vertical]]
       [:div.login.ng-cloak.pull-right {:ng-show "!username"}
        (link-to "#/login" "Login")]
       [:div.logout.ng-cloak {:ng-show "username"}
        [:span "{{username}}"]
        (submit-button {:ng-click "logout()"} "logout")]]]
   
     [:div#loginbox.modal.hide.fade {:tabindex -1
                                     :role "dialog"
                                     :aria-labelledby "Login"
                                     :aria-hidden "true"}
      [:div.modal-header
       [:button.close {:type "button"
                       :data-dismiss "modal"
                       :aria-hidden "true"} "x"]]
      [:div.modal-body
       [:form {:ng-controller "LoginCtrl"
               :ng-submit "submit()"
               :novalidate true}
        [:div (label "username" "Username") (text-field {:ng-model "username"} "username")]
        [:div (label "password" "Password") (password-field {:ng-model "password"} "password")]      
        (submit-button "VersionOne Login")]]
      ;TODO: should have a modal-footer with submit, but then no form?
      ]
     
     [:div#content.ng-view "Loading..."]
     (include-js "/js/jquery-1.8.0.min.js")
     (include-js "https://www.google.com/jsapi")
     (include-js "/js/angular-1.0.1.min.js")
     (include-js "/js/angular-resource-1.0.1.min.js")
     (include-js "/js/http-auth-interceptor.js")
     (include-js "/js/bootstrap.min.js")
     (include-js "/js/controllers.js")
     (include-js "/js/vone.js")]))

(defpage "/login" []
  (html
    [:form {:ng-submit "submit()"
           :novalidate true} 
          [:div (label "username" "Username") (text-field "username")]
          [:div (label "password" "Password") (password-field "password")]
          (submit-button "VersionOne Login")]))

(defpage [:post "/login"] {:keys [username password]}
  (println "login" username)
  (session/put! :username username)
  (session/put! :password password))

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
    [:h1 "Retrospective: {{sprint.team}}"]
    [:div [:select {:ng-model "sprint"
                    :ng-options "ts.sprint group by ts.team for ts in teamSprints"}
           [:option {:value ""} "-- choose team --"]]]
    [:hr]
    [:div {:title "Burndown - Total ToDo Remaining"
           :chart "Area"
           :source "/burndown/TC+Sharks/TC1211"
           :vtitle "ToDo Hours"
           :htitle "Day"
           :isstacked "false"   ;TODO: using a bool the value is lost
           :areaopacity 0.0}]
    [:div {:title "Burndown Comparison"
           :chart "Area"
           :source "/burndown/TC+Sharks/TC1210"
           :vtitle "ToDo Hours"
           :htitle "Day"
           :isstacked "false"
           :areaopacity 0.0}]
    [:div {:title "Cumulative Flow - Story Status Over Time"
           :chart "Area"
           :source "/cumulative/TC+Sharks/TC1211"
           :vtitle "Story Points"
           :htitle "Day"
           :isStacked "true"
           :areaOpacity 0.8}]
    [:div {:title "Previous Cumulative Flow"
           :chart "Area"
           :source "/cumulative/TC+Sharks/TC1210"
           :vtitle "Story Points"
           :htitle "Day"
           :isStacked "true"
           :areaOpacity 0.8}]
    [:div {:title "Velocity - Story Points per Sprint"
           :chart "Column"
           :source "/velocity/TC+Sharks/TC1211"
           :vtitle "Story Points"
           :htitle "Sprint"}]
    [:div {:title "Estimation"
           :chart "Table"
           :source "/estimates/TC+Sharks/TC1211"}]
    [:div {:style "width:800; height:400"} "Stories"]
    [:div "Splits"]
    [:div {:chart "Pie"
           :source "/customers/TC+Sharks/TC1211"
           :title "Customer Focus - Points per Customer"}]
    [:div "Summary"]
    
    [:div {:chart "Pie"
           :source "/customers/TC+Sharks/TC1212"
           :title "Next Sprint"}]
    [:div "Epics"]))

