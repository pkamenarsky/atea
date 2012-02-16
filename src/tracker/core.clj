(ns tracker.core
  (:require [clojure.string :as string])
  (:import (org.eclipse.swt.widgets Display Shell)
           (org.eclipse.swt.layout GridLayout)
           (org.eclipse.swt.events ShellAdapter)))

(defn load-icon [name]
  (let [is (.getResourceAsStream (ClassLoader/getSystemClassLoader) name)]
    (javax.imageio.ImageIO/read is)))

(defn get-systray []
  (java.awt.SystemTray/getSystemTray))

(defn create-menu []
  (let [menu (java.awt.PopupMenu.)]
    (println "enabled " (.isEnabled menu))
    (.addActionListener menu (reify java.awt.event.ActionListener 
                               (actionPerformed 
                                 [this event] (println "ACTION"))))
    menu))

(defn create-menu-item [desc enabled f]
  (let [listener (reify java.awt.event.ActionListener 
                   (actionPerformed 
                     [this event] (f))) 
        item (java.awt.MenuItem. desc)]
    (.addActionListener item listener)
    (.setEnabled item enabled)
    item))

(defn update-items [bug-id menu items]
  (.removeAll menu)
  (if bug-id
    (do
      (.add menu (create-menu-item "[00:16] - stop working" true #(update-items nil menu items))) 
      (.addSeparator menu))) 
  (.add menu (create-menu-item "Priority 1" false nil))
  (.addSeparator menu)
  (reduce #(do (.add % %2) %) menu (map #(create-menu-item
                                           (if (= bug-id (:number %))
                                             ;(str "◆ " (:description %))
                                             ;(str "✦ " (:description %))
                                             ;(str "● " (:description %))
                                             (str "➡ " (:description %))
                                             (:description %)) 
                                           true
                                          (fn [] (update-items (:number %) menu items))) items)))

(defn create-tray-icon [menu icon]
  (let [icon (java.awt.TrayIcon. (load-icon icon) "" menu)]
    (.setImageAutoSize icon true)
    (.addActionListener icon (reify java.awt.event.ActionListener 
                               (actionPerformed 
                                 [this event] (println "ACTION"))))
    (.addMouseListener icon (reify java.awt.event.MouseListener
                              (mousePressed [this event] (println "pressed " (.getSize icon)))
                              (mouseReleased [this event] (println "released"))
                              (mouseClicked [this event] (println "clicked")))) 
    icon))

(defn swt-thread [f]
  (.asyncExec (Display/getDefault) (reify java.lang.Runnable
                       (run [this] (f)))))


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
        menu (create-menu)
        icon (create-tray-icon menu "resources/clock.png")]
    (.add (get-systray) icon)
    (println "size " (.getSize icon))
    (watch-file "tracker.txt" 1000
                #(do
                   (printf "--- reloading " "tracker.txt")
                   (reset! bugs (load-bugs "tracker.txt"))
                   (update-items nil menu (:bugs @bugs))))))

; SWT ----------------------------------------------------------------------

(defn create-shell [display shell]
(let [layout (GridLayout.)]
 (doto shell
   (.setText "SWT Test")
   (.setLayout layout)
   (.addShellListener
    (proxy [ShellAdapter] []
      (shellClosed [evt]
        (System/exit 0)))))))

(defn swt-loop [display shell]
(loop []
 (if (.isDisposed shell)
   (.dispose display)
   (do
     (if (not (.readAndDispatch display))
       (.sleep display))
     (recur)))))

(defn begin []
 (let [display (Display.)
       shell (Shell. display)]
   (create-shell display shell)
   (.setSize shell 700 700)
   (.open shell)
   (swt-loop display shell)))

