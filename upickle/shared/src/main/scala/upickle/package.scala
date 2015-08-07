import acyclic.file

package object upickle {
  type ReadWriter[T] = Reader[T] with Writer[T]

  @inline private[upickle] final def validate[T](name: String)(pf: PartialFunction[Js.Value, T]): PartialFunction[Js.Value, T] =
    pf.orElse { case x => throw Invalid.Data(x, name) }

  @inline implicit class MPickleReadWriterCoreExt[A](private val rw: ReadWriter[A]) extends AnyVal {
    @inline def xmap[B](f: A => B)(g: B => A): ReadWriter[B] =
      ReadWriter.xmap(f)(g)(rw, rw)
  }
}