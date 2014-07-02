(ns loupi.startup
  (:require
   [loupi.cache-queue :as cq]
   [loupi.persist-queue :as pq]
   [loupi.removal-queue :as rq]
   [loupi.database :as database]
   [me.raynes.fs :as fs]))


(defn connect-to-database []
  (try
    (database/establish-database-connection)
    (catch Exception e (println e))))

(defn start-cache-queue []
  (cq/start-workers))

(defn start-persist-queue []
  (pq/start-workers))

(defn start-removal-queue []
  (rq/start-workers))

(defn check-that-log-file-exists []
  (if-not (fs/exists? "/var/log/loupi.log")
    (throw (Throwable. "In startup/check-that-log-file-exists, we could not find the log file /var/log/loupi.log"))))
