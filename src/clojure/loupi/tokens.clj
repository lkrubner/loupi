(ns loupi.tokens
  )

(def t (atom {}))

(defn create-token []
  "2014-08-07 - if the user is logged in, we use their session id. If the frontend code can not use a session id for a user, then we create a new token."

  )

(defn increment-token [token ip-address]
  (swap! t (fn [old-map-of-t]
             (let [new-map-of-t (assoc old-map-of-t token
                                       (+ (get old-map-of-t token 0) 1))
                   new-map-of-t (assoc new-map-of-t ip-address
                                       (+ (get new-map-of-t ip-address 0) 1))]
               new-map-of-t))))

(defn is-valid? [token ip-address]
  (if (> (get @t token 0) 1000)
    false
    (if (> (get @t ip-address 0) 1000)
      false
      true)))



