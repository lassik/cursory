(ns cursory
  (:import cursory.Cursory))

(defn get-terminal []
  (Cursory/getTerminal 0))

(defn get-size [^Cursory cursory]
  (let [size (.getSize cursory)]
    [(.x size) (.y size)]))
