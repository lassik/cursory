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
          (render! t (conj clear [:text (str "Hello world" " " "{" e "}")]))
          (recur (read-event t))))
      (render! t clear))))
