(ns loupi.database
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]
           [org.joda.time.DateTimeZone])
  (:require
   [loupi.dates :as tyme]
   [me.raynes.fs :as fs]
   [monger.core :as mg]
   [monger.collection :as mc]
   [monger.conversion :as convert]
   [monger.operators :as operators]
   [monger.joda-time]
   [clojure.pprint :as pp])
  (:refer-clojure :exclude [sort find])
  (:use
   [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]
   [clojure.java.io :only [as-file input-stream output-stream] :as io]
   [monger.query]))





;; TODO -- upgrading to Monger 2.0, which has new API for auth. I should look it up someday.
(defn establish-database-connection []
  (let [credentials (read-string (slurp "/etc/loupi_config")) 
;;        uri (str "mongodb://" (:username credentials) ":" (:password credentials) "@" (:host credentials) "/" (:db credentials))
         uri (str "mongodb://" (:host credentials) "/" (:db credentials))
        { :keys [conn db]} (mg/connect-via-uri uri)]
    (pp/pprint conn)
    (println uri)
    (println " our database is: ")
    (pp/pprint db)
    (def database-object db)))


;; (defn establish-database-connection []
;;   (let [credentials (read-string (slurp (clojure.java.io/resource "config/credentials.edn")))
;;         conn (mg/connect)]
;;     (pp/pprint conn)
;;   (def db (mg/get-db conn (:db credentials)))))

(defn remove-this-item [ctx]
  {:pre [
         (map? ctx)
         (string? (get-in ctx [:request :params :name-of-collection]))
         (or 
          (string? (get-in ctx [:request :params :document-id]))
          (string? (get-in ctx [:request :params :_id])))
         ]}
  (let [document-id (or 
                     (get-in ctx [:request :params :document-id])
                     (get-in ctx [:request :params :_id]))]
    (println " in remove-this-item")
    (println "the document id:")
    (pp/pprint document-id)
  (mc/remove database-object  (get-in ctx [:request :params :name-of-collection]) { :_id (ObjectId. document-id) })))

(defn create-this-item [ctx]
  {:pre [
         (map? ctx)
         (map? (get-in ctx [:request]))
         (map? (get-in ctx [:request :json-params]))
         (map? (get-in ctx [:request :params]))
         (string? (get-in ctx [:request :params :name-of-collection])) 
         (if (get-in ctx [:request :params :document-id])
           (string? (get-in ctx [:request :params :document-id]))
           true)
         ]}
  "2014-07-08 - if this app is called via a PUT then a new item should be created. If a document id is present, we want to overwrite the old document, so we delete it and create a new item."
  (let [document-id (get-in ctx [:request :params :document-id])
        item (get-in ctx [:request :json-params])
        item (assoc item :created-at (tyme/current-time-as-datetime))
        item (assoc item :updated-at (tyme/current-time-as-datetime))
        name-of-collection (get-in ctx [:request :params :name-of-collection])]
    (if document-id
      (do 
        (remove-this-item ctx)
        (mc/insert database-object name-of-collection (assoc item :_id (ObjectId. document-id))))
      (mc/insert database-object name-of-collection (assoc item :_id (ObjectId.))))))

