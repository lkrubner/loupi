(ns loupi.perpetual
  (:import
   (java.util TimerTask Timer))
  (:require
   [loupi.remember :as remember]
   [loupi.monitoring :as monitor]
   [loupi.dates :as dates]
   [clojure.pprint :as pp]))


(defn resource-usage []
  (let [task (proxy [TimerTask] [] (run []
                                     (do
                                       (println "The time is: " (dates/current-time-as-string))
                                       (doseq [x (monitor/thread-top)]
                                         (pp/pprint x))
                                       (println (str (monitor/show-stats-regarding-resources-used-by-this-app))))))]
    (. (new Timer) (schedule task (long 311000)))))

(defn show-whole-memory-cache []
  (let [task (proxy [TimerTask] [] (run []
                                     (do
                                       (println "The time is: " (dates/current-time-as-string))
                                       (println "We will now list everything in the memory cache:")
                                       (doseq [x (remember/get-whole-memory-cache)]
                                         (pp/pprint x)))))]
    (. (new Timer) (schedule task (long 120000)))))




