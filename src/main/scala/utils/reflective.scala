package jp.ct2.mcfm115.utils

import net.minecraft.item.BucketItem
import net.minecraft.fluid.Fluid

package object reflective {
  implicit final class BucketItemFluidAccess(private val bucket: BucketItem) extends AnyVal {
    def getContainedFluid() = {
      val ff = classOf[BucketItem] getDeclaredField "fluid"
      ff.setAccessible(true)
      ff.get(bucket).asInstanceOf[Fluid]
    }
  }
}