(defn find-this-specific-item [collection document-id]
  {:pre [
         (string? collection)
         (string? document-id)
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (mc/find-maps database-object collection { :_id (ObjectId. document-id) }))


(defn change-string-keys-to-keyword-keys [json-param-map]  
  (loop [sks (keys json-param-map) fullmap { (keyword (first (keys json-param-map))) (get json-param-map (first (keys json-param-map))) }]  
    (if (next sks) 
      (recur (rest sks)   
             (assoc fullmap  (keyword (first (rest sks))) (get json-param-map (first (rest sks))))) 
      fullmap)))

(defn persist-this-item [ctx]
  {:pre [
         (map? ctx)
         (map? (get-in ctx [:request]))
         (map? (get-in ctx [:request :params]))
         (map? (get-in ctx [:request :json-params]))
         (string? (get-in ctx [:request :params :name-of-collection])) 
         (if (get-in ctx [:request :params :document-id])
           (string? (get-in ctx [:request :params :document-id]))
           true)
         (if (get-in ctx [:request :params :created-at])
           (= (type (get-in ctx [:request :params :created-at])) org.joda.time.DateTime)
           true)]}
  "2014-07-08 - this function is called when the app receives a POST request. If there is a document-id, the new document should be merged with the old document. If the client code calling this app wants to over-write an existing document, they call with PUT instead of POST."
  (let [document-id (get-in ctx [:request :params :document-id])
        item (get-in ctx [:request :json-params])
        item (if (nil? (:created-at item))
               (assoc item :created-at (tyme/current-time-as-datetime))
               item)
        item (assoc item :updated-at (tyme/current-time-as-datetime))
        name-of-collection (get-in ctx [:request :params :name-of-collection])
        item (change-string-keys-to-keyword-keys item)
        item (if document-id
               (merge
                (first (find-this-specific-item name-of-collection document-id))
                item)
               item)
        item (if document-id
               (assoc item :_id (ObjectId. document-id))
               item)]
    (println " in database/persist-this-item, here is the merged item to be updated:")
    (pp/pprint item)
    (if document-id
      (mc/update database-object name-of-collection { :_id (ObjectId. document-id) } item)
      (mc/insert database-object name-of-collection (assoc item :_id (ObjectId.))))))

(defn find-this-item [ctx]
  {:pre [
         (map? ctx)
         (string? (get-in ctx [:request :params :name-of-collection]))
         (string? (get-in ctx [:request :params :document-id]))
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (mc/find-maps database-object (get-in ctx [:request :params :name-of-collection]) { :_id (ObjectId. (get-in ctx [:request :params :document-id])) }))

(defn find-these-items [ctx]
  {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (if (:database-fields-to-return-vector ctx)
           (vector? (:database-fields-to-return-vector ctx))
           true)
         (string? (get-in ctx [:request :params :name-of-collection]))
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  (println " in database/find-these-items")
  (if (:database-fields-to-return-vector ctx)
    (mc/find-maps database-object  (get-in ctx [:request :params :name-of-collection]) (:database-where-clause-map ctx)  (:database-fields-to-return-vector ctx))
    (mc/find-maps database-object  (get-in ctx [:request :params :name-of-collection]) (:database-where-clause-map ctx))))

(defn get-count [ctx]
  {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (string? (get-in ctx [:request :params :name-of-collection])) 
         ]
   :post [
          (= (type %) java.lang.Long)
          (or (pos? %) (zero? %))
          ]}
  (mc/count database-object  (get-in ctx [:request :params :name-of-collection]) (:database-where-clause-map ctx)))

(defn paginate-results [ctx]
  {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (string? (get-in ctx [:request :params :name-of-collection])) 
         (string? (get-in ctx [:request :params :field-to-sort-by])) 
         (string? (get-in ctx [:request :params :offset-by-how-many])) 
         (string? (get-in ctx [:request :params :return-how-many])) 
         (number? (Integer/parseInt (get-in ctx [:request :params :offset-by-how-many])))
         (or
          (pos? (Integer/parseInt (get-in ctx [:request :params :offset-by-how-many])))
          (zero? (Integer/parseInt (get-in ctx [:request :params :offset-by-how-many]))))
         (number? (Integer/parseInt (get-in ctx [:request :params :return-how-many]))) 
         (pos? (Integer/parseInt (get-in ctx [:request :params :return-how-many]))) 
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  "2014-07-01 - the route that calls this function was defined as:
   (GET \"/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many\" [] list-resource)"
  (let [name-of-collection (get-in ctx [:request :params :name-of-collection])
        field-to-sort-by (keyword (get-in ctx [:request :params :field-to-sort-by]))
        offset-by-how-many (Integer/parseInt (get-in ctx [:request :params :offset-by-how-many]))
        return-how-many (Integer/parseInt (get-in ctx [:request :params :return-how-many]))
        return-how-many (if-not (pos? return-how-many)
                          10
                          return-how-many)
        match-field (get-in ctx [:request :params :match-field])
        match-value (get-in ctx [:request :params :match-value])
        ctx (if match-field 
              (assoc ctx :database-where-clause-map { (keyword match-field) match-value})
              ctx)]
    (println " in database/paginate")
    (println " name-of-collection: " (str name-of-collection))
    (println " field-to-sort-by: " (str field-to-sort-by))
    (println " offset-by-how-many: " (str offset-by-how-many))
    (println " return-how-many: " (str return-how-many))
    (println " database-where-clause-map: ")
    (pp/pprint (:database-where-clause-map ctx))

    (with-collection database-object name-of-collection
      (find (:database-where-clause-map ctx))
      ;;    (fields [:item-name :item-type :user-item-name :created-at])
      (sort (array-map field-to-sort-by 1 ))
      (limit return-how-many)
      (skip offset-by-how-many))))
  








