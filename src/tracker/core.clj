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

(defn update-items [bug-id menu items activate-fn]
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
                         (activate-fn item)
                         (update-items (:number item) menu items activate-fn))))))

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
          (action #(update-items nil menu items activate-fn))) 
        (.addSeparator menu))) 

    ; add items sorted by priority and project
    (let [part-items (sort (parition-items items))]
      (add-priority false (key (first part-items)) (val (first part-items)))
      (doseq [[pri prjs] (next part-items)] (add-priority true pri prjs)))))

; IO -----------------------------------------------------------------------

(defn maybe-int [string]
  (try
    (Integer. string)
    (catch Exception e 999)))

(defn parse-bug [bugs [number line]]
  (if (re-matches #"[^#\s].*" line) 
    (conj bugs 
          (into {:number number}
                (update-in 
                  (zipmap
                    [:priority :project :description]
                    (string/split line #"\s+" 3))
                  [:priority] maybe-int)))   ; convert :priority to int
    bugs))

(defn write-bug [bug]
  (apply
    str
    (interpose "\t\t" (reduce
                        #(conj % (get bug %2))
                        []
                        [:priority :project :description]))))

(defn load-bugs [file]
  (with-open [rdr (java.io.BufferedReader. 
                    (java.io.FileReader. file))]
    (let [lines (vec (line-seq rdr))]
      {:lines lines
       :bugs (reduce parse-bug [] (map vector (range (count lines)) lines))})))

(defn write-bugs [file bugs]
  (with-open [wtr (java.io.BufferedWriter.
                    (java.io.FileWriter. file))]
    (doseq [line (:lines bugs)] (.write	wtr (str line "\n")))))

(defn update-bug [bugs id k v]
  (let [bug (assoc (get-in bugs [:bugs id]) k v)]
    (println bug)
    (-> bugs
      (assoc-in [:bugs id] bug)
      (assoc-in [:lines (:number bug)] (write-bug bug)))))

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
    (watch-file
      "tracker.txt"
      1000
      #(do
         (reset! bugs (load-bugs "tracker.txt"))
         (update-items nil menu (:bugs @bugs) (partial reset! active-item))))))

