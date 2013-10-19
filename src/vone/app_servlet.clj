(ns vone.app-servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:require [compojure.core :refer :all]
            [appengine-magic.servlet :refer [make-servlet-service-method]]
            [appengine-magic.multipart-params :refer [wrap-multipart-params]]
            [appengine-magic.core :as gae]
            [vone.middleware.httpsession :as httpsession]
            [vone.views.pages]
            [vone.models.queries]))


(def app-routes
  (apply routes
         (concat
          (page-routes #'vone.views.pages)
          (service-routes #'vone.models.queries)
          [(route/resources "/")
           (GET "" [] (response/redirect "/vone/"))
           (route/not-found "Not Found")])))

(def app-handler
  (handler/site app-routes {:session-store (httpsession/http-session-store "vone-session")}))


(handler/add-custom-middleware wrap-multipart-params)

;; def the appengine app for Google App Engine deployment
(gae/def-appengine-app vone (httpsession/wrap-http-session-store app-handler))

;; entry point for a regular WAR style deployment
(defn -service [this request response]
  ((make-servlet-service-method app-handler) this request response))





