(ns snakelake.main
  (:require
    [integrant.repl :as ig-repl]
    [snakelake.system :as system])
  (:gen-class))

(defn stop-app
  []
  (ig-repl/halt)
  (shutdown-agents))

(defn start-app
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop-app))
  (ig-repl/set-prep! #(system/prep :prod))
  (ig-repl/go))

(defn -main
  [& _args]
  (start-app))
