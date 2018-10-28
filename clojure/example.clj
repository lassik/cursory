(ns example
  (:use cursory))

(let [t (get-terminal)]
  (println "ok")
  (with-raw-mode t
    (loop [e (read-event t)]
      (println "{" e "}\r")
      (when-not (= e "q")
        (recur (read-event t))))))
