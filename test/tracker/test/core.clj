(ns tracker.test.core
  (:use [tracker.core])
  (:use [clojure.test]))

(deftest ttname-test
  (is (= ".tasks-times.csv" (ttname ".tasks")))
  (is (= "tasks-times.csv" (ttname "tasks.txt"))))
