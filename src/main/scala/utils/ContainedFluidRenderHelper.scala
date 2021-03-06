package jp.ct2.mcfm115.utils

import net.minecraft.client.MinecraftClient
import net.minecraft.block.Blocks
import net.minecraft.world.BlockRenderView
import net.minecraft.client.render.VertexConsumer
import net.fabricmc.api.{Environment, EnvType}
import net.minecraft.client.util.math.Matrix4f
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.color.world.BiomeColors
import net.minecraft.util.math.BlockPos
import net.minecraft.client.render.model.ModelLoader
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.LavaFluid
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderHandlerRegistryImpl
import net.minecraft.world.biome.Biomes

@Environment(EnvType.CLIENT)
final class ContainedFluidRenderHelper {
  private[this] final val waterSpriteStatic = MinecraftClient.getInstance.getBakedModelManager.getBlockModels.getModel(Blocks.WATER.getDefaultState).getSprite
  private[this] final val waterSpriteFlowing = ModelLoader.WATER_FLOW.getSprite
  private[this] final val lavaSpriteStatic = MinecraftClient.getInstance.getBakedModelManager.getBlockModels.getModel(Blocks.LAVA.getDefaultState).getSprite
  private[this] final val lavaSpriteFlowing = ModelLoader.LAVA_FLOW.getSprite

  def renderFluid(fluid: Fluid, mp: MatrixStack, world: BlockRenderView, blockPos: BlockPos, vertexConsumer: VertexConsumer, light: Int, height: Float = 1.0f, renderSideFlags: RenderSide) = {
    val currentModelMatrix = mp.peek.getModel
    val currentNormalMatrix = mp.peek.getNormal

    val targetSpriteS = if (fluid.isInstanceOf[LavaFluid]) this.lavaSpriteStatic else this.waterSpriteStatic
    val targetSpriteF = if (fluid.isInstanceOf[LavaFluid]) this.lavaSpriteFlowing else this.waterSpriteFlowing

    val tint = FluidRenderHandlerRegistry.INSTANCE get fluid getFluidColor (world, blockPos, fluid.getDefaultState)
    val tintR = (tint >> 16 & 255).asInstanceOf[Float] / 255.0f
    val tintG = (tint >> 8 & 255).asInstanceOf[Float] / 255.0f
    val tintB = (tint & 255).asInstanceOf[Float] / 255.0f

    var topMinU = targetSpriteS getFrameU 0.0
    var topMaxU = targetSpriteS getFrameU 16.0
    var topMinV = targetSpriteS getFrameV 0.0
    var topMaxV = targetSpriteS getFrameV 16.0

    // Render Top(VertexConsumerへはこの順じゃないとダメらしい)
    (vertexConsumer vertex (currentModelMatrix, 0.0f, height * 0.5f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (topMinU, topMinV) light light normal (currentNormalMatrix, 0.0f, 1.0f, 0.0f)).next()
    (vertexConsumer vertex (currentModelMatrix, 0.0f, height * 0.5f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (topMinU, topMaxV) light light normal (currentNormalMatrix, 0.0f, 1.0f, 0.0f)).next()
    (vertexConsumer vertex (currentModelMatrix, 1.0f, height * 0.5f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (topMaxU, topMaxV) light light normal (currentNormalMatrix, 0.0f, 1.0f, 0.0f)).next()
    (vertexConsumer vertex (currentModelMatrix, 1.0f, height * 0.5f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (topMaxU, topMinV) light light normal (currentNormalMatrix, 0.0f, 1.0f, 0.0f)).next()

    // RenderSide
    val sideMinU = targetSpriteF getFrameU 0.0
    val sideMinV = targetSpriteF getFrameV (16.0 - height * 8.0)
    val sideMaxU = targetSpriteF getFrameU 16.0
    val sideMaxV = targetSpriteF getFrameV 16.0

    // North(-Z)
    if (renderSideFlags contains RENDER_SIDE_NORTH) {
      (vertexConsumer vertex (currentModelMatrix, 1.0f, height * 0.5f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMinV) light light normal (currentNormalMatrix, 0.0f, 0.0f, -1.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 1.0f, 0.0f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMaxV) light light normal (currentNormalMatrix, 0.0f, 0.0f, -1.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 0.0f, 0.0f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMaxV) light light normal (currentNormalMatrix, 0.0f, 0.0f, -1.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 0.0f, height * 0.5f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMinV) light light normal (currentNormalMatrix, 0.0f, 0.0f, -1.0f)).next()
    }
    // South(+Z)
    if (renderSideFlags contains RENDER_SIDE_SOUTH) {
      (vertexConsumer vertex (currentModelMatrix, 0.0f, height * 0.5f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMinV) light light normal (currentNormalMatrix, 0.0f, 0.0f, 1.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 0.0f, 0.0f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMaxV) light light normal (currentNormalMatrix, 0.0f, 0.0f, 1.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 1.0f, 0.0f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMaxV) light light normal (currentNormalMatrix, 0.0f, 0.0f, 1.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 1.0f, height * 0.5f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMinV) light light normal (currentNormalMatrix, 0.0f, 0.0f, 1.0f)).next()
    }

    // East(+X?)
    if (renderSideFlags contains RENDER_SIDE_EAST) {
      (vertexConsumer vertex (currentModelMatrix, 1.0f, height * 0.5f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMinV) light light normal (currentNormalMatrix, 1.0f, 0.0f, 0.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 1.0f, 0.0f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMaxV) light light normal (currentNormalMatrix, 1.0f, 0.0f, 0.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 1.0f, 0.0f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMaxV) light light normal (currentNormalMatrix, 1.0f, 0.0f, 0.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 1.0f, height * 0.5f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMinV) light light normal (currentNormalMatrix, 1.0f, 0.0f, 0.0f)).next()
    }
    // West(-X?)
    if (renderSideFlags contains RENDER_SIDE_WEST) {
      (vertexConsumer vertex (currentModelMatrix, 0.0f, height * 0.5f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMinV) light light normal (currentNormalMatrix, -1.0f, 0.0f, 0.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 0.0f, 0.0f, 0.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMinU, sideMaxV) light light normal (currentNormalMatrix, -1.0f, 0.0f, 0.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 0.0f, 0.0f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMaxV) light light normal (currentNormalMatrix, -1.0f, 0.0f, 0.0f)).next()
      (vertexConsumer vertex (currentModelMatrix, 0.0f, height * 0.5f, 1.0f) color (tintR, tintG, tintB, 1.0f) texture (sideMaxU, sideMinV) light light normal (currentNormalMatrix, -1.0f, 0.0f, 0.0f)).next()
    }
  }
}
