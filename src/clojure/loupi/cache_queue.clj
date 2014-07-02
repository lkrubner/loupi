(ns loupi.cache-queue
  (:require
   [loupi.remember :as remember]
   [loupi.dates :as tyme]
   [lamina.core :refer [channel enqueue read-channel]]
   [dire.core :refer [with-postcondition with-handler supervise]]))



(def ^:private cache-channel (channel))

(defn cache-item [vector-of-locators item]
  {:pre [
         (vector? vector-of-locators)
         (future? item)
         ]}
  (enqueue cache-channel (fn []
                           (remember/add-to-memory-cache vector-of-locators item)
                           (. java.lang.Thread sleep 1000)
                           (remember/delete-from-memory-cache vector-of-locators))))

(defn- worker []
  (loop [closure-with-vector-of-locators @(read-channel cache-channel)]
    (closure-with-vector-of-locators)
    (recur @(read-channel cache-channel))))

(defn start-workers []
  "2014-07-01 - for now I'm hard-coding 6 worker threads to work on the cache queue, endlessly reading closures from the queue when something is put in the queue. Ideally, for CPU bound work, the number of threads should be the number of CPUs on the server, plus 1. I don't know if it is appropriate to cache data in memory. On another project I saved data for an hour. For now I am saving data for 5 seconds."
  (dotimes [_ 6]
    (future (worker))))




