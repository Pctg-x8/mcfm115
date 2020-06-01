package com.cterm2.mcfm115

import net.fabricmc.api.{ModInitializer, ClientModInitializer}
import net.minecraft.util.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.item.{BlockItem, Item, ItemGroup}
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.item.ItemStack
import net.minecraft.block.entity.BlockEntityType
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.text.TranslatableText

object Mod extends ModInitializer {
    final val ID = "mcfm115"

    final val ITEM_GROUP_MAIN = FabricItemGroupBuilder.build(
        new Identifier(Mod.ID, "main-group"),
        () => new ItemStack(net.minecraft.block.Blocks.COBBLESTONE)
    )

    override def onInitialize = {
        machines.LargeCombiner.register
        items.TinyPileOfCoalDust.register
        items.CoalDust.register
    }
}

object ClientMod extends ClientModInitializer {
    override def onInitializeClient = {
        machines.LargeCombiner.registerClient
    }
}
