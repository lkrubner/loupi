(ns loupi.supervisor
  (:require [loupi.perpetual :as perpetual]
            [loupi.startup :as startup]
            [loupi.server :as server]
            [loupi.logging :as log]
            [clojure.string :as st]
            [clojure.stacktrace :as stack]
            [dire.core :refer [with-handler supervise]])
  (:use
   [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]))


(defmacro start-perpetual-events []
  `(do ~@(for [i (map first (ns-publics 'loupi.perpetual))]
           `(~(symbol (str "perpetual/" i))))))

(defmacro start-startup-events []
  `(do ~@(for [i (map first (ns-publics 'loupi.startup))]
           `(~(symbol (str "startup/" i))))))

(defn start [args]
  (try 
    (start-startup-events)
    (start-perpetual-events)
    (server/start args)
    (catch Exception e (log/print-error-info e))))





