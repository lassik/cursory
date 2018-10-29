(ns cursory
  (:import cursory.Cursory))

(defn get-terminal []
  (Cursory/getTerminal 0))

(defn get-size [^Cursory cursory]
  (let [size (.getSize cursory)]
    [(.x size) (.y size)]))

(defmacro with-raw-mode [cursory & body]
  `(let [cursory# ~cursory]
     (try (do (.enableRawMode cursory#)
              ~@body)
          (finally (.restoreMode cursory#)))))

(defn read-event [^Cursory cursory]
  (let [event (.readEvent cursory)
        which (.which event)]
    (case (.eventType event)
      "" "(none)"
      "rune" which
      "controlkey" (str "Control-" which)
      "specialkey" which
      "mousebutton" "mousebutton"
      "resize" "resize")))
