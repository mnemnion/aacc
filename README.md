# aacc

Actually A Compiler Compiler. A backend to Instaparse that lets you do arbitrary things with a parse tree. 

yacc, and instaparse, are not compiler compilers. yacc is a parser compiler, and hence a parser parser; instaparse is in that family, but Clojurian, and delectably lexer free.

aacc extends that flexibility, allowing a single-pass, rule-driven walk through a parse tree.

aacc is also a bit of a pun:

```clojure
{:a a :c c}
```

##Usage

aacc takes a map of keywords to functions defined using the `def-rule-fn` macro. The keywords correspond to instaparse rule names.  Optionally, a map of literal tokens to rules may also be provided. 

aacc is called like this:

```clojure
(aacc state tree)
;or
(aacc state tree rule-map)
;or
(aacc state tree rule-map token-map)
```

`state` is a map, which is initialized with the rule map as the value of a `:rule-map` key. `:token-map` may be added to `state` as well; the 3 and 4 argument forms push the maps into state before beginning the seq. Failure to provide a rule-map results in a null pointer exception; it's that or heat up your computer. 

`state` is returned by aacc at the end of the walk. 

The variable order allows a convenient definition for a compiler function:

```clojure
(def compiler (partial aacc {:rule-map rule-map}))
(compiler some-instaparse-tree)
```

Which, when called on a tree, compiles it. Whether this is true compilation or interpretation depends on whether the focus is on side effects or on the contents of state, which could have, for example, an `:executable-binary` keyword, a `:clojure-program`, or anything else. 

The variable order also means that, to start aacc with a clean slate, you must provide an empty `state` explicitly:

```clojure
(aacc {} tree rule-map token-map)
```

As all four parameters are hash maps, aacc will respond to forgetting the state by silently returning the `tree` as `state`. 

Rules are defined like so:

```clojure
(def foo-rule 
  (aacc/def-rule-fn
  (println "I'm in the foo rule. I can access " (str rule-key) " among other things.")
  state)))

; macroexpand-1 to:

(fn 
 [rule-key state seq-tree] 
 (println "I'm in the foo rule. I can access " (str rule-key) " among other things.") 
                                                                               state)
; then put it in a map:

(def rule-map {:foo foo-rule})

```

##Behavior

aacc `recur`sively walks the parse tree, repeatedly calling the rule functions. Rule functions defined with `def-rule-fn` have convenient access to three magic variables: `state`, `rule-key`, and `seq-tree`. The `rule-key` is the keyword or token which called the rule, `seq-tree` is a sequence of the remaining tree to be walked, and `state` is returned by every rule.

```clojure
(first (rest seq-tree) 
```
will give you the next node on the tree, as expected. 

All rule functions are expected to return `state`, in a useful fashion.

`state` contains everything but the `seq-tree`, which ensures that aacc will exit unless a subrule contains an infinite loop. This means the value of `rule-map` and `token-map` may be dynamically changed by modifying the bindings of `:rule-map` or `:token-map` within the state map, with the changes reflected in the next iteration. 

aacc will exit immediately if the returned state map contains a value for the keyword `:stop`. `:error` is probably a good place to put things that go wrong, and `:warning` might be a nice location for warnings. 

`:stop`, `:root-tree`, `:rule-map`, and `:token-map` are the only magic values in `state`. `:pause`, `:aacc-error` and `:crash-only` are reserved, but not used. `:root-tree` contains the `tree` argument from the aacc call, not the seq, the original tree. Modifying the value of `:root-tree` will not change the underlying tree-seq, which is baked in at run time and will walk the entire tree exactly once.

There are no magic keywords in the `rule-map` or `token-map`. These are the namespace of the language you're parsing, and it is hygenic: anything instaparse will accept as a rule name or literal token may be specified. 

In many cases, a `token-map` is not necessary. If a grammar is designed so that all literal tokens are contained by a single rule, then `(frest seq-tree)` will always deliver that token from that node. This is good practice in many cases, and the instaparse documentation provides a formula for compacting your trees in this fashion. aacc will run faster if you do not provide a `token-map`, because it will not look for a rule or execute `default-token-rule`. This also means that if an initial `token-map` is not provided, adding `:token-map` to `state` will not cause the resulting rules to be executed. If you want to dynamically load token rules as you find them, starting from nothing, provide an empty `token-map` when calling aacc.

If the rule map does not contain a particular keyword, the default rule, `aacc/default-rule`, is used. It returns state, doing nothing further. Literal tokens that are not matched by the token map call `aacc/default-token-rule`. Both of these may be overridden if necessary, in the following fashion:

```clojure
(alter-var-root #'instaparse.aacc/default-rule (constantly new-rule))
```
Where new-rule should be created with the `def-rule-fn` macro or provide the same magic variables. When `default-rule` or `default-token-rule` is called, it is passed the `:rule` keyword or literal string that calls it, just like a specified rule. That is to say, the value of `rule-key` in a rule function depends on the instaparse graph, not on the rule nor token maps.

