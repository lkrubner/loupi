(ns loupi.removal-queue
  (:require
   [loupi.database :as database]
   [lamina.core :refer [channel enqueue read-channel]]))


(def ^:private removal-channel (channel))

(defn remove-item [ctx]
  {:pre [
         (map? (get-in ctx [:request :params]))
         (string? (get-in ctx [:request :params :_id]))
         ]}
  (enqueue removal-channel
           (fn [] (database/remove-this-item (get-in ctx [:request :params])))))

(defn- worker []
  (loop [closure-with-item-inside @(read-channel removal-channel)]
    (closure-with-item-inside)
    (recur @(read-channel removal-channel))))

(defn start-workers []
  "2014-07-01 - for now I'm hard-coding 6 worker threads to work on the persist queue, endlessly reading closures from the queue when something is put in the queue. Ideally, for CPU bound work, the number of threads should be the number of CPUs on the server, plus 1. Any document put on the removal queue is deleted from MongoDb."
  (dotimes [_ 6]
    (future (worker))))



