(ns loupi.remember
  (:require
   [clojure.core.incubator :as incubator]
   [dire.core :refer [with-postcondition with-handler supervise]]))


(def ^:private remember (atom {}))

(defn get-from-memory-cache[vector-of-locators]
  {:pre [(vector? vector-of-locators)]
   :post [(future? %)]}
  (get-in @remember vector-of-locators))

(defn add-to-memory-cache[vector-of-locators future-function]
  {:pre [
         (vector? vector-of-locators)
         (future? future-function)
         ]}
  (swap! remember (fn [map-of-cached-data]
                        (assoc-in map-of-cached-data vector-of-locators future-function))))

(defn delete-from-memory-cache[vector-of-locators]
  {:pre [(vector? vector-of-locators)]}
  (swap! remember (fn [map-of-cached-data]
                        (incubator/dissoc-in map-of-cached-data vector-of-locators))))



