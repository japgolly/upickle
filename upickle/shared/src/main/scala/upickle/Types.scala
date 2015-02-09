package upickle

import scala.{PartialFunction => PF}
import scala.annotation.implicitNotFound

/** Serialize a type [[T]] to JSON, and eventually to a string. */
@implicitNotFound("uPickle does not know how to write [${T}]s; define an implicit Writer[${T}] to teach it how")
trait Writer[T] {
  def write: T => Js.Value
}

object Writer {
  @inline final def apply[T](_write: T => Js.Value): Writer[T] =
    new Writer[T] { def write = _write }
}

/** Deserialize a type [[T]] from JSON, which can itself be read from a String. */
@implicitNotFound("uPickle does not know how to read [${T}]s; define an implicit Reader[${T}] to teach it how")
trait Reader[T] {
  def read: PF[Js.Value, T]
}

object Reader {
  @inline final def apply[T](_read: PF[Js.Value, T]): Reader[T] =
    new Reader[T] { def read = _read }
}

object ReadWriter {
  @inline final def apply[T](_write: T => Js.Value, _read: PF[Js.Value, T]): Writer[T] with Reader[T] =
    new Writer[T] with Reader[T] {
      def read = _read
      def write = _write
    }
}

/** Handy shorthands for Reader and Writer */
private[upickle] object Aliases {
  type R[T] = Reader[T]
  @inline final val R = Reader

  type W[T] = Writer[T]
  @inline final val W = Writer

  type RW[T] = R[T] with W[T]
  @inline final val RW = ReadWriter
}

/**
 * Basic functionality to be able to read and write objects. Kept as a trait so
 * other internal files can use it, while also mixing it into the `upickle`
 * package to form the public API
 */
object Fns {

  /** Serialize an object of type [[T]] to a `String` */
  @inline final def write[T: Writer](expr: T): String = json.write(writeJs(expr))

  /** Serialize an object of type [[T]] to a `Js.Value` */
  @inline final def writeJs[T: Writer](expr: T): Js.Value = implicitly[Writer[T]].write(expr)

  /** Deserialize a `String` object of type [[T]] */
  @inline final def read[T: Reader](expr: String): T = readJs[T](json.read(expr))

  /** Deserialize a `Js.Value` object of type [[T]] */
  @inline final def readJs[T: Reader](expr: Js.Value): T = implicitly[Reader[T]].read(expr)
}