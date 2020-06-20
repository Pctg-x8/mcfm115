package com.cterm2.mcfm115.items

import com.cterm2.mcfm115.Mod
import net.minecraft.item.Item
import net.minecraft.util.registry.Registry

/**
  * Sand Paper(handheld soft crusher)
  */
object SandPaper extends Item(new Item.Settings().group(Mod.ITEM_GROUP_MAIN).maxDamage(64)) {
  final val ID = Mod makeIdentifier "sand-paper"

  final def register() = {
    Registry.register(Registry.ITEM, ID, this)
  }
}
