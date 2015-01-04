import acyclic.file

package object upickle extends Types with Implicits with Generated {
  protected[this] def validate[T](name: String)(pf: PartialFunction[Js.Value, T]) = Internal.validate(name)(pf)
}