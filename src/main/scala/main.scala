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
    var BLOCK_ENTITY_TYPE_LARGE_COMBINER: BlockEntityType[blocks.LargeCombinerBlockEntity] = null

    override def onInitialize = {
        this.BLOCK_ENTITY_TYPE_LARGE_COMBINER = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            blocks.LargeCombiner.ID,
            BlockEntityType.Builder.create(() => new blocks.LargeCombinerBlockEntity(), blocks.LargeCombiner).build(null)
        )
        ContainerProviderRegistry.INSTANCE.registerFactory(
            blocks.LargeCombiner.ID,
            (sid, ident, player, buf) => {
                val world = player.world
                val pos = buf.readBlockPos

                world.getBlockState(pos).createContainerFactory(world, pos).createMenu(sid, player.inventory, player)
            }
        )

        Registry.register(
            Registry.BLOCK,
            blocks.LargeCombiner.ID,
            blocks.LargeCombiner)

        Registry.register(
            Registry.ITEM,
            blocks.LargeCombiner.ID,
            new BlockItem(blocks.LargeCombiner, new Item.Settings().group(Mod.ITEM_GROUP_MAIN).maxCount(1)))
    }
}

object ClientMod extends ClientModInitializer {
    override def onInitializeClient = {
        ScreenProviderRegistry.INSTANCE.registerFactory[blocks.LargeCombinerContainer](
            blocks.LargeCombiner.ID,
            c => new blocks.LargeCombinerPanelView(c, MinecraftClient.getInstance.player.inventory)
        )
    }
}
