(ns linedit
  (:use cursory))

(def initial-state {:text "" :pos 0 :start 0 :limit 0})

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

(defn cmd-history-next [st]
  st)

(defn cmd-history-prev [st]
  st)

(defn cmd-complete [st]
  st)

(defn cmd-return [st]
  (println)
  initial-state)

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
    "Left" cmd-move-char-backward
    "Right" cmd-move-char-forward
    (if (= 1 (count event)) (partial type-text event) identity)))

;;

(def esc (partial str "\u001b"))
(def erase-line (esc "[2K"))
(def erase-display (esc "[2J"))
(def cursor-top-left (esc "[H"))
(defn cursor-backward [n] (if (> n 0) (esc "[" n "D") ""))

(def prompt "> ")

(defn realize-state [st]
  (let [ansi (str
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

(defn run []
  (let [t (get-terminal)]
    (with-raw-mode t
      (loop [st initial-state]
        (let [[ansi column] (realize-state st)]
          (print ansi)
          (flush)
          (let [event (read-event t)
                st (unrealize-state column ((command event) st))]
            (when st (recur st))))))))

(defn -main [prog]
  (run))
