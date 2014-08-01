(ns loupi.server
  (:import
   [java.net URL])
  (:require
   [loupi.controller :as controller]
   [dire.core :refer [with-handler supervise]]
   [compojure.core :refer :all]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [me.raynes.fs :as fs]
   [net.cgrand.enlive-html :as enlive]
   [liberator.core :refer [resource defresource]]
   [ring.util.response :as ring-res]
   [clojure.pprint :as pp]
   [clojure.string :as st])
  (:use
   [clojure.java.io :only [as-file input-stream output-stream] :as io]
   [ring.middleware.params]
   [ring.middleware.keyword-params]
   [ring.middleware.multipart-params]
   [ring.middleware.nested-params]
   [ring.middleware.file]
   [ring.middleware.resource]
   [ring.middleware.cookies]
   [ring.middleware.session]
   [ring.middleware.session.cookie]
   [ring.middleware.content-type]
   [ring.middleware.not-modified]
   [ring.adapter.jetty :only [run-jetty]]
   [ring.middleware.json]))



(defn example []
  (assoc
      (ring.util.response/response
       (apply str (enlive/emit* (enlive/html-resource "templates/layout.html"))))
    :headers {"Content-Type" "text/html"}))

;; a helper to create a absolute url for the entry with the given id
(defn build-entry-url [request id]
  (println " here is the id:")
  (println (str id))
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types-we-allow]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (let [vector-of-headers-sent-by-client (st/split (get-in ctx [:request :headers "content-type"]) #";")]
      (or
       (not (every? nil? (map #(some #{%} content-types-we-allow) vector-of-headers-sent-by-client)))
       [false {:message "Unsupported Content-Type"}]))
    true))

(defresource list-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(controller/request-malformed? % ::data)
  :post! (fn [ctx] (controller/list-resource-post! ctx))
  :post-redirect? false
  :location #(build-entry-url (get % :request) (get % ::id))
  :handle-ok (fn [ctx]
               (controller/list-resource-handle-ok ctx)))

(defresource entry-resource 
  :allowed-methods [:get :put :delete]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? (fn [ctx]
             true)
  :existed? (fn [ctx] (controller/entry-resource-existed? ctx))
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (controller/entry-resource-handle-ok ctx))
  :delete! (fn [ctx] (controller/entry-resource-delete! ctx))
  :malformed? #(controller/request-malformed? % ::data)
  :can-put-to-missing? true
  :can-post-to-missing? true
  :put! (fn [ctx] (controller/entry-resource-put! ctx))
  :new? (fn [ctx] (nil? nil)))

(defn preflight [request]
  "2014-07-13 - this is meant to enable CORS so our frontenders can do cross-browser requests. The browser should do a 'preflight' OPTIONS request to get permission to do other requests."
  (let [origin (get-in request [:headers "origin"])
        origin (if (or (= origin "null") (nil? origin))
                   "*"
                   origin)]
    (assoc
        (ring.util.response/response "CORS enabled")
      :headers {"Content-Type" "application/json"
                "Access-Control-Allow-Origin" (str origin)
                "Access-Control-Allow-Methods" "PUT, DELETE, POST, GET, OPTIONS, XMODIFY" 
                "Access-Control-Max-Age" "4440"
                "Access-Control-Allow-Credentials" "true"
                "Access-Control-Allow-Headers" "Authorization, X-Requested-With, Content-Type, Origin, Accept"})))

(defn wrap-cors-headers
  "Adding CORS headers to all responses"
  [handler & [opts]]
  (fn [request]
    (let [resp (handler request)
          origin (get-in request [:headers "origin"])
          origin (if (or (= origin "null") (nil? origin))
                   "*"
                   origin)
          resp (ring-res/header resp "Content-Type" "application/json")
          resp (ring-res/header resp "Access-Control-Allow-Origin" (str origin))
          resp (ring-res/header resp "Access-Control-Allow-Methods" "PUT, DELETE, POST, GET, OPTIONS, XMODIFY")
          resp (ring-res/header resp "Access-Control-Max-Age" "4440")
          resp (ring-res/header resp "Access-Control-Allow-Credentials" "true")
          resp (ring-res/header resp "Access-Control-Allow-Headers" "Authorization, X-Requested-With, Content-Type, Origin, Accept")]
      (println " in wrap-cors-headers, the origin is: " origin)
      resp)))

(defroutes app-routes
  (ANY "/" [] (example))
  (ANY "/v0.1/" [] (example))
  (GET "/v0.1/:name-of-collection" [] list-resource)
  (GET "/v0.1/:name-of-collection/sort/:field-to-sort-by" [] list-resource)
  (GET "/v0.1/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many" [] list-resource)
  (GET "/v0.1/:name-of-collection/match-field/:match-field/match-value/:match-value" [] list-resource)
  (GET "/v0.1/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many/match-field/:match-field/match-value/:match-value" [] list-resource)
  (GET "/v0.1/:name-of-collection/:document-id" [] entry-resource)
  (POST "/v0.1/:name-of-collection/:document-id" [] list-resource)
  (PUT "/v0.1/:name-of-collection" [] entry-resource)
  (PUT "/v0.1/:name-of-collection/:document-id" [] entry-resource)
  (DELETE "/v0.1/:name-of-collection/:document-id" [] entry-resource)
  (OPTIONS "/v0.1/:name-of-collection" request (preflight request))
  (OPTIONS "/v0.1/:name-of-collection/:document-id" request (preflight request))
  (route/resources "/")
  (route/not-found "Page not found. Check the http verb that you used (GET, POST, PUT, DELETE) and make sure you put a collection name in the URL, and possbly also a document ID."))

(def app
  (-> app-routes
      (wrap-cors-headers)
      (wrap-cookies)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-json-params)
      (wrap-content-type)
      (wrap-not-modified)))

(defn start [args]
  (println "App 'loupi' is starting.")
  (println "If no port is specified then we will default to port 34000.")
  (println "You can specify the port by starting it like this:")
  (println "java -jar target/loupi-0.1-standalone.jar 80")
  (let [port (if (nil? (first args))
               34000
               (Integer/parseInt  (first args)))]
    (run-jetty #'app {:port port :join? false :max-threads 5000})))
