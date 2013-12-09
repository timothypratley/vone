(ns vone.views.pages
  (:require [noir.session :as session]
            [noir.response :as response]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.page :refer :all]
            [hiccup.element :refer :all]))

(defn logout []
  (println "logout" (session/get :username))
  (session/clear!)
  "Logged out")

(defn login
  ([]
   (html
    [:form {:ng-submit "submit()"
            :novalidate true}
     [:div (label "username" "Username") (text-field {:ng-model "username"} "username")]
     [:div (label "password" "Password") (password-field {:ng-model "password"} "password")]
     (submit-button "VersionOne Login")]))
  ([username password]
   (println "login" username)
   (session/put! :username username)
   (session/put! :password password)
   username))

(defn home []
  (html5
   [:head
    ; When run on an intranet, IE defaults to compatibility
    ; which does not work for Google Visualization library
    [:title "Vone: Version One Custom Reporting"]
    [:meta {:http-equiv "X-UA-Compatible"
            :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport"
            :content "width=device-width,initial-scale=1.0"}]
    [:link {:rel "icon"
            :href "img/favicon.ico"
            :type "image/x-icon"}]
    [:link {:rel "shortcut"
            :href "img/favicon.ico"
            :type "image/x-icon"}]
    (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.2/css/bootstrap.min.css")
    (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.2/css/bootstrap-theme.min.css")
    (include-css "/css/vone.css")
    "<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src='//oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js'></script>
    <script src='//oss.maxcdn.com/libs/respond.js/1.3.0/respond.min.js'></script>
    <![endif]-->"]

   [:body {:authenticate "loginbox"}
    [:div.container
     [:header.navbar.navbar-default {:role "banner"}
      [:div.navbar-header
       [:button.navbar-toggle {:type "button"
                               :data-toggle "collapse"
                               :data-target ".navbar-collapse"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]]
       [:a.navbar-brand {:href "#"} "Vone"]]
      [:div.collapse.navbar-collapse {:role "navigation"}
       [:ul.nav.navbar-nav
        ;[:li (link-to "#/overall" "Overall")]
        ;TODO: use angular to set the active menu
        [:li (link-to "#/retro" "Retrospective")]
        [:li (link-to "#/roadmap" "Roadmap")]
        [:li (link-to "#/fabel" "Fabel")]
        [:li (link-to "#/allteams" "All Teams")]
        [:li (link-to "#/projectdefectrate" "Project Defect Rate")]
        [:li (link-to "#/history" "Story History")]]
       [:ul.nav.navbar-nav.navbar-right.ng-cloak
        [:li.login {:ng-show "!username"}
         (link-to "/#/login" "Login")]
        [:li.logout {:ng-show "username"}
         [:span "{{username}}"]
         (submit-button {:ng-click "logout()"} "logout")]]]]]

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
       (submit-button "VersionOne Login")]]]
    ;TODO: should have a modal-footer with submit, but then no form?

    [:div.container.content
     [:div#content.ng-view "Loading..."]]

    (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js")
    (include-js "//www.google.com/jsapi")
    (include-js "//cdnjs.cloudflare.com/ajax/libs/underscore.js/1.5.2/underscore-min.js")
    (include-js "//ajax.googleapis.com/ajax/libs/angularjs/1.0.7/angular.min.js")
    (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.2/js/bootstrap.min.js")
    (include-js "/js/http-auth-interceptor.js")
    (include-js "/js/charts.js")
    (include-js "/js/controllers.js")
    (include-js "/js/vone.js")]))

(defn about []
  (html
   [:div "Please select a report from the navbar above."]))

(defn burndown []
  (html
   (form-to [:post "/burndown"]
            [:div (label "sprint" "Sprint") (text-field "sprint")]
            [:div (label "team" "Team") (text-field "team")]
            (submit-button "Get Burndown"))))

(defn cumulative []
  (html
   (form-to [:post "/cumulative"]
            [:div (label "sprint" "Sprint") (text-field "sprint")]
            [:div (label "team" "Team") (text-field "team")]
            (submit-button "Get Cumulative Flow"))))

(def select-retro
  [:div
   [:select {:ng-model "team"
             :ng-options "key as key for (key, value) in teamSprints"}
    [:option {:value ""} "-- choose team --"]]
   [:select {:ng-model "sprint"
             :ng-options "s for s in sprints"
             :ng-visible "sprints"}
    [:option {:value ""} "-- choose sprint --"]]])

(defn selectRetro []
  (html
   [:h1 "Retrospective:"]
   select-retro))

(defn retro []
  (html
   [:h1 "Retrospective: {{sprintBegin}} to {{sprintEnd}}"]
   select-retro
   [:hr]
   [:div.report {:ng-visible "team && sprint"}
    [:div {:chart "burndown"}]
    [:div {:chart "burndownComparison"}]
    [:div.break {:chart "cumulative"}]
    [:div {:chart "cumulativePrevious"}]
    [:div.break {:chart "velocity"}]
    [:div {:chart "estimates"}]
    [:div {:chart "failedReview"}]
    [:div {:chart "churnComparison"}]
    [:div {:chart "churnStories"}]
    [:div {:chart "participants"}]
    [:div.break {:chart "stories"}]
    [:div {:chart "defects"}]
    [:div {:chart "testSets"}]
    [:div {:chart "splits"}]
    [:div.break {:chart "customers"}]
    ;TODO:
    ;[:div "Epics"]
    [:div {:chart "customersNext"}]
    ;TODO: why does this have to be unsafe?
    [:div.break {:ng-bind-html-unsafe "feedback"}]]))

(defn allteams []
  (html
   [:h1 "All Teams Status: {{today}}"]
   [:div.report
    [:ul.list-unstyled
     [:li {:ng-repeat "args in argss"}
      [:br]
      [:br]
      [:h2 "{{args}}"]
      [:div.row
       [:div.col-md-6 {:chart "burndown" :height 320}]
       [:div.col-md-6 {:chart "cumulative" :height 320}]]
      [:div {:chart "churnStories"}]]]]))

(defn roadmap []
  (html
   [:h1 "Roadmap"]
   [:label.checkbox (check-box {:ng-model "showTeam"} "team") "Team"]
   [:label.checkbox (check-box {:ng-model "showProject"} "project") "Project"]
   [:label.checkbox (check-box {:ng-model "showCustomer"} "customer") "Customer"]
   (link-to "/csv/roadmap" "csv")
   [:div {:roadmap true}]))

(defn fabel []
  (html
   [:h1 "Fabel"]
   (link-to "/csv/fabel" "csv")
   [:div {:fabel true}]))

(defn overall []
  (html
   [:h1 "Overall"]
   (link-to "/csv/overall" "csv")
   [:div {:allocation true}]
   [:div {:churn true}]
   [:div {:quality true}]))

(defn members []
  (html
   [:h1 "Members"]
   [:ul
    [:li {:ng-repeat "member in members"}
     [:a {:href "/#/member/{{member[0]}}"} "{{member[0]}} {{member[1]}}"]]]))

(defn member []
  (html
   [:h1 "{{member}}"]
   [:div {:chart "workitems"}]))

(defn rankings []
  (html
   [:h1 "Rankings"]
   [:ul
    [:li {:ng-repeat "m in members"}
     [:a {:href "/#/member/{{m.name}}"} "{{m.name}}"]
     "score:{{m.score}} | points:{{m.points}} | role:{{m.role}} | teir:{{m.tier}} | team:{{m.team}}"]]))

(defn projectdefectrate []
  (html
   [:h1 "Defect Rate: {{today}}"]
   [:div.report
    [:div "Select" (submit-button {:ng-click "selectAll(true)"} "All") (submit-button {:ng-click "selectAll(false)"} "None")]
    [:label {:ng-repeat "(project,enabled) in projects"}
     [:input {:type "checkbox" :ng-model "projects[project]"} "{{project}}"]]
    [:ul.list-unstyled
     [:li {:ng-repeat "(args, enabled) in projects | filter:enabled"}
      [:hr]
      [:h2 "{{args}}"]
      [:div {:chart "defectRate"}]]]]))

(defn history []
  (html
   [:h1 "Story History"]
   (text-field {:ng-model "args"} "number")
   [:div {:chart "storyFullHistory"}]))
