(ns vone.views.welcome
  (:require [vone.views.common :as common]
            [noir.session :as session]
            [noir.response :as response]
            [clj-time.core :as time])
  (:use [vone.models.queries]
        [noir.core]
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

;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/burndown"] {:keys [team sprint]}
  (response/redirect (str "/burndown/" team "/" sprint)))

(defpage "/burndown/:team/:sprint" {:keys [team sprint]}
  (println "burndown " team sprint)
  (str (clojure.string/join "," (for-sprint (session/get :username) (session/get :password) team sprint todo-on))))

(defpage "/cumulative" []
  (common/layout
    (form-to [:post "/cumulative"]
             [:div (label "sprint" "Sprint") (text-field "sprint")]
             [:div (label "team" "Team") (text-field "team")]
             (submit-button "Get Cumulative Flow"))))

;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/cumulative"] {:keys [team sprint]}
  (response/redirect (str "/cumulative/" team "/" sprint)))

(defpage "/cumulative/:team/:sprint" {:keys [team sprint]}
  (println "cumulative " team sprint)
  (let [username (session/get :username)
        password (session/get :password)
        statuses (names username password "StoryStatus")
        f (partial cumulative-on statuses)]
    (str (clojure.string/join "," (for-sprint username password team sprint f)))))
