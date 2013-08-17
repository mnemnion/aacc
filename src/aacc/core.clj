(ns aacc.core
  (:use     [clojure.repl]
   :use [monger.collection :only [insert insert-batch find-one-as-map find-maps]])
  (:require [monger.core :as mg]
            [clojure.inspector :as cljin]
            [net.cgrand.enlive-html :as html]
            [rhizome.viz :refer [view-tree]]
            [me.raynes.conch :refer [programs with-programs let-programs]]
            [clojure.string :refer [split upper-case]]
            [instaparse.core :refer [parser parse parses transform]]
                                    )
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern]
           [java.net.URL]))()

#_(defn fire-up-mongo []
  (do (programs mongod)
      (mongod)
      (println "I just made a mongo")
      (mg/connect!)
      (mg/set-db! (mg/get-db "mongo-test"))
      (find-one-as-map "documents" {:first_name "John"})))
 
 (defn make-alternates [foo] 
   (str "(" "'" (upper-case foo ) "'" "|" "'" foo "'" ")"))
 
 (defn caseify [token]
   "produce a case insensitive token for ANTLR"
   (println (apply str 
                   (map make-alternates 
                        (rest (split token #""))))))
 
 (def json-parse
      "instaparser for JSON"
      (parser (slurp "json.grammar") :output-format :enlive))
              
(def make-tree "simple tree parser" (parser "tree: node* node: leaf | <'('> node (<'('> node <')'>)* node* <')'> leaf: #'a+' "))


(def make-tree-enlive
     "simple tree parser"
     (parser "tree: node* 
              node: leaf | '(' node (<'('> node <')'>)* node* ')' 
              leaf: #'a+'
              " :output-format :enlive))
              
              
(def a-tree (make-tree "a(aaa)a" ))
(def e-tree (make-tree-enlive "a(aaa)a" ))
                              
(def a-longer-e-tree (make-tree-enlive "aaaaa(aaaa(aaa(aaa)aa)aa)aaaa"))
              
(defn tree-viz 
    "visualize instaparse hiccup output as a rhizome graph"
    [mytree]
    (view-tree sequential? rest mytree 
               :node->descriptor (fn [n] {:label (if (coll? n) 
                                                     (first n) 
                                                     (when (string? n) n ))})))

(defn enlive-seq
  [tree]
  (if (map? tree)          
      (:content tree)
      (first tree)))

(defn- kids? [node]
  (not (= nil (:content node))))

(defn e-tree-viz
  "visualize enlive trees"
  [mytree]
  (view-tree kids? enlive-seq mytree 
             :node->descriptor (fn [n] 
                                 {:label (if (string? n)
                                             n
                                             (:tag n))})))
             
#_(tree-viz a-tree)


(defn a-func
  "a rule handler"
	[rule cdr state x]
	(let [local-state state] 
	  (println x)
	  local-state))

(def x 12)

(defn plusfive
  "plus 5"
  [y]
  (let [x x]  
  (+ 5 x y)))
     
(defn flatten
  "make an Instaparse graph flat again"
  [tree]
  )
     
   


