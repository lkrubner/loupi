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

(di/with-handler! #'loupi.query/fetch
  "I am suprised by this error:
        2014-08-11 12:45:09.370:WARN:oejs.AbstractHttpConnection:/v0.1/User/sort/username/0/100/match-field/role/match-value/parent
        java.util.concurrent.ExecutionException: java.lang.AssertionError: Assert failed: (string? (:field-to-sort-by context-wrapper-for-database-call))
so I am adding logging."
  java.util.concurrent.ExecutionException
  (fn [e & args]
    (tb/log :trace "Error in loupi.query/fetch: " e " " args)))

(di/with-post-hook! #'loupi.persistence/get-where-clause-map
  "An optional docstring."
  (fn [result]
    (tb/log :trace "Return value of loupi.persistence/get-where-clause-map:  " result)))

(di/with-handler! #'loupi.persistence/get-where-clause-map
  java.lang.Exception
  (fn [e & args]
    (tb/log :trace "Return value of loupi.persistence/get-where-clause-map:  " e " and args: ")
    (doseq [x args]
      (tb/log :trace "in loupi.persistence/get-where-clause-map, this arg:" x " had type: " (type x)))
    (if (seq args)
      (if (map? (first args))
        (doseq [[k v] (first args)]
          (tb/log :trace "in loupi.persistence/get-where-clause-map, this key:" k " had value of type: " (type v)))))))

(di/with-handler! #'loupi.controller/entry-resource-handle-ok
  "2014-08-26 - I am surprised that this: 

curl --verbose -X DELETE -H 'Content-Type: application/json' http://das.launchopen.com/new-unstable-api/v0.1/User/553fa5cace4b03a899c746acb

gave me this: 

WARN:oejs.AbstractHttpConnection:/v0.1/User/553fa5cace4b03a899c746acb
java.util.concurrent.ExecutionException: java.lang.IllegalArgumentException: invalid ObjectId [553fa5cace4b03a899c746acb]

adding a handler to catch the exception.
"
  java.util.concurrent.ExecutionException
  (fn [e & args]
    (tb/log :trace "Return value of loupi.controller/entry-resource-handle-ok:  " e " args: " args)))
