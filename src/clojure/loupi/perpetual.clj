(ns loupi.perpetual
  (:require
   [loupi.remember :as remember]
   [loupi.monitoring :as monitor]
   [loupi.dates :as dates]
   [overtone.at-at :as at]
   [clojure.pprint :as pp]))



(defn resource-usage []
  (let [my-pool (at/mk-pool)]
    (at/every 3110000
              (fn []
                (println " resource usage is: ")
                (println "The time is: " (dates/current-time-as-string))
                (doseq [x (monitor/thread-top)]
                  (pp/pprint x))
                (println (str (monitor/show-stats-regarding-resources-used-by-this-app))))
              my-pool)))

(defn show-whole-memory-cache []
  (let [my-pool (at/mk-pool)]
    (at/every 1200000
              (fn []
                (println "The time is: " (dates/current-time-as-string))
                (println "We will now list everything in the memory cache:")
                (doseq [x (remember/get-whole-memory-cache)]
                  (pp/pprint x)))
              my-pool)))



