(ns cursory
  (:import CursoryUnix))

(defn get-size []
  (let [size (-> (new CursoryUnix 0) .getSize)]
    [(-> size .x) (-> size .y)]))
