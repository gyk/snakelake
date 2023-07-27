(ns snakelake.system
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [integrant.core :as ig]
    [snakelake.routes :as http-server]))

;; Shared common refs

(defmethod ig/init-key :dev?
  [_ value]
  (true? value))


;; HTTP server

(defmethod ig/init-key :http/server
  [_ options]
  (http-server/start options))

(defmethod ig/halt-key! :http/server
  [_ server]
  (http-server/stop server))

;; System

(defn- load-config
  [profile]
  (let [file (or (System/getenv "SNAKELAKE_CONFIG_RESOURCE") "config.edn")]
    (aero/read-config (io/resource file) {:profile profile})))

(defn prep
  ([]
   (prep :default))
  ([profile]
   (doto
     (load-config profile)
     ig/load-namespaces)))

(defn init-from-config
  [config]
  (ig/init config))

(defn stop
  [system]
  (ig/halt! system))
