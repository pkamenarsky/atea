(ns tracker.core
  (:require [clojure.string :as string]))

(defn load-icon [name]
  (let [is (.getResourceAsStream (ClassLoader/getSystemClassLoader) name)]
    (javax.imageio.ImageIO/read is)))

(defn get-systray []
  (java.awt.SystemTray/getSystemTray))

(defn create-menu []
  (java.awt.PopupMenu.))

(defn create-menu-item [desc enabled]
  (let [item (java.awt.MenuItem. desc)]
    (.setEnabled item enabled)
    item))

(defn update-items [menu items]
  (.removeAll menu)
  (.add menu (create-menu-item "Priority 1" false))
  (.addSeparator menu)
  (reduce #(do (.add % %2) %) menu (map #(java.awt.MenuItem. (:description %)) items)))

(defn create-tray-icon [menu]
  (let [icon (java.awt.TrayIcon. (load-icon "resources/clock.png") "" menu)]
    (.setImageAutoSize icon false)
    icon))

; --------------------------------------------------------------------------

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


