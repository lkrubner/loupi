(ns loupi.faces
  (:require
   [loupi.persistence-queue :as pq]
   [loupi.tokens :as tk]
   [loupi.query :as query]
   [cheshire.core :as cheshire]
   [me.raynes.fs :as fs]
   [dire.core :as dire]
   [clojure.string :as st]
   [clojure.walk :as walk]
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [taoensso.timbre :as timbre]
   [clojure.java.io :as io]))


(defn walk-deep-structure [next-item function-to-transform-values]
  (walk/postwalk
   (fn [%]
     (if (and (vector? %) (= (count %) 2) (keyword? (first %)))
       [(function-to-transform-values %) (second %)]
       %))
   next-item))

(defn prepare-for-json [lazyseq-from-database]
  {:pre [(seq? lazyseq-from-database)]
   :post [(vector? %)]}
  (reduce
   (fn [vector-of-strings next-document]
     ;; need to avoid: 
     ;; java.lang.Exception: Don't know how to write JSON of class org.bson.types.ObjectId
     (timbre/log :trace (str " in prepare-for-json : " next-document))
     (let [next-document (assoc next-document "id" (str (get next-document "_id")))
           next-document (assoc next-document "_id" (str (get next-document "_id")))]
       (conj vector-of-strings next-document)))
   []
   lazyseq-from-database))

(defn request-malformed? [context key] 
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (get-in context [:request :json-params])]
        (if (or nil (= body {}))
          {:status 1 :messages [{:type "error" :message "Error: the body of your request was empty"}]}
          [false {key body}]))
      (catch Exception e
        (.printStackTrace e)
        {:message (str "Error: please re-check the document that you sent. " (format "IOException: %s" (.getMessage e)))}))))

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
        future-data-return (query/fetch ctx)
        results (walk-deep-structure @future-data-return (fn [%] (st/replace (name (first %)) "*" "$")))]
    (cheshire/generate-string (prepare-for-json results) {:pretty true})))

(defn collection-resource-post! [ctx]
  (pq/persist-this-item ctx))

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

(defn collection-resource-handle-ok [ctx]
  (timbre/log :trace (str "in list-resource-handle-ok" ctx))
  (let [ctx (set-database-query ctx)        
        future-data-return (paginate-results ctx)
        results (walk-deep-structure @future-data-return (fn [%] (st/replace (name (first %)) "*" "$")))]
    (cheshire/generate-string (prepare-for-json results) {:pretty true})))

(defn document-resource-handle-ok [ctx]
  (let [ctx (assoc ctx :database-where-clause-map {})
        ctx (assoc ctx :database-query-to-call "find-this-item")
        future-data-return (paginate-results ctx)
        results (walk-deep-structure @future-data-return (fn [%] (st/replace (name (first %)) "*" "$")))]
    (println " document-resource-handle-ok has been called")
    (cheshire/generate-string (first (prepare-for-json results)) {:pretty true})))

(defn document-resource-put! [ctx]
  "2014-07-08 - PUT leads us to create a new document. If the :document-id matches the :_id of an existing document, that other document needs to be removed and wholly over-written by this new document."
  (let [context-wrapper-for-database-call {
                                           :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                           :where-clause-map {}
                                           :document (get-in ctx [:request :json-params])
                                           :query-name :create-this-item          
                                           }]
  (pq/persist-this-item context-wrapper-for-database-call)
  {:status 0
   :messages [{:message "Attempting to create item"}]
   :data {:url (str "/v0.1/" (str (get-in ctx [:request :params :name-of-collection])))}
   }))

(defn document-resource-post! [ctx]
  "2014-07-08 - PUT leads us to create a new document. If the :document-id matches the :_id of an existing document, that other document needs to be removed and wholly over-written by this new document."
  (println " document-resource-put! has been called ")
  (pq/persist-this-item ctx)
  {:status 0
   :messages [{:message "Attempting to create item"}]
   :data {:url (str "/v0.1/" (str (get-in ctx [:request :params :name-of-collection])))}
   })


(defn document-resource-existed? [ctx]
  false)

(defn document-resource-delete! [ctx]
  (let [context-wrapper-for-database-call {
                                           :name-of-collection (get-in ctx [:request :params :name-of-collection])
                                           :where-clause-map { :_id (get-in ctx [:request :json-params :document-id])}
                                           :document (get-in ctx [:request :json-params])
                                           :query-name :remove-this-item      
                                           }]
  (pq/persist-this-item context-wrapper-for-database-call)))









