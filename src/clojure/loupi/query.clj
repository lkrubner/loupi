(ns loupi.query
  (:require
   [loupi.persistence :as ps]
   [taoensso.timbre :as timbre]))



(defn fetch [context-wrapper-for-database-call]
  {:pre [
         (map? context-wrapper-for-database-call)
         (string? (:name-of-collection context-wrapper-for-database-call))
         (map? (:where-clause-map context-wrapper-for-database-call))
         (map? (:document context-wrapper-for-database-call))
         (keyword? (:query-name context-wrapper-for-database-call))
        ]
   :post [(future? %)]}
  "2014-07-01 - first we check the cache. If we get back nil, then we call 'store', which should put a Future in the cache, and then we check the cache again."
  (future (ps/make-consistent context-wrapper-for-database-call)))
  








