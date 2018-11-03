;; Full-screen example

(ns ex_fullscreen
  (:use cursory))

(def initial-model nil)

(defn update-model [model event]
  (when-not (= "q" event) event))

(defn box [w h text]
  (let [xorig 0 yorig 1]
    (vec (concat [[:go-abs xorig yorig]
                  [:box-char "corner-top-left"]
                  [:box-char "horz" w]
                  [:box-char "corner-top-right"]]
                 (mapcat (fn [y]
                           [[:go-abs xorig (+ yorig y 1)]
                            [:box-char "vert" 1]
                            [:text (apply str (repeat w \space))]
                            [:box-char "vert" 1]])
                         (range h))
                 [[:go-abs xorig (+ yorig h 1)]
                  [:box-char "corner-bottom-left"]
                  [:box-char "horz" w]
                  [:box-char "corner-bottom-right"]]))))

(defn view-model [model]
  (vec (concat
        [[:set-background-color "default"]
         [:set-foreground-color "red"]
         [:text "Hello"]
         [:set-background-color "green"]
         [:set-foreground-color "black"]
         [:text "World"]
         [:set-background-color "default"]
         [:set-foreground-color "cyan"]
         [:text (str " {" model "}")]
         [:box-char "cross"]]
        (box 20 10 "Hello")
        [[:set-background-color "default"]])))

(defn -main [args]
  (run-app! (get-terminal) initial-model update-model view-model))


