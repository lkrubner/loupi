(ns loupi.perpetual
  (:require
   [loupi.remember :as remember]
   [loupi.monitoring :as monitor]
   [loupi.dates :as dates]
   [overtone.at-at :as at]
   [clojure.pprint :as pp]))



(def my-pool (at/mk-pool))

(defn resource-usage []
  (at/every 311000
            (fn []
              (println "The time is: " (dates/current-time-as-string))
              (doseq [x (monitor/thread-top)]
                (pp/pprint x))
              (println (str (monitor/show-stats-regarding-resources-used-by-this-app)))) my-pool))

(defn show-whole-memory-cache []
  (at/every 120000
            (fn []
              (println "The time is: " (dates/current-time-as-string))
              (println "We will now list everything in the memory cache:")
              (doseq [x (remember/get-whole-memory-cache)]
                (pp/pprint x))) my-pool))



