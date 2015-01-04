I like what μPickle does; I want it to work very differently.

This is my version.

Changes
=======

* Scala 2.11.4 only.
* Jawn 0.7.2.
* Fuck macros off.
* Fuck nulls off.
* Objects with `final` everywhere instead of traits.
* Nothing mixed into package object.
* Split implicits into `{Base,Stdlib,Tuple}Codecs`.
