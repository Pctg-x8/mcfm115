package com.cterm2.mcfm115.utils

import net.minecraft.container.Slot
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

final class OutputOnlySlot(
  inventory: Inventory, slotIndex: Int, xPos: Int, yPos: Int
) extends Slot(inventory, slotIndex, xPos, yPos) {
  override def canInsert(stack: ItemStack) = false
}
