(ns loupi.server
  (:import
   [java.net URL])
  (:require
   [loupi.controller :as controller]
   [loupi.faces :as faces]
   [loupi.perpetual :as perpetual]
   [loupi.startup :as startup]
   [loupi.dates :as dt]
   [compojure.core :refer :all]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [me.raynes.fs :as fs]
   [liberator.core :refer [resource defresource]]
   [ring.util.response :as rr]
   [taoensso.timbre :as timbre]
   [clojure.java.io :as io]
   [clojure.string :as st])
  (:use
   [ring.middleware.params]
   [ring.middleware.keyword-params]
   [ring.middleware.multipart-params]
   [ring.middleware.nested-params]
   [ring.middleware.file]
   [ring.middleware.resource]
   [ring.middleware.content-type]
   [ring.adapter.jetty :only [run-jetty]]
   [ring.middleware.json]))




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
          resp (rr/header resp "Content-Type" "application/json")
          resp (rr/header resp "Access-Control-Allow-Origin" (str origin))
          resp (rr/header resp "Access-Control-Allow-Methods" "PUT, DELETE, POST, GET, OPTIONS, XMODIFY")
          resp (rr/header resp "Access-Control-Max-Age" "4440")
          resp (rr/header resp "Access-Control-Allow-Credentials" "true")
          resp (rr/header resp "Access-Control-Allow-Headers" "Authorization, X-Requested-With, Content-Type, Origin, Accept")]
      (timbre/log :trace "In wrap-cors-headers, the origin is: " origin " and the request method is " (get-in request [:request-method])) 
      resp)))

(defn example []
  (assoc
      (ring.util.response/response "See documentation here: http://das.launchopen.com/api.html")
    :headers {"Content-Type" "text/plain"}))

