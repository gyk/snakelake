(ns snakelake.routes
  (:require
    [org.httpkit.server :as http]
    [reitit.middleware :as middleware]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as m.coercion]
    [reitit.ring.middleware.muuntaja :as m.muuntaja]
    [ring.middleware.anti-forgery :as anti-forgery]
    [ring.middleware.cors :as cors]
    [ring.middleware.defaults :as defaults]
    [ring.middleware.session :as session]
    [ring.util.response :as response]
    [snakelake.model :as model]
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]))

(declare channel-socket)

(defn start-websocket []
  (defonce channel-socket
    (sente/make-channel-socket!
      (get-sch-adapter)
      ; FIXME
      {:csrf-token-fn nil
       :user-id-fn    #'model/next-uid})))

(defn- wrap-cors
  [handler]
  (cors/wrap-cors
    handler
    :access-control-allow-origin #".*"
    :access-control-allow-methods [:get :put :post :delete]
    :access-control-allow-credentials "true"))

(defn make-handler
  [{:keys [dev?] :as config}]
  (ring/ring-handler
    (ring/router
      (concat
        [["/status"
          {:get  (fn [_req]
                   (-> (str "Running: " (pr-str @(:connected-uids channel-socket)))
                       (response/response)
                       (response/header "content-type" "text/plain")))
           :post (fn [req]
                   ((:ajax-get-or-ws-handshake-fn channel-socket) req))}]
         ["/chsk"
          {:get  (fn [req]
                   ((:ajax-get-or-ws-handshake-fn channel-socket) req))
           :post (fn [req]
                   ((:ajax-post-fn channel-socket) req))}]
         ["/ping"
          {:get (fn [_request]
                  (-> "pong"
                      (response/response)
                      (response/header "content-type" "text/plain")))}]])

      {::middleware/registry
       {:session             session/wrap-session
        :anti-forgery        anti-forgery/wrap-anti-forgery
        :cors                wrap-cors
        :params/wrap-keyword ring.middleware.keyword-params/wrap-keyword-params
        :params/wrap         ring.middleware.params/wrap-params
        :format              m.muuntaja/format-middleware
        :format/request      m.muuntaja/format-request-middleware
        :format/response     m.muuntaja/format-response-middleware
        :format/negotiate    m.muuntaja/format-negotiate-middleware
        :coercion/exceptions m.coercion/coerce-exceptions-middleware
        :coercion/request    m.coercion/coerce-request-middleware
        :coercion/response   m.coercion/coerce-response-middleware
        }
       :data
       {:middleware [:cors
                     :session
                     :params/wrap-keyword
                     :params/wrap
                     :format/negotiate
                     :format/response]}})

    ;; optional default ring handler (if no routes have matched)
    (->> [(ring/create-resource-handler {:path "/" :root "public"})
          (ring/create-default-handler {:not-found "Not found"})]
         (filter some?)
         (apply ring/routes))
    ))

(defmulti event :id)

(defmethod event :default [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod event :snakelake/dir [{:as ev-msg :keys [event uid ?data]}]
  (let [[dx dy] ?data]
    (model/dir uid dx dy)))

(defmethod event :snakelake/username [{:as ev-msg :keys [event uid ?data]}]
  (model/username uid ?data))

(defmethod event :snakelake/respawn [{:as ev-msg :keys [event uid ?data]}]
  (model/enter-game uid))

(defmethod event :chsk/uidport-open [{:keys [uid client-id]}]
  (println "New connection:" uid client-id)
  (model/enter-game uid))

(defmethod event :chsk/uidport-close [{:keys [uid]}]
  (println "Disconnected:" uid)
  (model/remove-player uid))

(defmethod event :chsk/ws-ping [_])

(defn start-router []
  (defonce router
    (sente/start-chsk-router! (:ch-recv channel-socket) event)))

(defn broadcast []
  (doseq [uid (:any @(:connected-uids channel-socket))]
    ((:send-fn channel-socket) uid [:snakelake/world @model/world])))

(defn ticker []
  (while true
    (Thread/sleep 500)
    (try
      (model/tick)
      (broadcast)
      (catch Exception ex
        (println ex)))))

(defn start-ticker []
  (defonce ticker-thread
    (doto (Thread. ticker)
      (.start))))

(defn wrap-system
  [handler system]
  (fn
    ([request]
     (handler (merge request system)))
    ([request respond raise]
     (handler (merge request system) respond raise))))

(defn start
  [{:keys [config] :as opts}]
  (println "HTTP server is starting")
  (let [system {::config config}
        app    (-> (make-handler config)
                   (defaults/wrap-defaults (assoc-in defaults/site-defaults
                                                     [:security :anti-forgery]
                                                     false))
                   (wrap-system system))
        _      (start-websocket)
        router (start-router)
        _      (start-ticker)
        server (http/run-server
                 app
                 {:ip   (:ip config)
                  :port (:port config)})]
    {:stop-server server
     :stop-router router}))

(defn stop
  [{:keys [stop-server stop-router]}]
  (println "HTTP server is stopping")
  (stop-server :timeout 3000)
  (when (fn? stop-router)
    (stop-router))
  (when (some? ticker-thread)
    (.interrupt ticker-thread))
  )
