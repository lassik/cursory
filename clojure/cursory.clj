(ns cursory
  (:require [clojure.core.match :refer [match]])
  (:import cursory.Cursory cursory.RenderAction))

(defn get-terminal []
  (Cursory/getTerminal 0))

(defn get-size [^Cursory cursory]
  (let [size (.getSize cursory)]
    (when size [(.x size) (.y size)])))

(defn get-cursor-pos [^Cursory cursory]
  (let [pos (.getCursorPos cursory)]
    (when pos [(.x pos) (.y pos)])))

(defmacro with-raw-mode [cursory & body]
  `(let [cursory# ~cursory]
     (try (do (.enableRawMode cursory#)
              ~@body)
          (finally (.restoreMode cursory#)))))

(defn read-event [^Cursory cursory]
  (let [event (.readEvent cursory)
        which (.which event)]
    (case (.eventType event)
      "" "(none)"
      "rune" which
      "specialkey" which
      "mousebutton" "mousebutton"
      "resize" "resize")))

(defn render! [^Cursory cursory actions]
  (. cursory render
     (map (fn [action]
            (match action
              [:clear-to-line-end]
              (RenderAction/clearToLineEnd)
              [:clear-to-screen-end]
              (RenderAction/clearToScreenEnd)
              [:go-abs x y]
              (RenderAction/goAbs ^int x ^int y)
              [:set-background-color color]
              (RenderAction/setBackgroundColor color)
              [:set-foreground-color color]
              (RenderAction/setForegroundColor color)
              [:text s]
              (RenderAction/text ^String s)
              [:box-char s n]
              (RenderAction/boxChar s n)))
          actions)))

(def clear-screen [[:set-background-color "default"]
                   [:set-foreground-color "default"]
                   [:go-abs 0 0]
                   [:clear-to-screen-end]])

(defn run-app! [^Cursory cursory initial-model update-model view-model]
  (with-raw-mode cursory
    (loop [model initial-model]
      (render! cursory (concat clear-screen (view-model model)))
      (let [event (read-event cursory)
            new-model (update-model model event)]
        (when new-model (recur new-model))))
    (render! cursory clear-screen)))
