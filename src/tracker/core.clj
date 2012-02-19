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

(defn key-task [task]
  (str (:project task) (:description task)))

(defn parition-items [items]
  ; first group by priority into a {pri item} map,
  ; then group the first 10 items of every priority by project
  (reduce (fn [a [pri item]]
            (assoc a pri (group-by :project (take 10 item))))
          {}
          (group-by :priority items)))

(defn update-items [menu items active actfn deactfn]
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
              (if (= (key-task item) (key-task active))
                (str "➡ " (:description item))  ;◆✦●
                (:description item)) 
              (action #(actfn (assoc item :since (now)))))))

        add-priority
        (fn [add-sep? priority prjs]
          (add-section
            add-sep?
            (str "Priority " (inc priority) " - " (key (first prjs)))
            (val (first prjs)))
          (doseq [[prj items] (next prjs)] (add-section true prj items)))]

    ; remove old items
    (doseq [index (range (.getItemCount menu))] (.removeItem menu 0)) 

    ; add "now working" section
    (when active
      (let [stime (to-mins (- (now) (:since active)))]
        (.addItem
          menu
          (str "Session: "
               (to-str stime)
               " - Sum: "
               (to-str (+ (:time active) stime)))
          nil) 
        (.addSeparator menu)
        (.addItem menu "Stop work" (action #(deactfn)))
        (.addSeparator menu))) 

    ; add items sorted by priority and project
    (let [part-items (sort (parition-items items))]
      (when (first part-items)
        (add-priority false (key (first part-items)) (val (first part-items)))) 
      (doseq [[pri prjs] (next part-items)] (add-priority true pri prjs)))))

; IO -----------------------------------------------------------------------

; tracked tasks
(defn parse-status [line]
  (let [match (re-matches #"# Working on \"(.*)\" in \"(.*)\" since (\d*) for (\d*)" line)]
    (when match
      {:description (match 1)
       :project (match 2)
       :since (Long. (match 3))
       :time (Long. (match 4))})))

(defn parse-ttask [line]
  (let [match (re-matches #"(\d*) (\d*) \[(.*)\] (.*)" line)]
    (when match
      {:priority (Long. (match 1)) 
       :time (Long. (match 2)) 
       :project (match 3)
       :description (match 4)})))

(defn load-ttasks [file]
  (try 
    (let [lines (string/split-lines (slurp file))
          status (parse-status (first lines))]
      (if status
        {:active status
         :ttasks (map parse-ttask (next lines))}
        {:active nil
         :ttasks (map parse-ttask lines)}))
    (catch java.io.FileNotFoundException e
      {:active nil
       :ttasks []})))

; tasks
(defn parse-task [line]
  (let [match (re-matches #"\s*\[(.*)\]\s*(.*)" line)]
    (if match
      {:project (match 1) :description (match 2)}
      {:project "Default" :description (string/trim line)})))

(defn load-tasks [file]
  (try 
    (let [lines (string/split-lines (slurp file))
          pris (filter #(not (empty? (first %))) (partition-by empty? lines))
          tasks (zipmap (range (count pris)) pris)]

      ; flatten into maps
      (for [[pri items] tasks
            task items] (into (parse-task task) {:priority pri :time 0}))) 
    (catch java.io.FileNotFoundException e [])))

(defn key-tasks [tasks]
  (zipmap (map key-task tasks) tasks))

(defn write-status [active]
  (str "# Working on \"" (:description active) "\" in \"" (:project active) "\" since " (:since active) " for " (:time active)))

(defn write-ttask [ttask]
  (apply format "%d %d [%s] %s" (map ttask [:priority :time :project :description])))

(defn write-ttasks [file tasks ttasks new-active]
  (try
    (let [active (:active ttasks)
          kts (if active
                (update-in (key-tasks (:ttasks ttasks))
                           [(key-task active) :time]
                           #(+ % (to-mins (- (now) (:since active)))))
                (key-tasks (:ttasks ttasks)))

          ; merge textfile tasks and tracked tasks
          tmerged (vals (merge-with (fn [t tt] {:priority (:priority t) 
                                                :project (:project t)
                                                :description (:description t)
                                                :time (:time tt)})
                                    (key-tasks tasks)
                                    kts))
          
          ; get active time
          tactive (if new-active 
                    (assoc new-active :time (get-in kts [(key-task new-active) :time] 0))
                    nil)

          ; write lines
          lines (map write-ttask tmerged)
          content (string/join "\n" (if tactive
                                      (cons (write-status tactive) lines)
                                      lines))]

      (spit file content)) 
    (catch java.io.FileNotFoundException e nil)))

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

(defn ttname [tname]
  (let [match (re-matches #"(.*)\.(.*)" tname)]
    (if match
      (str (match 1) "-times." (match 2))
      (str tname "-times"))))

(defn main []
  (let [old-file (atom nil)
        icon (load-icon "resources/clock.png")
        menu (create-menu)]
    (.addTrayIcon (get-tray) menu 0)
    (.setIcon menu icon)
    (.setActionListener
      menu
      (action #(let [file (:file (load-cfg))
                     tfile (ttname file)
                     tasks (load-tasks file)
                     ttasks (load-ttasks tfile)]

                 ; if file *name* changed, write out old one first
                 (when (and @old-file (not= @old-file file))
                   ; we presume this is gonna work since it worked last time :)
                   (write-ttasks (ttname @old-file)
                                 (load-tasks @old-file)
                                 (load-ttasks (ttname @old-file))
                                 nil))

                 ; update menu
                 (when tasks
                   (reset! old-file file) 
                   (update-items menu tasks (:active ttasks)
                                 (fn [new-active] (write-ttasks tfile tasks ttasks new-active)) 
                                 (fn [] (write-ttasks tfile tasks ttasks nil)))))))))

