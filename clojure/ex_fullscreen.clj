;; Full-screen example

(ns ex_fullscreen
  (:use cursory))

(def initial-model nil)

(defn update-model [model event]
  (when-not (= "q" event) event))

(defn view-model [model]
  [[:set-background-color "default"]
   [:set-foreground-color "red"]
   [:text "Hello"]
   [:set-background-color "green"]
   [:set-foreground-color "black"]
   [:text "World"]
   [:set-background-color "default"]
   [:set-foreground-color "cyan"]
   [:text (str " {" model "}")]
   [:set-background-color "default"]])

(defn -main [args]
  (run-app! (get-terminal) initial-model update-model view-model))


