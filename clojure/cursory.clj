(ns cursory
  (:import Termios))

(defn get-size []
  (let [size (-> (new Termios 0) .getSize)]
    [(-> size .x) (-> size .y)]))
