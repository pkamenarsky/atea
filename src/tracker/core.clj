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
              (action #(actfn (assoc item :since (.getTime (java.util.Date.))))))))

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
      (do
        (.addItem
          menu
          "[00:16] - stop working"
          (action #(deactfn))) 
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

(defn write-bug [bug]
  (apply format "%s\t%s\t%s\t%s" (map bug [:priority :project :time :description])))

(defn load-bugs [file]
  (with-open [rdr (java.io.BufferedReader. 
                    (java.io.FileReader. file))]
    (let [lines (line-seq rdr)
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
                          (into % {:since (Long. (active-match 3))})) bugs))})))

(defn write-bugs [file bugs active]
  (with-open [wtr (java.io.BufferedWriter.
                    (java.io.FileWriter. file))]
    (let [lines (if active
                  (assoc (:lines bugs) (:number active) (write-bug active))
                  (:lines bugs))]
      (doseq [line lines] (.write wtr (str line "\n"))) 
      (when active
        (.write wtr (format
                      "\n\n# Working on \"%s\" in \"%s\" since %d"
                      (:description active)
                      (:project active) 
                      (:since active)))))))

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

(defn main []
  (let [icon (load-icon "resources/clock.png")
        menu (create-menu)]
    (.addTrayIcon (get-tray) menu 0)
    (.setIcon menu icon)
    (.setActionListener
      menu
      (action #(let [bugs (load-bugs "tracker.txt")]
                 (update-items menu bugs
                               (fn [active] (write-bugs "tracker.txt" bugs active)) 
                               (fn [] (write-bugs "tracker.txt" bugs nil))))))))

