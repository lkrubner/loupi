(ns loupi.persist-queue
  (:require
   [loupi.logging :as log]
   [loupi.database :as database]
   [lamina.core :refer [channel enqueue read-channel]]))



(def ^:private persist-channel (channel))

(defn persist-item [ctx]
  {:pre [(map? ctx)]}
  (enqueue persist-channel
           (fn [] (try
                    (database/persist-this-item ctx)
                    (catch Exception e (log/print-error-info e))))))

(defn- worker []
  (loop [closure-with-item-inside @(read-channel persist-channel)]
    (closure-with-item-inside)
    (recur @(read-channel persist-channel))))

(defn start-workers []
  "2014-07-01 - TODO -- I/O bound workers should be managed by a proper thread pool but I'm too lazy to implement that right now so I'm just hardcoding 12 workers. Any document put on the persist queue is saved to MongoDb."
  (dotimes [_ 12]
    (println " starting up the persist queue workers ")
    (future (worker))))



