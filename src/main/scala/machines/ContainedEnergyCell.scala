package com.cterm2.mcfm115.machines

import net.minecraft.nbt.CompoundTag

/**
  * マシンの内部EnergyCell
  *
  * @param max 最大量(単位: RF)
  */
final class ContainedEnergyCell(val max: Int) {
  /**
    * 現在のEnergyCellの保有量(単位: RF)
    */
  private var current: Int = 0
  /**
    * 残量(単位: RF)
    */
  final def left = this.current
  /**
    * 残量(0.0～1.0)
    */
  final def rateLeft = this.current.asInstanceOf[Float] / this.max.asInstanceOf[Float]

  private final val TAG_KEY_CURRENT = "ContainedECCurrent"
  def toTag(tag: CompoundTag): CompoundTag = {
    tag.putInt(TAG_KEY_CURRENT, current)

    tag
  }
  def fromTag(tag: CompoundTag) = {
    this.current = tag getInt TAG_KEY_CURRENT min this.max
  }
}
