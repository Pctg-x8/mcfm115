package jp.ct2.mcfm115.utils

import net.minecraft.item.ItemStack

package object ItemStackExt {
  def (s: ItemStack) canBeMerged (other: ItemStack): Boolean = {
      if (s.isEmpty() || other.isEmpty()) return true
      if (!(s isItemEqualIgnoreDamage other)) return false
      if (s.getCount() + other.getCount() > s.getMaxCount()) return false

      true
  }
  def (s: ItemStack) isEqual (other: ItemStack): Boolean = s.getCount() == other.getCount() && s.getItem() == other.getItem()
}
