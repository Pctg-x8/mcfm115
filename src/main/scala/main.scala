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
import net.fabricmc.api.{EnvType, Environment}
import net.fabricmc.fabric.api.client.model.ModelResourceProvider
import net.fabricmc.fabric.api.client.model.ModelProviderContext
import net.minecraft.client.render.model.UnbakedModel
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry

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
    machines.AnchorFlag.register()
    items.TinyPileOfCarbonDust.register()
    items.CarbonDust.register()
    items.SandPaper.register()
  }
}

@Environment(EnvType.CLIENT)
object ClientMod extends ClientModInitializer {
  final lazy val fluidRenderHelper = new utils.ContainedFluidRenderHelper

  override def onInitializeClient() = {
    ModelLoadingRegistry.INSTANCE registerResourceProvider { _ => new CustomModelProvider() }

    transports.WoodGutter.registerClient()
    machines.LargeCombiner.registerClient()
  }
}

@Environment(EnvType.CLIENT)
class CustomModelProvider extends ModelResourceProvider {
  final val ANCHOR_FLAG_MODEL_ID = Mod makeIdentifier "block/anchor-flag"
  final val ANCHOR_FLAG_ITEM_MODEL_ID = Mod makeIdentifier "item/anchor-flag-white"
  override def loadModelResource(resourceId: Identifier, context: ModelProviderContext) =
    if ((resourceId equals ANCHOR_FLAG_MODEL_ID) || (resourceId equals ANCHOR_FLAG_ITEM_MODEL_ID)) renderer.AnchorFlagRenderer
    else null
}
