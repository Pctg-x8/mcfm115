package jp.ct2.mcfm115.renderer

import net.minecraft.client.render.model.{UnbakedModel, BakedModel}
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.minecraft.block.MaterialColor
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.util.Identifier
import java.{util => ju}
import com.mojang.datafixers.util.Pair
import scala.jdk.CollectionConverters._
import net.minecraft.client.render.model.{ModelBakeSettings, ModelLoader}
import net.minecraft.client.texture.Sprite
import net.minecraft.block.BlockState
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.util.math.Direction
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList
import java.util.function.Supplier
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockRenderView
import net.minecraft.item.ItemStack
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.api.{Environment, EnvType}
import jp.ct2.mcfm115.machines.AnchorFlag.Metrics

@Environment(EnvType.CLIENT)
object AnchorFlagRenderer extends UnbakedModel with BakedModel with FabricBakedModel {
  var baked = false

  private final val SPRITE_IDS = List(
    new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("minecraft:block/stone")),
    new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("minecraft:block/oak_planks")),
    new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("minecraft:block/white_wool"))
  )
  private var sprites = Array[Sprite]()
  private var bakedMesh: Mesh = null

  // UnbakedModel //
  override final def getModelDependencies() = ju.Collections.emptyList()
  override final def getTextureDependencies(unbakedModelGetter: java.util.function.Function[Identifier, UnbakedModel], unresolvedTextureReferences: ju.Set[Pair[String,String]]) = this.SPRITE_IDS.asJavaCollection
  override final def bake(loader: ModelLoader, textureGetter: java.util.function.Function[SpriteIdentifier,Sprite], rotationContainer: ModelBakeSettings, modelId: Identifier): BakedModel = {
    if (this.baked) return this
    this.baked = true

    // Load all sprites
    this.sprites = (this.SPRITE_IDS map textureGetter.apply).toArray

    // Build mesh
    val renderer = RendererAccess.INSTANCE.getRenderer()
    val builder = renderer.meshBuilder
    val emitter = builder.getEmitter()

    // Build base box
    // Note: 重ねた際に下部がちゃんと描画されるように0.01にする(ほんとはたぶんBlockかどっかで設定できるはずなんだけど......)
    emitter square (Direction.DOWN, Metrics.Space, Metrics.Space, Metrics.InvSpace, Metrics.InvSpace, 0.01f)
    emitter spriteBake (0, this.sprites(0), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
    emitter.emit()
    emitter square (Direction.UP, Metrics.Space, Metrics.Space, Metrics.InvSpace, Metrics.InvSpace, 1.0f - Metrics.BaseHeight)
    emitter spriteBake (0, this.sprites(0), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
    emitter.emit()
    for (d <- Direction.values if d != Direction.UP && d != Direction.DOWN) {
      emitter square (d, Metrics.Space, 0.0f, Metrics.InvSpace, Metrics.BaseHeight, Metrics.Space)
      emitter spriteBake (0, this.sprites(0), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
      emitter.emit()
    }

    // Build Pole
    emitter square (Direction.UP, Metrics.Pole, Metrics.Pole, 1.0f - Metrics.Pole, 1.0f - Metrics.Pole, 0.0f)
    emitter spriteBake (0, this.sprites(1), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
    emitter.emit()
    for (d <- Direction.values if d != Direction.UP && d != Direction.DOWN) {
      emitter square (d, Metrics.Pole, Metrics.BaseHeight, 1.0f - Metrics.Pole, 1.0f, Metrics.Pole)
      emitter spriteBake (0, this.sprites(1), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
      emitter.emit()
    }

    // Build Flag Part(Square White)
    emitter square (Direction.WEST, Metrics.Space, 1.0f - 5.5f / 16.0f, Metrics.Pole, 1.0f - 1.0f / 16.0f, 0.5f - Metrics.FlagThickness)
    emitter spriteBake (0, this.sprites(2), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
    emitter.emit()
    emitter square (Direction.EAST, 1.0f - Metrics.Pole, 1.0f - 5.5f / 16.0f, 1.0f - Metrics.Space, 1.0f - 1.0f / 16.0f, 0.5f - Metrics.FlagThickness)
    emitter spriteBake (0, this.sprites(2), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
    emitter.emit()
    emitter square (Direction.NORTH, 0.5f - Metrics.FlagThickness, 1.0f - 5.5f / 16.0f, 0.5f + Metrics.FlagThickness, 1.0f - 1.0f / 16.0f, Metrics.Space)
    emitter spriteBake (0, this.sprites(2), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
    emitter.emit()
    emitter square (Direction.UP, 0.5f - Metrics.FlagThickness, 1.0f - Metrics.Pole, 0.5f + Metrics.FlagThickness, Metrics.InvSpace, 1.0f / 16.0f)
    emitter spriteBake (0, this.sprites(2), MutableQuadView.BAKE_LOCK_UV) spriteColor(0, -1, -1, -1, -1)
    emitter.emit()
    emitter square (Direction.DOWN, 0.5f - Metrics.FlagThickness, Metrics.Space, 0.5f + Metrics.FlagThickness, Metrics.Pole, 1.0f - 5.5f / 16.0f)
    emitter spriteBake (0, this.sprites(2), MutableQuadView.BAKE_LOCK_UV) spriteColor (0, -1, -1, -1, -1)
    emitter.emit()

    this.bakedMesh = builder.build()
    this
  }

  // BakedModel //
  override def getQuads(state: BlockState, face: Direction, random: ju.Random) = null
  override def useAmbientOcclusion() = false
  override def hasDepth() = false
  override def isSideLit() = false
  override def isBuiltin() = false
  /**
    * 壊したときに使われるスプライト
    */
  override def getSprite() = this.sprites(1)
  override def getTransformation() = null
  override def getItemPropertyOverrides() = null

  // FabricBakedModel //
  /**
    * false to use FabricBakedModel rendering
    */
  override def isVanillaAdapter() = false
  override def emitBlockQuads(blockView: BlockRenderView, state: BlockState, pos: BlockPos, randomSupplier: Supplier[ju.Random], context: RenderContext) = {
    context.meshConsumer accept this.bakedMesh
  }
  override def emitItemQuads(stack: ItemStack, randomSupplier: Supplier[ju.Random], context: RenderContext) = {

  }
}
