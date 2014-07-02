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
   [monger.joda-time])
  (:refer-clojure :exclude [sort find])
  (:use
   [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]
   [clojure.java.io :only [as-file input-stream output-stream] :as io]
   [monger.query]))





;; TODO -- upgrading to Monger 2.0, which has new API for auth. I should look it up someday.
;; (defn establish-database-connection []
;;   (let [credentials (read-string (slurp (clojure.java.io/resource "config/credentials.edn")))                    
;;         uri (str "mongodb://" (:username credentials) ":" (:password credentials) "@" (:host credentials) "/" (:db credentials))
;;         { :keys [conn database]} (mg/connect-via-uri uri)]
;;     (println uri)
;;     (def db database)))


(defn establish-database-connection []
  (let [credentials (read-string (slurp (clojure.java.io/resource "config/credentials.edn")))] 
  (def db (mg/get-db (mg/connect) (:db credentials)))))

(defn remove-this-item [ctx]
  {:pre [
         (map? ctx)
         (string? (get-in ctx [:request :name-of-collection])) 
         (string? (get-in ctx [:request :params :_id])) 
         ]}
  (mc/remove db (get-in ctx [:request :name-of-collection]) { :_id (ObjectId. (get-in ctx [:request :document-id])) }))

(defn persist-this-item [ctx]
  {:pre [
         (map? ctx)
         (map? (get-in ctx [:request]))
         (map? (get-in ctx [:request :params]))
         (string? (get-in ctx [:request :name-of-collection])) 
         (if (get-in ctx [:request :params :_id])
           (string? (get-in ctx [:request :params :_id]))
           true)
         (if (get-in ctx [:request :params :created-at])
           (= (type (get-in ctx [:request :params :created-at])) org.joda.time.DateTime)
           true)]}
  (let [item (get-in ctx [:request :params])
        item (if (nil? (:created-at item))
               (assoc item :created-at (tyme/current-time-as-datetime))
               item)
        item (assoc item :updated-at (tyme/current-time-as-datetime))
        name-of-collection (get-in ctx [:request :name-of-collection])]
    (if (:_id item)
      (mc/update db name-of-collection { :_id (ObjectId. (:_id item)) } item)
      (mc/insert db name-of-collection (assoc item :_id (ObjectId.))))))

(defn find-these-items [ctx]
  {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (if (:database-fields-to-return-vector ctx)
           (vector? (:database-fields-to-return-vector ctx))
           true)
         (string? (get-in ctx [:request :name-of-collection]))
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  "2013-12-11 - if there is something in args, we assume it is the vector of fields which limits which fields are returned."
  (if (:database-fields-to-return-vector ctx)
    (mc/find-maps db (get-in ctx [:request :name-of-collection]) (:database-where-clause-map ctx)  (:database-fields-to-return-vector ctx))
    (mc/find-maps db (get-in ctx [:request :name-of-collection]) (:database-where-clause-map ctx))))

(defn get-count [ctx]
  {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (string? (get-in ctx [:request :name-of-collection])) 
         ]
   :post [
          (= (type %) java.lang.Long)
          (or (pos? %) (zero? %))
          ]}
  (mc/count db (get-in ctx [:request :name-of-collection]) (:database-where-clause-map ctx)))

(defn paginate-results [ctx]
  {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (string? (get-in ctx [:request :name-of-collection])) 
         (string? (get-in ctx [:request :field-to-sort-by])) 
         (string? (get-in ctx [:request :offset-by-how-many])) 
         (string? (get-in ctx [:request :return-how-many])) 
         (number? (Integer/parseInt (get-in ctx [:request :offset-by-how-many])))
         (number? (Integer/parseInt (get-in ctx [:request :return-how-many]))) 
         ]
   :post [(= (type %) clojure.lang.LazySeq)]}
  "2014-07-01 - the routet that calls this function was defined as:
   (GET \"/:name-of-collection/sort/:field-to-sort-by/:offset-by-how-many/:return-how-many\" [] list-resource)"
  (with-collection db (get-in ctx [:request :name-of-collection])
    (find (:database-where-clause-map ctx))
;    (fields [:item-name :item-type :user-item-name :created-at])
    (sort (array-map  (get-in ctx [:request :field-to-sort-by]) 1))
    (limit (Integer/parseInt (get-in ctx [:request :return-how-many]))
    (skip (Integer/parseInt (get-in ctx [:request :offset-by-how-many]))))))









