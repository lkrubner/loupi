(ns loupi.startup
  (:require
   [loupi.persistence-queue :as pq]
   [loupi.persistence :as ps]
   [slingshot.slingshot :as ss]
   [me.raynes.fs :as fs]
   [taoensso.timbre :as timbre]))


(defn connect-to-persistence []
  (println " in startup/connect-to-persistence")
  (try
    (ps/establish-database-connection)
    (catch Exception e (println e))))

(defn start-persist-queue []
  (pq/start-workers))

(defn check-that-log-file-exists[]
  (if-not (fs/exists? "/var/log/loupi.log")
    (ss/throw+ {:type :loupi.supervisor/no-log-file :message "In startup/check-that-log-file-exists, we could not find the log file /var/log/lo-login-service.log" })))

(defn check-that-config-file-exists[]
  (if-not (fs/exists? "/etc/launchopen_config")
    (ss/throw+ {:type  :loupi.supervisor/no-config-file :message "In startup/check-that-config-file-exists, we could not find the config file /etc/launchopen_config" })))

(defn set-timbre-level[]
  (println " setup for timbre ")
  (timbre/set-level! :trace)
  (timbre/set-config! [:appenders :spit :enabled?] true)
  (timbre/set-config! [:shared-appender-config :spit-filename] "/var/log/loupi.log"))
