package com.cterm2.mcfm115.items

import com.cterm2.mcfm115.Mod
import net.minecraft.item.Item
import net.minecraft.util.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.item.ItemStack

/**
  * Crushed dust, made from coal
  */
object CoalDust extends Item(new Item.Settings().group(Mod.ITEM_GROUP_MAIN)) {
    final val ID = new Identifier(Mod.ID, "coal-dust")

    final def register = {
        Registry.register(Registry.ITEM, CoalDust.ID, this)
    }

    final def stacked(amount: Int): ItemStack = new ItemStack(this, amount)
}

/**
  * Tiny pile of crushed dust, made from chacoals
  * combining 16x pile of coal dust becomes a coal dust
  */
object TinyPileOfCoalDust extends Item(new Item.Settings().group(Mod.ITEM_GROUP_MAIN)) {
    final val ID = new Identifier(Mod.ID, "tiny-pile-of-coal-dust")

    final def register = {
        Registry.register(Registry.ITEM, TinyPileOfCoalDust.ID, this)
    }

    final def stacked(amount: Int): ItemStack = new ItemStack(this, amount)
}
