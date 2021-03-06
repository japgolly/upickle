package upickle

import scala.reflect.ClassTag
import scala.concurrent.duration.{FiniteDuration, Duration}
import acyclic.file
import TupleCodecs._
import Aliases._
import Fns._

object BaseCodecs {

//  implicit final val NothingR = R[Nothing]{case x => ???}
//  implicit final val NothingW = W[Nothing](x => ???)

  private[this] type JPF[T] = PartialFunction[Js.Value, T]
  private[this] final val booleanReaderFunc: JPF[Boolean] = validate("Boolean"){
    case Js.True => true
    case Js.False => false
  }
  implicit final val BooleanRW = RW[Boolean](
    if (_) Js.True else Js.False,
    booleanReaderFunc
  )
  implicit final val UnitRW = RW[Unit](
    _ => Js.Obj(),
    {case _ => ()}
  )

  private[this] def numericStringReaderFunc[T](func: String => T): JPF[T] = validate("Number"){
    case x: Js.Str => func(x.value)
  }
  private[this] def NumericStringReadWriter[T](func: String => T) = RW[T](
    x => Js.Str(x.toString),
    numericStringReaderFunc[T](func)
  )
  private[this] def numericReaderFunc[T: Numeric](func: Double => T, func2: String => T): JPF[T] = validate("Number"){
    case n @ Js.Num(x) => try{func(x) } catch {case e: NumberFormatException => throw Invalid.Data(n, "Number")}
    case s @ Js.Str(x) => try{func2(x) } catch {case e: NumberFormatException => throw Invalid.Data(s, "Number")}
  }

  private[this] def NumericReadWriter[T: Numeric](func: Double => T, func2: String => T): RW[T] = RW[T](
    {
      case x @ Double.PositiveInfinity => Js.Str(x.toString)
      case x @ Double.NegativeInfinity => Js.Str(x.toString)
      case x => Js.Num(implicitly[Numeric[T]].toDouble(x))
    },
    numericReaderFunc[T](func, func2)
  )
  private[this] final val stringReaderFunc: JPF[String] = validate("String"){
    case x: Js.Str => x.value
  }
  implicit final val StringRW = RW[String](Js.Str, stringReaderFunc)

  implicit final val CharRW = NumericStringReadWriter[Char](_(0))
  implicit final val ByteRW = NumericReadWriter(_.toByte, _.toByte)
  implicit final val ShortRW = NumericReadWriter(_.toShort, _.toShort)
  implicit final val IntRW = NumericReadWriter(_.toInt, _.toInt)
  implicit final val LongRW = NumericStringReadWriter[Long](_.toLong)
  implicit final val FloatRW = NumericReadWriter(_.toFloat, _.toFloat)
  implicit final val DoubleRW = NumericReadWriter(_.toDouble, _.toDouble)
}

object StdlibCodecs {
  import collection.generic.CanBuildFrom

  object All extends Durations with Eithers with Options with Arrays with Maps

  trait Seqs {
    implicit def SeqishR[T: R, V[_]]
    (implicit cbf: CanBuildFrom[Nothing, T, V[T]]): R[V[T]] = R[V[T]](
      validate("Array(n)"){case Js.Arr(x@_*) => x.map(readJs[T]).to[V]}
    )

    implicit def SeqishW[T: W, V[_] <: Iterable[_]]: W[V[T]] = W[V[T]]{
      (x: V[T]) => Js.Arr(x.iterator.asInstanceOf[Iterator[T]].map(writeJs(_)).toArray:_*)
    }

    protected def SeqLikeW[T: W, V[_]](g: V[T] => Option[Seq[T]]): W[V[T]] = W[V[T]](
      x => Js.Arr(g(x).get.map(x => writeJs(x)):_*)
    )
    protected def SeqLikeR[T: R, V[_]](f: Seq[T] => V[T]): R[V[T]] = R[V[T]](
      validate("Array(n)"){case Js.Arr(x@_*) => f(x.map(readJs[T]))}
    )
  }

  trait Maps extends Seqs {
    implicit def MapW[K: W, V: W] =  W[Map[K, V]](
      x => Js.Arr(x.toSeq.map(writeJs[(K, V)]):_*)
    )
    implicit def MapR[K: R, V: R] = R[Map[K, V]](
      validate("Array(n)"){case x: Js.Arr => x.value.map(readJs[(K, V)]).toMap}
    )
  }

  trait Options extends Seqs {
    implicit def OptionW[T: W]: W[Option[T]] = SeqLikeW[T, Option](x => Some(x.toSeq))
    implicit def SomeW[T: W] = W[Some[T]](OptionW[T].write)
    implicit def NoneW(implicit I: W[Int]): W[None.type] = W[None.type](OptionW[Int].write)
    implicit def OptionR[T: R]: R[Option[T]] = SeqLikeR[T, Option](_.headOption)
    implicit def SomeR[T: R] = R[Some[T]](OptionR[T].read andThen (_.asInstanceOf[Some[T]]))
    implicit def NoneR(implicit I: R[Int]): R[None.type] = R[None.type](OptionR[Int].read andThen (_.asInstanceOf[None.type]))
  }

  trait Arrays extends Seqs {
    implicit def ArrayW[T: W: ClassTag] = SeqLikeW[T, Array](Array.unapplySeq)
    implicit def ArrayR[T: R: ClassTag] = SeqLikeR[T, Array](x => Array.apply(x:_*))
  }

  trait Eithers {
    implicit def EitherR[A: R, B: R]: R[Either[A, B]] = R[Either[A, B]](
      RightR[A, B].read orElse LeftR[A, B].read
    )
    implicit def RightR[A: R, B: R]: R[Right[A, B]] = R[Right[A, B]] {
      case Js.Arr(Js.Num(1), x) => Right(readJs[B](x))
    }
    implicit def LeftR[A: R, B: R]: R[Left[A, B]] = R[Left[A, B]] {
      case Js.Arr(Js.Num(0), x) => Left(readJs[A](x))
    }

    implicit def RightW[A: W, B: W]: W[Right[A, B]] = W[Right[A, B]](EitherW[A, B].write)

    implicit def LeftW[A: W, B: W]: W[Left[A, B]] = W[Left[A, B]](EitherW[A, B].write)

    implicit def EitherW[A: W, B: W]: W[Either[A, B]] = W[Either[A, B]]{
      case Left(t) => Js.Arr(Js.Num(0), writeJs(t))
      case Right(t) => Js.Arr(Js.Num(1), writeJs(t))
    }
  }

  trait Durations {
    import BaseCodecs.{StringRW, LongRW}

    implicit final val DurationW: W[Duration] = W[Duration]{
      case Duration.Inf => writeJs("inf")
      case Duration.MinusInf => writeJs("-inf")
      case x if x eq Duration.Undefined => writeJs("undef")
      case x => writeJs(x.toNanos)
    }

    implicit final val InfiniteW = W[Duration.Infinite](DurationW.write)
    implicit final val InfiniteR = R[Duration.Infinite]{
      case Js.Str("inf") => Duration.Inf
      case Js.Str("-inf") => Duration.MinusInf
      case Js.Str("undef") => Duration.Undefined
    }

    implicit final val FiniteW = W[FiniteDuration](DurationW.write)
    implicit final val FiniteR = R[FiniteDuration]{
      case x: Js.Str => Duration.fromNanos(x.value.toLong)
    }

    implicit final val DurationR = R[Duration](validate("DurationString"){FiniteR.read orElse InfiniteR.read})
  }
}