;; a helper to create a absolute url for the entry with the given id
(defn build-entry-url [request]
  (println " in build-entry-url here is the id:")
  (println (get-in request [:params :document-id]))
  (if (get-in request [:params :document-id])
    (URL. (format "%s://%s:%s%s/%s"
                  (name (:scheme request))
                  (:server-name request)
                  (:server-port request)
                  (:uri request)
                  (get-in request [:params :document-id])))
    (URL. (format "%s://%s:%s%s"
                  (name (:scheme request))
                  (:server-name request)
                  (:server-port request)
                  (:uri request)))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types-we-allow]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (let [vector-of-headers-sent-by-client (st/split (get-in ctx [:request :headers "content-type"]) #";")]
      (or
       (not (every? nil? (map #(some #{%} content-types-we-allow) vector-of-headers-sent-by-client)))
       [false {:message "Unsupported Content-Type"}]))
    true))

(defresource list-resource
  :allowed-methods [:get :put]
  :available-media-types ["application/json"]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(controller/request-malformed? % ::data)
  :handle-options (fn [ctx] "CORS enabled")
  :respond-with-entity? true
  :multiple-representations? false
  :location #(build-entry-url (get % :request))
  :new? (fn [ctx] false)
  :handle-ok (fn [ctx]
               (timbre/log :trace " now we are in list-resource :handle-ok")
               (controller/list-resource-handle-ok ctx)))

(defresource entry-resource 
  :allowed-methods [:get :post :put :delete]
  :known-content-type? #(check-content-type % ["application/json"])
  :available-media-types ["application/json"]
  :exists? (fn [ctx] true)
  :malformed? #(controller/request-malformed? % ::data)
  :can-put-to-missing? true
  :multiple-representations? false
  :can-post-to-missing? true
  :respond-with-entity? true
  :new? (fn [ctx]
          (if (= (get-in ctx [:request :request-method]) :post) false true))
  :post-redirect? false
  :existed? (fn [ctx]
              (timbre/log :trace " now we are in entry-resource :existed? ")
              (controller/entry-resource-existed? ctx))
  :post! (fn [ctx]
           (timbre/log :trace " now we are in entry-resource :post! ")
           (controller/entry-resource-post! ctx))
  :handle-ok (fn [ctx]
               (timbre/log :trace " now we are in entry-resource :handle-ok")
               (controller/entry-resource-handle-ok ctx))
  :delete! (fn [ctx]
             (timbre/log :trace " now we are in entry-resource :delete! ")
             { :deleted-document (controller/entry-resource-delete! ctx) })
  :put! (fn [ctx]
          (timbre/log :trace " now we are in entry-resource :put! ")
          {:new-document (controller/entry-resource-put! ctx)})
  :handle-options (fn [ctx]
                    (timbre/log :trace " now we are in entry-resource :handle-options ")
                    "CORS enabled")
  :handle-created (fn [ctx]
                    (timbre/log :trace " now we are in entry-resource :handle-created ")
                    (:new-document ctx)))

(defresource collection-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :put]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(faces/request-malformed? % ::data)
  :location #(build-entry-url (get % :request))
  :respond-with-entity? true
  :multiple-representations? false
  :new? (fn [ctx] false)
  :put! (fn [ctx]
          (timbre/log :trace " now we are in collection-resource :put! ")
          (faces/document-resource-post! ctx))
  :handle-options (fn [ctx]
                    (timbre/log :trace " now we are in collection-resource :handle-options")
                    "CORS enabled")
  :handle-ok (fn [ctx]
               (timbre/log :trace " now we are in collection-resource :handle-ok")
               (faces/collection-resource-handle-ok ctx)))

(defresource document-resource 
  :allowed-methods [:get :post :put :delete]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? (fn [ctx] true)
  :existed? (fn [ctx] (faces/document-resource-existed? ctx))
  :available-media-types ["application/json"]
  :respond-with-entity? true
  :malformed? #(faces/request-malformed? % ::data)
  :can-put-to-missing? true
  :multiple-representations? false
  :can-post-to-missing? true
  :new? (fn [ctx] (if (= (get-in ctx [:request :method]) :post) false true))
  :handle-options (fn [ctx]
                    (timbre/log :trace " now we are in document-resource :handle-options")
                    "CORS enabled")
  :handle-ok (fn [ctx]
               (timbre/log :trace " now we are in document-resource :handle-ok")
               (faces/document-resource-handle-ok ctx))
  :put! (fn [ctx]
          (timbre/log :trace " now we are in document-resource put!")
          (faces/document-resource-put! ctx))
  :post! (fn [ctx]
           (timbre/log :trace " now we are in document-resource :post!")
           (faces/document-resource-post! ctx))
  :delete! (fn [ctx]
             (timbre/log :trace " now we are in document-resource :delete!")
             (faces/document-resource-delete! ctx)))



(defroutes app-routes
  (ANY "/" [] (example))
  (GET "/v0.1/:name-of-collection" [] list-resource)
  (GET "/v0.1/:name-of-collection/sort/:field-to-sort-by" [] list-resource)
  (GET "/v0.1/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many" [] list-resource)
  (GET "/v0.1/:name-of-collection/match-field/:match-field/match-value/:match-value" [] list-resource)
  (GET "/v0.1/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many/match-field/:match-field/match-value/:match-value" [] list-resource)
  (GET "/v0.1/:name-of-collection/:document-id" [] entry-resource)
  (POST "/v0.1/:name-of-collection/:document-id" [] entry-resource)
  (PUT "/v0.1/:name-of-collection" [] entry-resource)
  (PUT "/v0.1/:name-of-collection/:document-id" [] entry-resource)
  (DELETE "/v0.1/:name-of-collection/:document-id" [] entry-resource)
  (OPTIONS "/v0.1/:name-of-collection" request (preflight request))
  (OPTIONS "/v0.1/:name-of-collection/:document-id" request (preflight request))
  (OPTIONS "/v0.1/:name-of-collection/sort/:field-to-sort-by"  request (preflight request))
  (OPTIONS "/v0.1/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many"  request (preflight request))
  (OPTIONS "/v0.1/:name-of-collection/match-field/:match-field/match-value/:match-value"  request (preflight request))
  (OPTIONS "/v0.1/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many/match-field/:match-field/match-value/:match-value"  request (preflight request))  
  (ANY "/v0.2/:token/:name-of-collection" [] collection-resource)
  (ANY "/v0.2/:token/:name-of-collection/:document-id" [] document-resource)
  (ANY "/v0.2/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many" [] collection-resource)
  (ANY "/v0.2/:name-of-collection/match-field/:match-field/match-value/:match-value" [] collection-resource)
  (ANY "/v0.2/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many/match-field/:match-field/match-value/:match-value" [] collection-resource)  
  (route/resources "/")
  (route/not-found "Page not found. Check the http verb that you used (GET, POST, PUT, DELETE) and make sure you put a collection name in the URL, and possbly also a document ID."))

(def app
  (-> app-routes
      (wrap-cors-headers)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-json-params)
      (wrap-content-type)))

(defmacro start-perpetual-events []
  `(do ~@(for [i (map first (ns-publics 'loupi.perpetual))]
           `(~(symbol (str "perpetual/" i))))))

(defmacro start-startup-events []
  `(do ~@(for [i (map first (ns-publics 'loupi.startup))]
           `(~(symbol (str "startup/" i))))))

(defn start [args]
  (try
    (println "App 'loupi' is starting.")
    (println "If no port is specified then we will default to port 34000.")
    (println "You can specify the port by starting it like this:")
    (println "java -jar target/loupi-0.1-standalone.jar 80")
    (start-startup-events)
    (start-perpetual-events)
    (let [port (if (nil? (first args))
                 34000
                 (Integer/parseInt  (first args)))]
      (timbre/log :trace (str "Starting the app at: " (dt/current-time-as-string)))
      (def server (run-jetty #'app {:port port :join? false :max-threads 5000})))
    (catch Exception e (println e))))

(defn stop []
  (.stop server))
