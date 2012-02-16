(defproject tracker "1.0.0-SNAPSHOT"
            :description "Minimalistic MacOs time tracker"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [org.eclipse/swt-cocoa-macosx-x86_64 "3.5.2"]]
            :dev-dependencies [[vimclojure/server "2.3.1" :exclusions [org.clojure/clojure]]]
            :main hs.base.core)
