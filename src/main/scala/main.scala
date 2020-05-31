package com.cterm2.mcfm115

import net.fabricmc.api.ModInitializer
import net.minecraft.util.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.item.{BlockItem, Item, ItemGroup}
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.item.ItemStack

object Mod extends ModInitializer {
    final val ID = "mcfm115"

    final val ITEM_GROUP_MAIN = FabricItemGroupBuilder.build(
        new Identifier(Mod.ID, "main-group"),
        () => new ItemStack(net.minecraft.block.Blocks.COBBLESTONE)
    )

    override def onInitialize = {
        Registry.register(
            Registry.BLOCK,
            new Identifier(Mod.ID, blocks.LargeCombiner.ID),
            blocks.LargeCombiner)

        Registry.register(
            Registry.ITEM,
            new Identifier(Mod.ID, blocks.LargeCombiner.ID),
            new BlockItem(blocks.LargeCombiner, new Item.Settings().group(Mod.ITEM_GROUP_MAIN).maxCount(1)))
    }
}
