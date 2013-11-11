(ns vone.app-servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:require [compojure.core :refer :all]
            [compojure.handler :refer [site api]]
            [compojure.route :refer [resources not-found]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.response :refer [content-type status]]
            [slingshot.slingshot :refer [try+]]
            ;[appengine-magic.servlet :refer [make-servlet-service-method]]
            ;[appengine-magic.multipart-params :refer [wrap-multipart-params]]
            ;[appengine-magic.core :as gae]
            [routegen.core :refer :all]
            [vone.views.pages :refer [home login]]
            [vone.views.services]))


(defn with-401
  "Catch and respond on 401 exception"
  [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:status 401] []
       (status 401 "Please login"))
     (catch Exception e
       (if (= "java.io.IOException: Authentication failure" (.getMessage e))
         (status 401 "Please login")
         (throw e))))))

(def api-routes
  (apply routes
         (concat
          (path-routes 'vone.views.services)
          (post-routes 'vone.views.services))))

(def site-routes
  (apply routes
         (concat
          (page-routes 'vone.views.pages)
          [(GET "/" [] (home))
           (GET "" [] (redirect "/"))
           (POST "/login" [username password] (login username password))
           (resources "/")
           (not-found "Not Found")])))

(defroutes app-routes
  (api api-routes)
  (site site-routes))

(def app-handler
  (session/wrap-noir-session app-routes))

;; def the appengine app for Google App Engine deployment
;(gae/def-appengine-app app-handler)

;; entry point for a regular WAR style deployment
;(defn -service [this request response]
;  ((make-servlet-service-method app-handler) this request response))











