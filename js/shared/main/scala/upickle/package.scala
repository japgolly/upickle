import acyclic.file

package object upickle {
  type ReadWriter[T] = Reader[T] with Writer[T]
}