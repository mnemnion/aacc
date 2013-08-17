(ns aacc.compiler
    (:require [aacc.core :as aacc]
     :require [swiss-arrows.core :refer :all]))


(defmacro def-rule-fn 
      "given the body of a function, defines the
      function as taking the args [rule-key state seq-tree]"    
          [& body]  
        `(fn [~'rule-key ~'state  ~'seq-tree]
           #_(println "Calling ..." ~'rule-key)    
           ~@body))
           
(defmacro call-rule 
        "calls a rule with the arguments [rule-key state seq-tree],
         which must be bound before the call. Intended to be called
         within the environment of a rule."  
          [rule-to-call]
          `(~rule-to-call ~'rule-key ~'state ~'seq-tree))
           
(defn- e-tree-seq 
  "tree-seqs enlive trees, at least instaparse ones"
  [e-tree]
  (tree-seq (comp seq :content) :content e-tree)) 
            
(def inc-state
     (def-rule-fn
     (assoc state :count (inc (:count state)))))
                 
(def ^:private test-rule
     (def-rule-fn
       (println "Keyword! " (str rule-key))
       (println "State! " (str (:count state)))
       #_(println "Contents! " (apply str (:content (first seq-tree))))
       #_(println "First of Seq-Tree!" (apply str (first seq-tree)))
       (println "Seq Tree! " (apply str seq-tree))
       (when (string? (first (rest seq-tree)))
             (println "we got a token coming up"))
       (if (= rule-key :tree)
           (assoc state :count 1)
           (call-rule inc-state))))  

(def ^:private threaded-rule
      (def-rule-fn
        (-<> (call-rule test-rule)
            (assoc :count (inc (:count (call-rule test-rule))))
            (assoc :foo "bar")
            (dissoc :bar))
            ))

(def ^:private test-token-rule 
     (def-rule-fn
       (println "Executing Literal Token Rule ")
       (println "Literal Token Contains \"" (str (first seq-tree)) "\"")
                                                 (call-rule inc-state)))

(def ^:private l-paren-rule
      (def-rule-fn
       (println "Executing L Paren Rule ")
       (println "Token Contains: " (str (first seq-tree)))
            (call-rule inc-state)))     
                       
(def ^:private single-a-rule test-token-rule)

(def ^:private test-rule-map
     {:tree test-rule
      :node test-rule
      :leaf test-rule})
      
(def ^:private test-token-map
      {"(" l-paren-rule
       ")" test-token-rule
       "a" single-a-rule
       })
       



(def default-rule
      (def-rule-fn
       (println "default rule reached")
        state))

(def default-token-rule
      (def-rule-fn
       (println "default token rule reached")
        state))

(def default-rule-map
     {:aac-default-rule default-rule})

(def default-token-map
     {:aac-default-token-rule default-token-rule})

; to replace default-rule, 
; or literal-token-rule, 
; (alter-var-root #'instaparse.aacc/default-rule (constantly new-rule))
 
(defn- retrieve-rule
  [seq-tree rule-map]
  (let [rule-fn ((:tag (first seq-tree)) rule-map)] 
       (if (not (false? rule-fn))
           rule-fn
           default-rule)))
           
(defn- retrieve-token-rule
  [rule-string rule-map]
  (let [rule-fn (rule-map rule-string)] 
       (if (fn? rule-fn)
           rule-fn
           default-token-rule)))
           
(defmacro ^:private recur-on-rule []
           `(recur ((~'retrieve-rule ~'seq-tree (:rule-map ~'state)) 
                       (:tag (first ~'seq-tree)) 
                       ~'state 
                       ~'seq-tree) 
                   (rest ~'seq-tree))) 
                          

                          
(defmacro ^:private recur-on-token []
           `(recur ((~'retrieve-token-rule (str (first ~'seq-tree)) (:token-rule-map ~'state)) 
                     (first ~'seq-tree) 
                     ~'state 
                     ~'seq-tree)
                   (rest ~'seq-tree))
)
                             
(defn aacc-looper
   "main aacc loop. Provides :stop functionality and a separate token rule map"
   [state seq-tree] 
   (if (:stop state)
       state
       (if (coll? (:content (first seq-tree)))
         (recur-on-rule) 
         (if (seq seq-tree)
           (recur-on-token)
           state))))

(defn aacc-looper-no-tok
   "main aacc loop, for programs with no literal token rules"
   [state seq-tree]
   (if (:stop state)
       state
       (if (coll? (:content (first seq-tree)))
         (recur-on-rule) 
         (if (seq seq-tree)
           (recur state (rest seq-tree))
           state))))

(defn aacc
  "actually a compiler compiler:
   
   tree is instaparse-enlive format \n
   state initializes the compiler \n
   if rule-map is not provided state 
   should contain :rule-map, or no behavior
   will result.
   
   returns state."
  ([state tree]
   (if (not (nil? (:token-rule-map state)))
       (aacc-looper (assoc state :root-tree tree ) (e-tree-seq tree))
       (aacc-looper-no-tok (assoc state :root-tree tree ) (e-tree-seq tree))))              
  ([state tree rule-map]
   (if (not (nil? (:token-rule-map state)))
       (aacc-looper (assoc state :rule-map rule-map :root-tree tree) (e-tree-seq tree))
       (aacc-looper-no-tok (assoc state :rule-map rule-map :root-tree tree) (e-tree-seq tree))))
  ([state tree rule-map token-map]
   (aacc-looper (assoc state :rule-map rule-map :root-tree tree :token-rule-map token-map) (e-tree-seq tree))))
                     
        