package com.cterm2.mcfm115.utils

import net.minecraft.item.ItemStack

package object ItemStackExt {
  implicit final class ItemStackExtWrapper(private val stack: ItemStack) extends AnyVal {
    /**
      * otherをthisにマージ可能かどうか
      * マージ可能なら、クラフト時にItemStack.incrementで増やしても問題ないことになる
      *
      * @param other マージ対象のItemStack
      */
    final def canBeMerged(other: ItemStack): Boolean = {
      if (this.stack.isEmpty() || other.isEmpty()) return true
      if (!(this.stack isItemEqualIgnoreDamage other)) return false
      if (this.stack.getCount() + other.getCount() > this.stack.getMaxCount()) return false

      true
    }

    /**
      * 同値判定(アイテムの個数とItemインスタンスで判定する)
      */
    final def isEqual(other: ItemStack): Boolean = this.stack.getCount() == other.getCount() && this.stack.getItem() == other.getItem()
  }
}
