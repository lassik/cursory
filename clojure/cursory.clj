(ns cursory
  (:import cursory.Cursory))

(defn get-size []
  (let [size (-> (Cursory/getTerminal 0) .getSize)]
    [(-> size .x) (-> size .y)]))
