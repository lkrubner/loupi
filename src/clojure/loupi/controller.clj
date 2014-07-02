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
   [clojure.java.io :as io]))


(defn transform-keyword-into-string [some-keyword]
  {:pre [(= (type some-keyword) clojure.lang.Keyword)]
   :post [(string? %)]}
  (let [some-keyword (st/replace (str some-keyword) #":" "")
        some-keyword (st/replace (str some-keyword) #"-" " ")]
    some-keyword))

(defn list-resource[ctx]
  (pp/pprint ctx))

(defn list-resource-handle-ok [ctx]
  "{ data:\"this resource seems to render okay\" }")

(defn entry-resource-put! [ctx]
  "{ put-data : \"We have updated the data!\"}")

(defn entry-resource-existed? [ctx]
  false)

(defn entry-resource-delete! [ctx]
  (rq/remove-item ctx))
  





