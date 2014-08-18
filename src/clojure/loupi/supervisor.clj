(ns loupi.supervisor
  (:require
   [loupi.perpetual :as perpetual]
   [loupi.startup :as startup]
   [loupi.server :as server]
   [loupi.controller :as ct]
   [loupi.dates :as dt]
   [loupi.query :as qy]
   [clojure.string :as st]
   [clj-stacktrace.core :as stack]
   [dire.core :as di]
   [taoensso.timbre :as tb]
   [loupi.persistence-queue :as pq]))


;; 2014-08-07 - I dislike cluttering my code (the part that's visible to human eyes)
;; with try/catch blocks, but I need all functions wrapped in try/catch blocks, so we
;; use Dire, which uses macros to wrap functions in try/catch blocks at compile time.
;; Thus, 'supervisor' is the namespace for catching anything thrown in other namespaces.


(derive ::persistence-channel-closed ::channel-problem)
(derive ::persistence-channel-drained ::channel-problem)
(derive ::unable-to-connect-to-database ::database-problem)  
(derive ::datbase-problem ::problem)
(derive ::no-config-file ::startup-problem)
(derive ::no-log-file ::startup-problem)
(derive ::startup-problem ::problem)
(derive java.lang.Exception ::problem)
(derive ::database-logging ::logging)


(di/with-handler! #'server/start
  java.lang.Exception
  (fn [e & args]
    (println "We are in the server/start handler")
    (tb/log :trace  (str " server/start: The time of the error: " (dt/current-time-as-string) " " (stack/parse-exception e) " " (str e) " " (str args)))))

(di/with-handler! #'server/start
  Object
  (fn [e & args]
    (println "We are in the server/start handler")
    (tb/log :trace  (str " server/start: The time of the error: " (dt/current-time-as-string)  " " (str e) " " (str args)))))

(di/with-handler! #'loupi.persistence-queue/persist-this-item
  java.lang.Exception
  (fn [e & args]
    (tb/log :trace  (str " pq/persist-this-item The time of the error: " (dt/current-time-as-string) " " (stack/parse-exception e) " " (str e) " " (str args)))
    ))

(di/with-handler! #'loupi.persistence-queue/worker
  Object
  (fn [e & args]
    (tb/log :trace  (str " di/with-handler! #'loupi.persistence-queue/worker: The time of the error: " (dt/current-time-as-string)" " (str e) " " (str args)))))

(di/with-handler! #'loupi.persistence-queue/start-workers
  Object
  (fn [e & args]
    (println "We are in the start-workers handler")
    (tb/log :trace  (str " start-worker: The time of the error: " (dt/current-time-as-string) " " (stack/parse-exception e) " " (str e) " " (str args)))))

(di/with-eager-pre-hook! #'loupi.persistence-queue/persist-this-item
  "An optional docstring."
  (fn [context-wrapper-for-database-call]
    (tb/log :trace "with-eager-pre-hook! #'loupi.persistence-queue/persist-this-item" context-wrapper-for-database-call)))

(di/with-eager-pre-hook! #'loupi.query/fetch
  "I am suprised by this error:
        2014-08-11 12:45:09.370:WARN:oejs.AbstractHttpConnection:/v0.1/User/sort/username/0/100/match-field/role/match-value/parent
        java.util.concurrent.ExecutionException: java.lang.AssertionError: Assert failed: (string? (:field-to-sort-by context-wrapper-for-database-call))
so I am adding logging."
  (fn [context-wrapper-for-database-call]
    (tb/log :trace "with-eager-pre-hook! #'loupi.persistence-queue/persist-this-item" context-wrapper-for-database-call)))

(di/with-eager-pre-hook! #'loupi.controller/entry-resource-post!
  "An optional docstring."
  (fn [ctx]
    (tb/log :trace "with-eager-pre-hook! #'loupi.controller/entry-resource-post!" ctx)))

(di/with-eager-pre-hook! #'loupi.controller/entry-resource-put!
  "An optional docstring."
  (fn [ctx]
    (tb/log :trace "with-eager-pre-hook! #'loupi.controller/entry-resource-put!" ctx)))

(di/with-post-hook! #'loupi.persistence/get-where-clause-map
  "An optional docstring."
  (fn [result]
    (tb/log :trace "Return value of loupi.persistence/get-where-clause-map:  " result)))