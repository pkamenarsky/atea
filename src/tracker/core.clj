(ns tracker.core
  (:require [clojure.string :as string])
  (:import org.jdesktop.jdic.tray.internal.impl.MacSystemTrayService)
  (:import org.jdesktop.jdic.tray.internal.impl.MacTrayIconService))

(defn load-icon [name]
  (let [is (.getResourceAsStream (ClassLoader/getSystemClassLoader) name)]
    (javax.swing.ImageIcon. (javax.imageio.ImageIO/read is))))

(defn get-tray []
  (MacSystemTrayService/getInstance))

(defn create-menu []
  (MacTrayIconService.))

(defn action [f]
  (reify java.awt.event.ActionListener 
    (actionPerformed 
      [this event] (f))))

; Item management ----------------------------------------------------------

(defn now []
  (.getTime (java.util.Date.)))

(defn to-mins [msec]
  (quot msec 1000))

(defn to-str [mins]
  (format "[%02d:%02d]" (quot mins 60) (mod mins 60)))

(defn parition-items [items]
  ; first group by priority into a {pri item} map,
  ; then group the first 10 items of every priority by project
  (reduce (fn [a [pri item]]
            (assoc a pri (group-by :project (take 10 item))))
          {}
          (group-by :priority items)))

(defn update-items [menu items actfn deactfn]
  ; project / priority section functions
  (let [add-section
        (fn [add-sep? title sec-items]
          (if add-sep?
            (.addSeparator menu)) 
          (.addItem menu title nil)
          (.addSeparator menu)
          (doseq
            [item sec-items]
            (.addItem
              menu
              (if (= (get-in items [:active :number]) (:number item))
                (str "➡ " (:description item))  ;◆✦●
                (:description item)) 
              (action #(actfn (assoc item :since (now)))))))

        add-priority
        (fn [add-sep? priority prjs]
          (add-section
            add-sep?
            (str "Priority " priority " - " (key (first prjs)))
            (val (first prjs)))
          (doseq [[prj items] (next prjs)] (add-section true prj items)))]

    ; remove old items
    (doseq [index (range (.getItemCount menu))] (.removeItem menu 0)) 

    ; add "now working" section
    (when (:active items)
      (let [stime (to-mins (- (now) (get-in items [:active :since])))]
        (.addItem
          menu
          (str "Session: "
               (to-str stime)
               " - Sum: "
               (to-str (+ (get-in items [:active :time]) stime)))
          nil) 
        (.addSeparator menu)
        (.addItem menu "Stop work" (action #(deactfn)))
        (.addSeparator menu))) 

    ; add items sorted by priority and project
    (let [part-items (sort (parition-items (:bugs items)))]
      (add-priority false (key (first part-items)) (val (first part-items)))
      (doseq [[pri prjs] (next part-items)] (add-priority true pri prjs)))))

; IO -----------------------------------------------------------------------

(defn maybe-int [string v]
  (try
    (Integer. string)
    (catch Exception e v)))

(defn parse-bug [bugs [number line]]
  (if (re-matches #"[^#\s].*" line) 
    (conj bugs 
          (let [tokens (string/split line #"\s+" 4)
                t (maybe-int (tokens 2) nil)]
            {:number number
             :priority (maybe-int (tokens 0) 999)
             :project (tokens 1)
             :time (if t t 0)
             :description (if t
                            (tokens 3)
                            ; if no time given, desc is 2nd token
                            ((string/split line #"\s+" 3) 2))}))
    bugs))

(def status-re #"# Working on \"(.*)\" in \"(.*)\" since (\d*)")

(defn drop-last-elems [pred coll]
  (reverse (drop-while pred (reverse coll))))

(defn pad-tabs [s n]
  (str s (apply str (repeat (- n (quot (count s) n)) "\t"))))

(defn write-bug [bug]
  (format "%s\t%s\t%s\t%s"
          (pad-tabs (:priority bug) 1) 
          (pad-tabs (:project bug) 3)
          (pad-tabs (str (:time bug)) 2) 
          (:description bug)))

(defn load-bugs [file]
  (try 
    (let [lines (string/split-lines (slurp file))
          bugs (reduce parse-bug [] (map vector (range (count lines)) lines))
          active-match (some (partial re-matches status-re) lines)]
 
      ; drop the status line and all trailing empty lines
      {:lines (vec (drop-last-elems
                     (comp empty? string/trim)
                     (filter #(not (re-matches status-re %)) lines))) 
       :bugs bugs
 
       ; active task only when match has 4 tokens
       :active (when (= (count active-match) 4)   
                 (some #(when
                          (and (= (:description %) (active-match 1))
                               (= (:project %) (active-match 2)))
                          (into % {:since (Long. (active-match 3))})) bugs))})
    (catch Exception e nil)))

(defn write-bugs [file bugs new-active]
  (try
    ; add elapsed time to old active bug and update its associated line
    (let [old-active (:active bugs)
          a (println "old active time: " (:time old-active))
          lines (if old-active
                  (assoc
                    (:lines bugs)
                    (:number old-active)
                    (write-bug (update-in
                                 old-active
                                 [:time]
                                 #(+ % (to-mins (- (now) (:since old-active)))))))
                  (:lines bugs))]

      ; write out new lines & new active bug
      (spit file (string/join "\n" (if new-active
                                     (conj lines
                                           (format
                                             "\n\n# Working on \"%s\" in \"%s\" since %d"
                                             (:description new-active)
                                             (:project new-active) 
                                             (:since new-active)))
                                     lines))))))

; Track file updates -------------------------------------------------------

(defn watch-file [filename interval f]
  (let [file (java.io.File. filename)
        timestamp (atom 0)
        listener (reify java.awt.event.ActionListener
                   (actionPerformed
                     [this event]
                     (if (not= @timestamp (.lastModified file))
                       (do
                         (f)
                         (reset! timestamp (.lastModified file))))))
        timer (javax.swing.Timer. interval listener)]
    (.start timer)
    timer))

; main ---------------------------------------------------------------------

(def default-cfg {:file "tracker.txt"})

(defn write-default-cfg []
  (spit ".kali" (pr-str default-cfg)))

(defn load-cfg []
  (try
    (load-file ".kali")
    (catch Exception e
      (do
        (write-default-cfg)
        default-cfg))))

(defn main []
  (let [icon (load-icon "resources/clock.png")
        menu (create-menu)]
    (.addTrayIcon (get-tray) menu 0)
    (.setIcon menu icon)
    (.setActionListener
      menu
      (action #(let [file (:file (load-cfg))
                     bugs (load-bugs file)]
                 (when bugs
                   (update-items menu bugs
                                 (fn [active] (write-bugs file bugs active)) 
                                 (fn [] (write-bugs file bugs nil)))))))))

