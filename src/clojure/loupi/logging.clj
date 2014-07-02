(ns loupi.logging
  (:require
   [clojure.stacktrace :as stack]
   [me.raynes.fs :as fs]
   [clojure.pprint :as pp])
  (:use [taoensso.timbre :as timbre :only (trace debug info warn error fatal spy)]))



;; TODO implement this logging
(defn send-to-log-queue [message])

(defn print-error-info [e]
  "2012-12-10 - I originally put this in core.clj, but it is really just a utility function, so I will put it here."
  (prn "Exception in the main function: " e)
  (prn (apply str "The print-cause-trace with chained exceptions is:" (stack/print-cause-trace e)))
  (prn (apply str "The print-stack-trace standard clojure is:" (stack/print-stack-trace e))))
