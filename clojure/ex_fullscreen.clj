;; Full-screen example

(ns ex_fullscreen
  (:use cursory))

(def clear [[:go-abs 0 0] [:clear-to-screen-end]])

(defn -main [args]
  (let [t (get-terminal)]
    (println "ok")
    (with-raw-mode t
      (loop [e nil]
        (when-not (= e "q")
          (render! t (conj clear
                           [:set-background-color "default"]
                           [:set-foreground-color "red"]
                           [:text "Hello"]
                           [:set-background-color "green"]
                           [:set-foreground-color "black"]
                           [:text "World"]
                           [:set-background-color "default"]
                           [:set-foreground-color "cyan"]
                           [:text (str " {" e "}")]
                           [:set-background-color "default"]))
          (recur (read-event t))))
      (render! t clear))))


