(ns loupi.controller
  (:require
   [loupi.cache-queue :as cq]
   [loupi.persist-queue :as pq]
   [loupi.removal-queue :as rq]
   [loupi.remember :as remember]
   [loupi.query :as query]
   [me.raynes.fs :as fs]
   [dire.core :refer [with-postcondition with-handler supervise]]
   [clojure.pprint :as pp]
   [clojure.string :as st]
   [clojure.java.io :as io]
   [clojure.data.json :as json])
  (:use
   [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]
   [clojure.java.io :only [as-file input-stream output-stream] :as io]))


(defn transform-keyword-into-string [some-keyword]
  {:pre [(= (type some-keyword) clojure.lang.Keyword)]
   :post [(string? %)]}
  (let [some-keyword (st/replace (str some-keyword) #":" "")
        some-keyword (st/replace (str some-keyword) #"-" " ")]
    some-keyword))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

(defn remove-final-comma [string-with-comma-at-end]
  (subs string-with-comma-at-end 0 (- (count string-with-comma-at-end) 1)))

(defn prepare-for-json [lazyseq-from-database]
  {:pre [(seq? lazyseq-from-database)]
   :post [(vector? %)]}
  "2014-07-02 - we get a lazyseq back from the database, that is stored in a Future. When we deref the Future, we need to turn the lazyseq into a string before we can give it to json/read-string"
  (reduce
   (fn [vector-of-strings next-document]
     ;; Javascript won't accept this:
     ;; :_id #<ObjectId 53a484cd3d698858278c3baa>
     ;; so we need to move the colon, remove the "_" and return the id value as a string
     (let [next-document (assoc next-document :id (str (:_id next-document)))
           next-document (dissoc next-document :_id)]
       (conj vector-of-strings next-document)))
   []
   lazyseq-from-database))


(defn parse-json [context key] false)

;; (defn parse-json [context key]
;;   (when (#{:put :post} (get-in context [:request :request-method]))
;;     (try
;;       (if-let [body (body-as-string context)]
;;         (let [data (json/read-str body)]
;;           [false {key data}])
;;         {:message "No body"})
;;       (catch Exception e
;;         (.printStackTrace e)
;;         {:message (format "IOException: %s" (.getMessage e))}))))

(defn paginate-results [ctx]
  {:pre [(map? ctx)]
   :post [(future? %)]}
  "2014-07-02 -- we want to enforce a strict contract on the function database/paginate-results, but we don't want our frontenders to have to strictly send us all the necessary parameters on every Ajax call, so we set useful defaults here"
  (let [ctx (if (:database-query-to-call ctx)
              ctx
              (assoc ctx :database-query-to-call "paginate-results"))
        ctx (if (:database-where-clause-map ctx)
              ctx
              (assoc ctx :database-where-clause-map {}))
        ctx (if (get-in ctx [:request :params :field-to-sort-by])
              ctx
              (assoc-in ctx [:request :params :field-to-sort-by] "_id"))
        ctx (if (get-in ctx [:request :params :offset-by-how-many])
              ctx
              (assoc-in ctx [:request :params :offset-by-how-many] "0"))

        ctx (if (get-in ctx [:request :params :return-how-many])
              ctx
              (assoc-in ctx [:request :params :return-how-many] "10"))]
    (query/fetch ctx)))

(defn find-this-item [ctx]
  (let [ctx (assoc ctx :database-query-to-call "find-this-item")
        future-data-return (query/fetch ctx)]
    (json/write-str (prepare-for-json @future-data-return))))

(defn list-resource-post! [ctx]
  (pq/persist-item ctx))

(defn set-database-query [ctx]
  (let [field-to-sort-by (get-in ctx [:request :params :field-to-sort-by])
        match-field (get-in ctx [:request :params :match-field])
        match-value (get-in ctx [:request :params :match-value])
        ctx (if (and (nil? field-to-sort-by) match-field)
              (assoc ctx :database-query-to-call "find-these-items")
              ctx)
        ctx (if (and (nil? field-to-sort-by) match-field)
              (assoc ctx :database-where-clause-map { (keyword match-field) match-value })
              ctx)]
    ctx))

(defn list-resource-handle-ok [ctx]
  (println "in list-resource-handle-ok")
  (pp/pprint ctx)
  (let [ctx (set-database-query ctx)        
        future-data-return (paginate-results ctx)]
    (json/write-str (prepare-for-json @future-data-return))))

(defn entry-resource-handle-ok [ctx]
  (let [ctx (assoc ctx :database-where-clause-map {})
        ctx (assoc ctx :database-query-to-call "find-this-item")
        future-data-return (paginate-results ctx)]
    (json/write-str (prepare-for-json @future-data-return))))

(defn entry-resource-put! [ctx]
  "2014-07-08 - PUT leads us to create a new document. If the :document-id matches the :_id of an existing document, that other document needs to be removed and wholly over-written by this new document."
  (pq/create-item ctx)
  {:status 0
   :messages [{:message "Attempting to create item"}]
   :data {:url (str "/v0.1/" (str (get-in ctx [:request :params :name-of-collection])))}
   })

(defn entry-resource-existed? [ctx]
  false)

(defn entry-resource-delete! [ctx]
  (rq/remove-item ctx))










