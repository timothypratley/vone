(ns vone.app_servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use [appengine-magic.servlet :only [make-servlet-service-method]]
        [appengine-magic.multipart-params :only [wrap-multipart-params]])
  (:require [appengine-magic.core :as gae]
            [noir.util.gae :as noir-gae]
            [noir.server.handler :as handler]
            [vone.middleware.httpsession :as httpsession]))

;; custom middlewares
(handler/add-custom-middleware wrap-multipart-params)

;; load views (must manually load all routes)
(require 'vone.views.welcome)
(require 'vone.views.service)

;; def the ring handler with httpsession
;; must also enable sessions in appengine-web.xml
(def ring-handler
  (httpsession/wrap-http-session-store
    (noir-gae/gae-handler
      {:session-store (httpsession/http-session-store "my-app-session")})))

;; def the appengine app
(gae/def-appengine-app my-site ring-handler
  :war-root "/Users/me/Devel/my-app/war")

;; entry point
(defn -service [this request response]
  ((make-servlet-service-method my-site) this request response))

