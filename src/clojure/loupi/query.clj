(ns loupi.query
  (:require
   [loupi.cache-queue :as cq]
   [loupi.remember :as remember]
   [loupi.database :as database]
   [clojure.pprint :as pp]))


;; 2014-07-01 - this namespace checks the in-memory cache, which we
;; or may not need. It checks the cache first, and if nothing is in the cache
;; then it creates a Future that sends a call to Monger. The only thing we
;; ever store in the cache are Futures. The client code always needs to deref.


(defn- execute-a-database-query [ctx]
  (future
    (let [function-name-as-symbol (symbol (:database-query-to-call ctx))
          function-name-as-function (ns-resolve 'loupi.database function-name-as-symbol)]
      (function-name-as-function ctx))))

(defn- store [ctx]
  (let [fetched-data (execute-a-database-query ctx)]
    (cq/cache-item [(get-in ctx [:request :uri]) (:database-query-to-call ctx)] fetched-data)
    fetched-data))

(defn fetch [ctx]
  {:pre [
         (map? ctx)
         (do (pp/pprint ctx) true)
         (string? (:database-query-to-call ctx))
         (map? (:database-where-clause-map ctx))
        ]
   :post [(future? %)]}
  "2014-07-01 - first we check the cache. If we get back nil, then we call 'store', which should put a Future in the cache, and then we check the cache again."
  (let [cached-data (remember/get-from-memory-cache [(get-in ctx [:request :uri]) (:database-query-to-call ctx)])]
    (if (nil? cached-data)
      (store ctx)
      cached-data)))









