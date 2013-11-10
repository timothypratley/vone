(ns vone.app-servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:require [compojure.core :refer :all]
            [compojure.handler :refer [site]]
            [compojure.route :refer [resources not-found]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            ;[appengine-magic.servlet :refer [make-servlet-service-method]]
            ;[appengine-magic.multipart-params :refer [wrap-multipart-params]]
            ;[appengine-magic.core :as gae]
            [routegen.core :refer :all]
            [vone.views.pages :refer [home login ping logout]]
            [vone.views.services]))


(def app-routes
  (apply routes
         (concat
          (page-routes 'vone.views.pages)
          (path-routes 'vone.views.services)
          (post-routes 'vone.views.services)
          [(GET "/" [] (home))
           (POST "/login" [username password] (login username password))
           (GET "/ping" [] (ping))
           (GET "/logout" [] (logout))
           (GET "" [] (redirect "/"))
           (resources "/")
           (not-found "Not Found")])))

(def app-handler
  (session/wrap-noir-session (site app-routes)))

;; def the appengine app for Google App Engine deployment
;(gae/def-appengine-app app-handler)

;; entry point for a regular WAR style deployment
;(defn -service [this request response]
;  ((make-servlet-service-method app-handler) this request response))









