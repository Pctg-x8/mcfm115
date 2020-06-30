package jp.ct2.mcfm115.utils

import net.minecraft.fluid.Fluid
import net.minecraft.fluid.EmptyFluid

package object fluid {
  implicit final class FluidOpHelper(private val f: Fluid) extends AnyVal {
    final def filterEmptyType: Option[Fluid] = Some(f) filterNot { _.isInstanceOf[EmptyFluid] }
  }
}
