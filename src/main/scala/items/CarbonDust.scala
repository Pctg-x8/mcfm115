package com.cterm2.mcfm115.items

import com.cterm2.mcfm115.Mod
import net.minecraft.item.Item
import net.minecraft.util.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.item.ItemStack

/**
  * Crushed dust, made from coal
  */
object CarbonDust extends Item(new Item.Settings().group(Mod.ITEM_GROUP_MAIN)) {
    final val ID = new Identifier(Mod.ID, "carbon-dust")

    final def register = {
        Registry.register(Registry.ITEM, ID, this)
    }

    final def stacked(amount: Int) = new ItemStack(this, amount)
}

/**
  * Tiny pile of crushed dust, made from charcoals
  * combining 16x this dust becomes a carbon dust
  */
object TinyPileOfCarbonDust extends Item(new Item.Settings().group(Mod.ITEM_GROUP_MAIN)) {
    final val ID = new Identifier(Mod.ID, "tiny-pile-of-carbon-dust")

    final def register = {
        Registry.register(Registry.ITEM, ID, this)
    }

    final def stacked(amount: Int) = new ItemStack(this, amount)
}
