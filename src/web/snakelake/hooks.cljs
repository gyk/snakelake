(ns snakelake.hooks
  (:require
    ["react" :as react]))

; https://github.com/lilactown/hooks-demo/blob/master/src/hooks_demo.cljs
; https://github.com/roman01la/hooks

(defn use-atom
  ;; if no deps are passed in, we assume we only want to run
  ;; subscrib/unsubscribe on mount/unmount
  ([a] (use-atom a []))
  ([a deps]
   ;; create a react/useState hook to track and trigger renders
   (let [[v u] (react/useState @a)]
     ;; react/useEffect hook to create and track the subscription to the iref
     (react/useEffect
       (fn []
         (println "adding watch")
         (add-watch a :use-atom
                    ;; update the react state on each change
                    (fn [_ _ _ v'] (u v')))
         ;; return a function to tell react hook how to unsubscribe
         #(do
            (println "removing watch")
            (remove-watch a :use-atom)))
       ;; pass in deps vector as an array
       (clj->js deps))
     ;; return value of useState on each run
     v)))
