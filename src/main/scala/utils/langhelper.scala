package jp.ct2.mcfm115.utils

import scala.reflect.ClassTag

package object langHelper {
  implicit final class ConversionHelper(private val v: Any) extends AnyVal {
    // https://stackoverflow.com/questions/1803036/how-to-write-asinstanceofoption-in-scala
    @inline
    def tryInstanceOf[T: ClassTag]: Option[T] = Some(this.v) collect { case vv: T => vv }
  }
}
