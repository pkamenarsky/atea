(defproject tracker "1.0.0-SNAPSHOT"
            :description "Minimalistic MacOs time tracker"
            :dependencies [[org.clojure/clojure "1.3.0"]]
			:native-dependencies [[org.clojars.pka/jdic-macos-tray "0.0.1"]]
            :dev-dependencies
				[[native-deps "1.0.5"]
				[vimclojure/server "2.3.1" :exclusions [org.clojure/clojure]]]
            :main hs.base.core)
