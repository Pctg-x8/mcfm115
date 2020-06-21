package jp.ct2.mcfm115

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
  final def makeIdentifier(localId: String) = new Identifier(Mod.ID, localId)

  final val ITEM_GROUP_MAIN = FabricItemGroupBuilder.build(
    Mod makeIdentifier "main-group",
    () => new ItemStack(net.minecraft.block.Blocks.COBBLESTONE)
  )

  override def onInitialize() = {
    transports.WoodGutter.register()
    machines.LargeCombiner.register()
    items.TinyPileOfCarbonDust.register()
    items.CarbonDust.register()
    items.SandPaper.register()
  }
}

object ClientMod extends ClientModInitializer {
  override def onInitializeClient() = {
    machines.LargeCombiner.registerClient()
  }
}
