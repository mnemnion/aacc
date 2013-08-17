# Introduction to the Martian Stack: What it Is, Why You Should Care

The first question is easy to answer: the Martian Stack is my nickname for [Urbit](http://moronlab.blogspot.com/) and friends, which was, for awhile there, the only English on the subject. Typical of the author, he opened up a [github](https://github.com/cgyarvin), without further comment or explanation. He's busy, you see; let's not bother him. 

There's apparently a list somewhere, and I'm not on it. By rumour, he's not exactly being forthcoming on the list either. 

This level of firm isolation from the body public is unusual in an open source effort. The entire thing is cryptic, if rich in metaphor. One is reminded of early mathematics, wherein the true significance of a finding was often deeply buried, the idea being to get later credit for the discovery without arming your competitors with the knowledge gained. 

In this foray, I will have little to say about Urbit, proper. Understanding Urbit requires understanding Hoon, and I do not. To understand Hoon, you must first understand Nock, which I do, somewhat. That's why I'm writing this, actually, because if I can explain Nock, I understand it well enough to make progress with Hoon. 

## Nock

First things first: @cgyarvin is a raconteur of rare erudition. His prose, which can be as lapidary as a small Lebanese pillbox, often contains contents at least as mind-expanding. His [Nock documentation](https://github.com/cgyarvin/urbit/blob/master/doc/book/1-nock.markdown) is the canon on the subject; all mistakes in this post are emphatically mine. 

Next: read the whole thing. Don't feel as though you have to understand it, but give each paragraph a good-faith go. 

Ok. You're back. You may or may not have any idea what you just read. If you're getting the uneasy feeling that you simply don't care, give me another five paragraphs before you bail. After that it's a-ok, this stuff is not for most. 

cgyarvin, our auteur, compares and contrasts Nock to the JVM. I feel that this is a deliberate error, if you will, and a trap for the unwary. There are a number of such in the Martian Stack, and each serves a purpose. 

Nock is an executable, canonical specification language for code. It is among the simplest possible such. It is a **functional** equivalent of **imperative** structures such as the JVM, but that distinction is fundamental and cannot be bridged. 

Let's break that claim down. First, canonical: Any statement written in Nock will produce a result, and it will be the same result, in all cases and situations. Nock is straightforward mathematics, and is at 5K, meaning only five changes can ever be made to the specification. Even in the event that Nock moves (and I will in fact argue for a Nock 4K), Nock 5K is a pure state machine and the decideable results of Nock 5K remain decideable with mathematical certainty. 

Next: Executable. Nock defines transformations on its own structure, meaning it can be repeatedly reduced to a form that either crashes (forms an infinite loop) or cannot be reduced further. Those transformations must be executed: like any number, Nock just sits there if you do nothing to it. Naive Nock runs slowly, but naive Nock is for mathematics and deep systems programming, not execution. 

Specification language: Nock is only useful if you have a virtual machine that actually runs it. cgyarvin calls this jet propulsion, and I'll get into my understanding of how this works in more depth as we go. The idea is that Nock is always right, and if the Nock Jet Engine produces different results, it is wrong and must be corrected. Since Nock is both straightforward and executable, any subsection of the Nock code may be checked for its result at any time.

That's five paragraphs. If you don't want portable code that is specified exactly and mathematically and will give the same result in 1000 years, no matter what happens, you're in the wrong place. Cheers for visiting!

##Why Nock

Code rots. This is a weird and ultimately unacceptable thing for mathematics to do, and computation is mathematical no matter how many times you try to paint it with a different brush. 

There is an important exception: if you are Donald Knuth, your code does not rot. That is because, before Donald Knuth writes code, he first specifies completely the machine and language he will be using, implements it on existing hardware, then presents his results. They are then frozen into bug only, asymtotic, irrational versioning, which is certainly one way to do it. 

Martians, given TAoCP, would be able to reconstruct the contents into executable code. Given TeX, the specs of at least one popular Earth processor, and some support tools that really shouldn't be necessary, they could do the same for TeX. 

Ordinary humans, given ordinary code from ten years ago, find it monumentally difficult in many cases to get it to run. Where the Internet is concerned, you can basically forget it: a page from six years ago will not, in general, contain working links, and if it does, the contents are by no means to be considered the same contents. 

The Martian Stack is an attempt to solve this problem for everything: formats, languages, applications, and network contents. All of it functional, all of it frozen. The contents may disappear, entropy is a stern master, but you will either retrieve correctly or you will not. 

I can't tell if it will succeed. Nock is the right kind of approach. 

##Structure of Nock

This is straight from the Nock 5K spec:

```
A noun is an atom or a cell.  An atom is any natural number.
A cell is any ordered pair of nouns.
```

and 

```
:: [a b c]    [a [b c]]
```

Meaning that brackets associate to the right so `[a b c d]` becomes `[a [b [c d]]]`.

As an implementation detail, note that this means that, in order to have common structures such as `[[a b]] [c d]]`, one must either have a separator character or a bit in the binary representation the shows whether an atom is part of a pair or not. The latter is clearly prefereble. We may say that an atom that is not a part of a cell is a noble atom. The first member of a cell is the carion, and the second member, just as clearly, the cdrion. I will brook no argument in these assessments. Noble atoms may not be ionic, and all is right in God's Kingdom.

I will not burden you with these terms, however: Nock convention is to call the head of a cell the subject, the tail the formula, and the result of Nocking the cell is the product. 

A noble atom is easy to interpret in Nock: it is unstructured data, such as one might send if two parties understand the format of the payload. Nock, helpfully, goes into an infinite loop if called on a noble atom. This is a formality which needn't be acted out in practice. 

All the action in Nock happens on cells, which are reduced through automatic rules, that together make up the Nock spec. 

`.*([42 42] [5 [0 3] [0 2]])`

`.*(42 [5 [1 3] [1 2]])`




