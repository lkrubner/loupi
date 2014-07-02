(ns loupi.perpetual
  (:require
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



