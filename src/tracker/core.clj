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

(defn update-items [bug-id menu items actfn deactfn]
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
              (if (= bug-id (:number item))
                (str "➡ " (:description item))  ;◆✦●
                (:description item)) 
              (action #(do
                         (actfn (assoc item :since (.getTime (java.util.Date.))))
                         (update-items (:number item) menu items actfn deactfn))))))

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
    (if bug-id
      (do
        (.addItem
          menu
          "[00:16] - stop working"
          (action #(do
                     (deactfn)
                     (update-items nil menu items actfn deactfn)))) 
        (.addSeparator menu))) 

    ; add items sorted by priority and project
    (let [part-items (sort (parition-items items))]
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

(defn write-bug [bug]
  (apply format "%s\t%s\t%s\t%s" (map bug [:priority :project :time :description])))

(defn load-bugs [file]
  (with-open [rdr (java.io.BufferedReader. 
                    (java.io.FileReader. file))]
    (let [lines (vec (line-seq rdr))]
      {:lines lines
       :bugs (reduce parse-bug [] (map vector (range (count lines)) lines))})))

(defn write-bugs [file bugs actbug]
  (with-open [wtr (java.io.BufferedWriter.
                    (java.io.FileWriter. file))]
    (doseq [line (:lines bugs)] (.write wtr (str line "\n")))
    (.write wtr (str "\n\n# Working on \"" (:description actbug) "\" in " (:project actbug) " since " (:since actbug)))))

(defn update-bug [bugs id k v]
  (let [bug (assoc (get-in bugs [:bugs id]) k v)]
    (println bug)
    (-> bugs
      (assoc-in [:bugs id] bug)
      (assoc-in [:lines (:number bug)] (write-bug bug)))))

(defn update-line [bugs bug]
  (assoc-in bugs [:lines (:number bug)] (write-bug bug)))

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
  (let [bugs (atom nil)
        active-item (atom nil)
        icon (load-icon "resources/clock.png")
        menu (create-menu)]
    (.addTrayIcon (get-tray) menu 0)
    (.setIcon menu icon)
    (.setActionListener
      menu
      (action #(do
                 (reset! bugs (load-bugs "tracker.txt"))
                 (update-items nil menu (:bugs @bugs)
                               (fn [actbug]
                                 (reset! active-item actbug)
                                 (swap! bugs update-line actbug)
                                 (write-bugs "tracker-out.txt" @bugs actbug)) 
                               (fn [])))))))

