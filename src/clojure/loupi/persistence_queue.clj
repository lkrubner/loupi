(ns loupi.persistence-queue
  (:require
   [loupi.persistence :as persistence]
   [slingshot.slingshot :as ss]
   [lamina.core :as lamina]))


(def ^:private persistence-channel (lamina/channel))

(defn persist-this-item [context-wrapper-for-database-call]
  (lamina/enqueue persistence-channel
                  (fn []
                    (ss/try+ 
                     (persistence/make-consistent context-wrapper-for-database-call)
                     (catch Object o (ss/throw+ {:type :loupi.supervisor/problem
                                                 :message "Error in persistence-queue/persist-this-itme."
                                                 :data o}))))))

(defn worker []
  (loop [closure-with-item-inside @(lamina/read-channel persistence-channel)]
    (ss/try+ 
     (closure-with-item-inside)
     (catch Object o (ss/throw+ {:type :loupi.supervisor/problem
                                 :message "Error in persistence-queue/worker."
                                 :closure closure-with-item-inside
                                 :data o})))
    (recur @(lamina/read-channel persistence-channel))))

(defn start-workers []
  (lamina/on-closed persistence-channel
                    (fn [] (ss/throw+
                            {:type :loupi.supervisor/persistence-channel-closed
                             :message "In persistence-queue, an error has closed the persistence-channel"}))) 
  (lamina/on-drained persistence-channel
                     (fn [] (ss/throw+
                             {:type :loupi.supervisor/persistence-channel-drained
                              :message "In persistence-queue, an error has closed the persistence-channel"})))
  (dotimes [_ 20]
    (println "Starting up the persist queue workers.")
    (future (worker))))
