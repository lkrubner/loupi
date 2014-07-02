(ns loupi.perpetual
  (:require
   [loupi.remember :as remember]
   [loupi.monitoring :as monitor]
   [loupi.dates :as dates]
   [clojure.pprint :as pp]))



(defn resource-usage []
  ;; 2013-05-28 - THIS FUNCTION IS CALLED AT STARTUP AND RUNS IN ITS OWN THREAD, REPEATING ENDLESSLY.
  ;;
  (println "The time is: " (dates/current-time-as-string))
  (doseq [x (monitor/thread-top)]
    (pp/pprint x))
  (println (str (monitor/show-stats-regarding-resources-used-by-this-app)))
  (. java.lang.Thread sleep 120000)
  (resource-usage))

(defn show-whole-memory-cache []
  ;; 2013-05-28 - THIS FUNCTION IS CALLED AT STARTUP AND RUNS IN ITS OWN THREAD, REPEATING ENDLESSLY.
  ;;
  (println "The time is: " (dates/current-time-as-string))
  (println "We will now list everything in the memory cache:")
  (doseq [x (remember/get-whole-memory-cache)]
    (pp/pprint @x))
  (. java.lang.Thread sleep 120000)
  (show-whole-memory-cache))





