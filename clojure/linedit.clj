(ns linedit
  (:require [clojure.string :as string])
  (:use cursory))

(def initial-state {:text "" :pos 0 :start 0 :limit 0 :histpos 0})

(defn reset-state [st] (merge st initial-state))

(defn clamp [pos st]
  (let [len (count (:text st))]
    (max 0 (min len pos))))

(defn grapheme-cluster-backward [pos st]
  (clamp (dec pos) st))

(defn grapheme-cluster-forward [pos st]
  (clamp (inc pos) st))

(defn is-word-rune-before [pos st]
  (and (> pos 0) (not (= " " (subs (:text st) (dec pos) pos)))))

(defn update-pos [f st]
  (assoc st :pos (clamp (f st) st)))

(defn delete-region [st]
  (let [old-text (:text st)
        before-region (subs old-text 0 (:start st))
        after-region (subs old-text (:limit st))
        len (- (:limit st) (:start st))
        new-text (str before-region after-region)
        old-pos (:pos st)
        new-pos (+ old-pos (max 0 (- old-pos (:start st))))]
    (update-pos (constantly new-pos)
                (assoc st
                       :text new-text
                       :limit (:start st)))))

;;

(defn set-region-line-backward [st]
  (assoc st
         :start 0
         :limit (:pos st)))

(defn set-region-line-forward [st]
  (assoc st
         :start (:pos st)
         :limit (count (:text st))))

(defn set-region-word-backward [st]
  (let [limit (clamp (:pos st) st)
        start
        (loop [start limit]
          (if (or (= 0 start) (is-word-rune-before start st))
            start
            (recur (dec start))))
        start
        (loop [start start]
          (if (or (= 0 start) (not (is-word-rune-before start st)))
            start
            (recur (dec start))))]
    (assoc st :start start :limit limit)))

(defn set-region-word-forward [st]
  st)

(defn set-region-char-backward [st]
  (let [oldpos (:pos st)
        newpos (grapheme-cluster-backward oldpos st)]
    (assoc st
           :start newpos
           :limit oldpos)))

(defn set-region-char-forward [st]
  (let [oldpos (:pos st)
        newpos (grapheme-cluster-forward oldpos st)]
    (assoc st
           :start oldpos
           :limit newpos)))

;;

(defn cmd-move-line-backward [st]
  (update-pos :start (set-region-line-backward st)))

(defn cmd-move-line-forward [st]
  (update-pos :limit (set-region-line-forward st)))

(defn cmd-move-word-backward [st]
  (update-pos :start (set-region-word-backward st)))

(defn cmd-move-word-forward [st]
  (update-pos :limit (set-region-word-forward st)))

(defn cmd-move-char-backward [st]
  (update-pos :start (set-region-char-backward st)))

(defn cmd-move-char-forward [st]
  (update-pos :limit (set-region-char-forward st)))

;;

(defn cmd-delete-line-backward [st]
  (update-pos :start (delete-region (set-region-line-backward st))))

(defn cmd-delete-line-forward [st]
  (update-pos :start (delete-region (set-region-line-forward st))))

(defn cmd-delete-word-backward [st]
  (update-pos :start (delete-region (set-region-word-backward st))))

(defn cmd-delete-word-forward [st]
  (update-pos :start (delete-region (set-region-word-forward st))))

(defn cmd-delete-char-backward [st]
  (update-pos :start (delete-region (set-region-char-backward st))))

(defn cmd-delete-char-forward [st]
  (update-pos :start (delete-region (set-region-char-forward st))))

;;

(defn type-text [typed-text st]
  (let [old-text (:text st)
        old-pos (:pos st)
        before (subs old-text 0 old-pos)
        after (subs old-text old-pos)
        new-text (str before typed-text after)
        new-pos (+ old-pos (count typed-text))]
    (update-pos (constantly new-pos) (assoc st :text new-text))))

(defn history-common [update-fn st]
  (let [newpos (max 0 (update-fn (:histpos st)))
        newtext (if (= 0 newpos)
                  (:text st)
                  ((-> st :config :history :get) st (dec newpos)))]
    (if newtext
      (assoc (reset-state st) :histpos newpos :text newtext)
      st)))

(def cmd-history-prev (partial history-common inc))

(def cmd-history-next (partial history-common dec))

(defn cmd-complete [st]
  st)

(defn cmd-return [st]
  (println)
  (reset-state ((-> st :config :history :add) st (:text st))))

(defn cmd-interrupt [st]
  (println)
  nil)

(defn cmd-delete-char-forward-or-eof [st]
  (if (= 0 (count (:text st)))
    (do (println) nil)
    (cmd-delete-char-forward st)))

(defn cmd-clear-screen [st]
  (assoc st :clear true))

(defn command [event]
  (case event
    "Control-A" cmd-move-line-backward
    "Control-B" cmd-move-char-backward
    "Control-C" cmd-interrupt
    "Control-D" cmd-delete-char-forward-or-eof
    "Control-E" cmd-move-line-forward
    "Control-F" cmd-move-char-forward
    "Control-K" cmd-delete-line-forward
    "Control-L" cmd-clear-screen
    "Control-N" cmd-history-next
    "Control-P" cmd-history-prev
    "Control-U" cmd-delete-line-backward
    "Control-W" cmd-delete-word-backward
    "Alt-D" cmd-delete-word-forward
    "Alt-Backspace" cmd-delete-word-backward
    "Tab" cmd-complete
    "Return" cmd-return
    "Backspace" cmd-delete-char-backward
    "Delete" cmd-delete-char-forward
    "Up" cmd-history-prev
    "Down" cmd-history-next
    "Left" cmd-move-char-backward
    "Right" cmd-move-char-forward
    "Home" cmd-move-line-backward
    "End" cmd-move-line-forward
    (if (= 1 (count event)) (partial type-text event) identity)))

;;

(def esc (partial str "\u001b"))
(def erase-line (esc "[2K"))
(def erase-display (esc "[2J"))
(def cursor-top-left (esc "[H"))
(defn cursor-backward [n] (if (> n 0) (esc "[" n "D") ""))

(defn realize-state [st]
  (let [prompt ((or (-> st :config :prompt) (constantly "> ")) st)
        ansi (str
              prompt
              (:text st)
              (cursor-backward (- (count (:text st)) (:pos st))))
        column (+ (count prompt) (:pos st))]
    [ansi column]))

;;

(defn unrealize-state [old-column new-state]
  (let [clear (:clear new-state) new-state (dissoc new-state :clear)]
    (print (if clear
             (str erase-display cursor-top-left)
             (str (cursor-backward old-column) erase-line)))
    new-state))

(defn run [config]
  (let [t (get-terminal)]
    (with-raw-mode t
      (loop [st (assoc initial-state :config config)]
        (let [[ansi column] (realize-state st)]
          (print ansi)
          (flush)
          (let [event (read-event t)
                st (unrealize-state column ((command event) st))]
            (when st (recur st))))))))

;;

(def natural-history
  {:items []
   :get (fn [st i] (first (nthnext (-> st :config :history :items) i)))
   :add (fn [st s]
          (if (string/blank? s)
            st
            (update-in st [:config :history :items]
                       (fn [items] (conj (remove (partial = s) items) s)))))})

(defn -main [prog]
  (run {:history natural-history :prompt (constantly "partycat> ")}))
