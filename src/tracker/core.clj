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

(defn update-items [bug-id menu items]
  (doseq [index (range (.getItemCount menu))] (.removeItem menu 0)) 
  (if bug-id
    (do
      (.addItem menu "[00:16] - stop working" (action #(update-items nil menu items))) 
      (.addSeparator menu))) 
  (.addItem menu "Priority 1" nil) 
  (.addSeparator menu) 
  (doseq [item items] (.addItem
                        menu
                        (if (= bug-id (:number item))
                          ;(str "◆ " (:description %))
                          ;(str "✦ " (:description %))
                          ;(str "● " (:description %))
                          (str "➡ " (:description item))
                          (:description item)) 
                        (action #(update-items (:number item) menu items)))))

; IO -----------------------------------------------------------------------

(defn parse-bug [bugs [number line]]
  (if (re-matches #"\S.*" line)
    (cons 
      (into {:number number}
            (zipmap
              [:priority :project :description]
              (string/split line #"\s+" 3)))
      bugs)
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
      (doall
        {:lines lines
         :bugs (vec (reduce parse-bug [] (zipmap (range (count lines)) lines)))}))))

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
        icon (load-icon "resources/clock.png")
        menu (create-menu)]
    (.addTrayIcon (get-tray) menu 0)
    (.setIcon menu icon)
    (watch-file "tracker.txt" 1000
                #(do
                   (reset! bugs (load-bugs "tracker.txt"))
                   (update-items nil menu (:bugs @bugs))))))

