(ns vone.app-servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use [appengine-magic.servlet :only [make-servlet-service-method]]
        [appengine-magic.multipart-params :only [wrap-multipart-params]])
  (:require [appengine-magic.core :as gae]
            [vone.middleware.httpsession :as httpsession]
            ;; load views (must manually load all routes)
            [vone.views.welcome]
            [vone.views.service]))

;; custom middlewares
(handler/add-custom-middleware wrap-multipart-params)
(handler/add-custom-middleware wrap-strip-trailing-slash)


;; def the ring handler with httpsession
;; must also enable sessions in appengine-web.xml
(def ring-handler
  (httpsession/wrap-http-session-store
    (noir-gae/gae-handler
      {:session-store (httpsession/http-session-store "vone-session")})))

;; def the appengine app
(gae/def-appengine-app vone-site ring-handler)

;; entry point
(defn -service [this request response]
  ((make-servlet-service-method vone-site) this request response))



