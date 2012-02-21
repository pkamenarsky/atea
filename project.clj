(defproject atea "1.0.2"
            :description "Minimalistic time tracker for MacOS"
            :dependencies [[org.clojure/clojure "1.3.0"]]
            :native-dependencies [[org.clojars.pka/jdic-macos-tray "0.0.2"]]
            :dev-dependencies
            [[native-deps "1.0.5"]
             [vimclojure/server "2.3.1" :exclusions [org.clojure/clojure]]]
            :main tracker.core)
