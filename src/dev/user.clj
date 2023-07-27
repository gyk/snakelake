(ns user
  (:require
    [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
    [integrant.repl :as ig-repl]
    [integrant.repl.state :as state]
    [snakelake.system :as system]))

(set-refresh-dirs "src/server" "src/dev" "test")

(ig-repl/set-prep! #(system/prep :dev))

(def go ig-repl/go)

(def stop ig-repl/halt)

(defn reset
  []
  (ig-repl/reset))

(def reset-all ig-repl/reset-all)

(comment

  (go)
  (stop)
  (reset)
  (reset-all)

  state/system

  nil)

;; as main
(defn -main
  [& _args]
  (go))