This allows rule functions to be generic, so that many types can be handled by a single rule. This is the only way to collect all literal tokens, which can be generated by regular expressions and are practically infinite in extent. Rules may also be threaded with the `call-rule` macro, which does what you'd expect:

```clojure
(call-rule foo-rule) ; must be in a rule or otherwise provide [rule-key state seq-tree] bindings
```

Threading from tail position will give the most predictable behavior. 

Note that aacc either recurs from tail position or returns state, and doesn't care what the rule expressed at the root node is. That means it may be called, recursively, on any node encountered during the walk, or on any tree generated by aacc rules, or passed into `state`. This will *not* consume the original tree-seq or modify it in any fashion: the original tree provided to any call of aacc is frozen and will be depth-first walked exactly once before returning state. 

This allows you to do interesting things like embed a tree and rule-map from another language, call aacc using that language, then return to parsing the first language. `state` is always threaded, presuming your rules are properly written.

A useful way to do this is to define a compiler as above, and add it to state. This closes over the rule-map, making it unavailable for accidental modification until you enter the new compiler. Like so:

```clojure
(def rule-map {:json json-rule})
(def json-compiler (partial aacc {:rule-map json-rules}))
(def meta-compiler (partial aacc {:rule-map rule-map :json-compiler json-compiler)
```

Then, when you hit a `:json` tag, the `json-rule` can call `json-compiler` on the child node, which presumably contains JSON. Note that this *does* *not* consume the child node in the `meta-compiler`, which must be handled separately after `json-compiler` returns `state`. 

You can also pack state with a json tree and call `json-compiler` on that tree at any point. The rule is that tree-seq is immutable, you will visit every rule and token exactly once and the only way out is to add `:stop` to the state. 

This also means you can trivially send aacc into recursive descent hell by calling `(aacc state :root-tree)` from within a rule. Please don't do that. aacc, left to its own devices, will exit, given a data structure of finite size, which is currently the only input option. 

##Future

aacc is meant to be performant, suitable for processing, for example, hundreds of thousands of json objects. There's not much in the main loop, and it will stay that way.

aacc will already run faster if you don't provide a literal token map, which is unnecessary for many purposes as all literal tokens are available in the seq and can be utilized from the grammar rules. 

Similarly, `:stop` may not be necessary if you want a crash-only compiler, and it would be nice to expose a faster, unsafe aacc that doesn't stop and check for `:stop`. I haven't done this, but if I do, you will be able to pass `{:crash-only true}` to the initial aacc state and aacc will leave it in there so your rules can check which environment they're running in. 

There is one command that is common and should be supported out of the loop. `{:drop n}`, added to `state`, would cause aacc to drop n tokens without calling any rules on them. If I add this, you'll have to activate it with a `{:drop true}` in `state`.

At the moment, aacc only supports the enlive output format. This is because it's easy to work with and conceivably other key-value pairs could be profitably added to the tree before aacc does its thing. aacc only uses the `:tag` and `:content` keys, because that's all instaparse outputs, but additional key-value pairs in the enlive graph should not cause problems (this is worth verifying).

I may want to add a set of standard utilities; these would be fragments of macro-defined code that assume they're inside an environment where the `->` macro has been called on `state`. That way a simple `aac.util/count` would expand into `(assoc ,,, :count (inc (:count state)))` where the commas are the expansion point for the `->` macro. `->` would have to explicitly be called; the expected structure of a rule is possibly a conditional or so, followed by one or more actions on `state`. If it's more than one, thread. 

The hiccup output has the advantage that any vector within it is valid Clojure code. Enlive embeds literals strings in lists, which can be at the first position, causing an error; since hiccup uses vectors, anything can be at the first position. It would be good to support both formats, so that rules can be trivially called on any subsection of a tree, for diagnostic purposes. 

The difficulty with supporting multiple output formats is that the rules will work differently on different data structures. Unless I can find a way around this, I will stick with enlive for now. Supporting the experimental `:lisp` format would also be useful, as it has the fastest seq of any of the options. It shares the enlive disadvantage that unquoted lists will in general fail if passed as naked values.  

This may not turn out to be a problem in practice. All three formats are simply trees, containing nodes which are keywords and optional leaves which are literal strings. The tree-seq wrapper means that the seq will return the same values; it's only if you manipulate the root tree directly that aacc will work differently, since `state` is a map in all cases. 

In particular, `:hiccup` and `:lisp` should behave identically, as there is no way to embed additional tags into the tree. This should support a workflow where aacc compilers are developed in `:hiccup` to make parsing substrings from the REPL trivial, switching to `:lisp` format in production for the speed advantages on a head-first seq.

It would be quite nice to add a magic word `:pause` to the state machine, that would return control to the REPL and allow re-entry at the point of departure. I'm honestly unsure where to begin with something like this, and it shouldn't be part of the basic state machine, as in many contexts it would slow things down to no purpose. 

## TO DO


add error correction that causes the lack of a rule-map to cause aacc to push `:aacc-error true` into `state` and exit. Update documentation accordingly.


## License

Copyright © 2013 Sam Putman.

Distributed under the BSD 2 Clause License. 
