(defproject atea "1.0.3"
            :description "Minimalistic time tracker for MacOS"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [org.clojars.pka/jdic-macos-tray "0.0.3"]
                           [org.clojars.pka/jdic-macos-tray-native-deps "0.0.3"]]
            :native-path "native"
            :dev-dependencies
            [[vimclojure/server "2.3.1" :exclusions [org.clojure/clojure]]]
            :main tracker.core)
