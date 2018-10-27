(ns example
  (:use cursory))

(let [t (get-terminal)]
  (with-raw-mode t
    (loop [e (read-event t)]
      (when e
        (println e)
        (when-not (= e "q")
          (recur (read-event t)))))))
