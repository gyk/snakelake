(ns snakelake.main
  (:require
    [helix.core :refer [$ <>]]
    [helix.hooks]
    ["react-dom" :as rdom]
    ["react-dom/client" :as react-dom-client]
    [snakelake.ainit]
    [snakelake.view :as view]))

(defn init [])

(defonce react-root nil)

(defn- -render
  "Renders element into DOM node. The first argument is React element."
  [element root]
  (.render ^js root element))

(defn- create-root
  [node]
  (react-dom-client/createRoot node))

(defn render
  []
  (set! react-root (create-root (.getElementById js/document "app")))
  (-render ($ view/main) react-root))

(render)
