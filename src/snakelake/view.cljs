(ns snakelake.view
  (:require
    [goog.events :as events]
    [goog.events.KeyCodes :as KeyCodes]
    [snakelake.model :as model]
    [snakelake.communication :as communication]))

(defn keydown [e]
  (.preventDefault e)
  (condp = (.-keyCode e)
    KeyCodes/LEFT (communication/dir -1 0)
    KeyCodes/RIGHT (communication/dir 1 0)
    KeyCodes/UP (communication/dir 0 -1)
    KeyCodes/DOWN (communication/dir 0 1)
    nil))

(defonce listener
  (events/listen js/document "keydown" keydown))

(defn segment [uid i j me?]
  [:rect
   {:x (+ i 0.55)
    :y (+ j 0.55)
    :fill (subs uid 0 7)
    :stroke-width 0.3
    :stroke (subs uid 7 14)
    :rx (if me? 0.4 0.2)
    :width 0.9
    :height 0.9}])

(defn food [i j]
  [:circle
   {:cx (inc i)
    :cy (inc j)
    :r 0.45
    :fill "lightgreen"
    :stroke-width 0.2
    :stroke "green"}])

(defn eye [dx dy]
  [:circle
   {:cx (/ dx 2)
    :cy (/ dy 2)
    :r 0.2
    :stroke "black"
    :stroke-width 0.05
    :fill "red"}])

(defn board [{{:keys [board players]} :world my-uid :uid}]
  (let [width (count (first board))
        height (count board)]
    [:svg.board
     {:style {:border "1px solid black"
              :width "90%"}
      :view-box [0 0 (inc width) (inc height)]}
     (doall
       (for [i (range width)
             j (range height)
             :let [uid (get-in board [j i])]
             :when uid]
         ^{:key [i j]}
         (if (= uid "food")
           [food i j]
           [segment uid i j (= my-uid uid)])))
     (doall
       (for [[uid [health x y dx dy]] players
             :when (= health :alive)]
         ^{:key uid}
         [:g
          {:transform (str "translate(" (inc x) " " (inc y) ")")}
          [eye
           (+ (/ dx 2) (/ dy 2))
           (+ (/ dy 2) (/ dx 2))]
          [eye
           (- (/ dx 2) (/ dy 2))
           (- (/ dy 2) (/ dx 2))]]))]))

(defn main []
  [:div.content
   [:h1 "Snakelake"]
   [:center
    [board @model/app-state]]])