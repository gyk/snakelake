(ns snakelake.communication
  (:require
    [snakelake.ainit]
    [snakelake.model :as model]
    [taoensso.sente :as sente]))

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(defonce channel-socket
  (sente/make-channel-socket!
    "/chsk"
    ?csrf-token
    {:type :auto
     :port 3008}))

(defonce chsk (:chsk channel-socket))
(defonce ch-chsk (:ch-recv channel-socket))
(defonce chsk-send! (:send-fn channel-socket))
(defonce chsk-state (:state channel-socket))

(defn dir [dx dy]
  (chsk-send! [:snakelake/dir [dx dy]]))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [{:as ev-msg :keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod event-msg-handler :chsk/state [{:as ev-msg :keys [?data]}]
  (if (= ?data {:first-open? true})
    (println "Channel socket successfully established!")
    (println "Channel socket state change:" ?data)))

(defmethod event-msg-handler :chsk/recv [{:as ev-msg :keys [?data]}]
  (model/world! (second ?data)))

(defn send-username []
  (chsk-send! [:snakelake/username (:username @model/app-state)]))

(defmethod event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake:" ?data)
    (model/uid! ?uid)
    (send-username)))

(defonce router
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))

(defn respawn []
  (chsk-send! [:snakelake/respawn]))
