(ns catnip-project.core-test
  (:use clojure.test
        catnip-project.core))

(deftest parsertest
  (testing "my-parse"
    (is (= (my-parse "ab") [:S [:AB [:A "a"] [:B "b"]]]))))
