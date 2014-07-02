(ns loupi.removal-queue
  (:require
   [loupi.database :as database]
   [lamina.core :refer [channel enqueue read-channel]]))


(def ^:private removal-channel (channel))

(defn remove-item [ctx]
  {:pre [
         (map? (get-in ctx [:request :params]))
         (string? (get-in ctx [:request :params :document-id]))
         ]}
  (enqueue removal-channel
           (fn []
             (println " the closure in the removal queue is now being called ")
             (database/remove-this-item ctx))))

(defn- worker []
  (loop [closure-with-item-inside @(read-channel removal-channel)]
    (closure-with-item-inside)
    (recur @(read-channel removal-channel))))

(defn start-workers []
  "2014-07-01 - TODO -- I/O bound workers should be managed by a proper thread pool but I'm too lazy to implement that right now so I'm just hardcoding 12 workers. Any document put on the removal queue is deleted from MongoDb."
  (dotimes [_ 12]
    (println " starting up the removal workers ")
    (future (worker))))



