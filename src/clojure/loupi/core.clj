(ns loupi.core
  (:gen-class)
  (:require [loupi.supervisor :as s]))

(defn -main [& args]
  (s/start args))

