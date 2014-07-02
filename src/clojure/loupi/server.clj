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
   [liberator.core :refer [resource defresource]]
   [clojure.data.json :as json])
  (:use
   [clojure.java.io :only [as-file input-stream output-stream] :as io]
   [ring.util.response]
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
   [ring.adapter.jetty :only [run-jetty]]))



(defn welcome-message []
  (assoc
      (ring.util.response/response "Hi. This is LaunchOpen. But you must be looking for a different URL?")
    :headers {"Content-Type" "text/plain"}))

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

(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn parse-json [context key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (json/read-str body)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

;; create and list entries
(defresource list-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::data)
  :post! (fn [ctx] (controller/list-resource ctx))
  :post-redirect? true
  :location #(build-entry-url (get % :request) (get % ::id))
  :handle-ok (fn [ctx]
               (controller/list-resource-handle-ok ctx)))

(defresource entry-resource []
  :allowed-methods [:get :put :delete]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? (fn [ctx]
             {::entry " if this shows up then something must exist"})
  :existed? (fn [ctx] (controller/entry-resource-existed? ctx))
  :available-media-types ["application/json"]
  :handle-ok ::entry
  :delete! (fn [ctx] (controller/entry-resource-delete! ctx))
  :malformed? #(parse-json % ::data)
  :can-put-to-missing? false
  :put! (fn [ctx] (controller/entry-resource-put! ctx))
  :new? (fn [ctx] (nil? nil)))

(defroutes app-routes
  (ANY "/" [] (welcome-message))
  (GET "/:name-of-collection" [] list-resource)
  (GET "/:name-of-collection/sort/:field-to-sort-by" [] list-resource)
  (GET "/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many" [] list-resource)
  (GET "/:name-of-collection/:document-id" [] list-resource)
  (POST "/:name-of-collection/:document-id" [] entry-resource)
  (PUT "/:name-of-collection/:document-id" [] entry-resource)
  (PUT "/:name-of-collection/" [] entry-resource)
  (DELETE "/:name-of-collection/:document-id" [] entry-resource)  
  (route/resources "/")
  (route/not-found "Page not found. Check the http verb that you used (GET, POST, PUT, DELETE) and make sure you put a collection name in the URL, and possbly also a document ID."))

(def app
  (-> app-routes
      (wrap-session {:cookie-name "loupi-session" :cookie-attrs {:max-age 90000000}})
      (wrap-cookies)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-nested-params)
      (wrap-params)
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
