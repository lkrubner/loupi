(ns loupi.persist-queue
  (:require
   [loupi.logging :as log]
   [loupi.database :as database]
   [lamina.core :refer [channel enqueue read-channel]]))



(def ^:private persist-channel (channel))

(defn persist-item [item]
  {:pre [(map? item)]}
  (enqueue persist-channel
           (fn [] (try
                    (database/persist-this-item item)
                    (catch Exception e (log/print-error-info e))))))

(defn- worker []
  (loop [closure-with-item-inside @(read-channel persist-channel)]
    (closure-with-item-inside)
    (recur @(read-channel persist-channel))))

(defn start-workers []
  "2014-07-01 - for now I'm hard-coding 6 worker threads to work on the persist queue, endlessly reading closures from the queue when something is put in the queue. Ideally, for CPU bound work, the number of threads should be the number of CPUs on the server, plus 1. Any document put on the persist queue is saved to MongoDb."
  (dotimes [_ 6]
    (future (worker))))



