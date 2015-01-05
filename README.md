I like what μPickle does; I need it to do it differently.  This is my version.

This is meant for internal protocols.
For public protocols including interop with external services, you will hate this;
use [@lihaoyi's original](https://github.com/lihaoyi/upickle).

Changes
=======

* Scala 2.11.4 only.
* Jawn 0.7.2.
* Fuck macros off.
* Fuck nulls off.
* Objects instead of traits.
* Nothing mixed into package object. ***No implicits by default.***
* Split implicits into `{Base,Stdlib,Tuple}Codecs`.
