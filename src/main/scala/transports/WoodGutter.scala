package jp.ct2.mcfm115.transports

import jp.ct2.mcfm115.Mod
import jp.ct2.mcfm115.{utils, constants}
import jp.ct2.mcfm115.utils.reflective._
import jp.ct2.mcfm115.utils.langHelper._
import jp.ct2.mcfm115.utils.player._
import jp.ct2.mcfm115.utils.fluid._
import net.minecraft.block.{BlockWithEntity, Block => McBlock}
import net.minecraft.block.entity.{BlockEntity => McBlockEntity, BlockEntityType}
import net.minecraft.block.Material
import net.minecraft.util.registry.Registry
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.world.BlockView
import net.minecraft.util.Tickable
import net.minecraft.block.SlabBlock
import net.minecraft.block.BlockState
import net.minecraft.entity.EntityContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.client.render.block.entity.{BlockEntityRenderer, BlockEntityRenderDispatcher}
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.fabricmc.api.{Environment, EnvType}
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import scala.jdk.FunctionConverters._
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.MinecraftClient
import net.minecraft.block.Blocks
import net.minecraft.block.FluidBlock
import net.minecraft.state.property.Property
import net.minecraft.world.BlockRenderView
import net.minecraft.fluid.FluidState
import net.minecraft.client.render.block.FluidRenderer
import net.minecraft.client.render.RenderLayers
import jp.ct2.mcfm115.ClientMod
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.fluid.Fluid
import net.minecraft.nbt.CompoundTag
import net.minecraft.tag.FluidTags
import net.minecraft.util.Identifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.{ActionResult, Hand}
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import net.minecraft.item.BucketItem
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import java.{util => ju}
import net.minecraft.server.world.ServerWorld

package object WoodGutter {
  final val ID = Mod makeIdentifier "wood-gutter"
  private var BLOCK_ENTITY_TYPE: BlockEntityType[BlockEntity] = null

  final def register() = {
    BLOCK_ENTITY_TYPE = Registry.register(
      Registry.BLOCK_ENTITY_TYPE, ID,
      BlockEntityType.Builder.create(() => new BlockEntity(), Block).build(null)
    )
    Registry.register(Registry.BLOCK, ID, Block)
    Registry.register(Registry.ITEM, ID, new BlockItem(Block, new Item.Settings() group Mod.ITEM_GROUP_MAIN))
  }
  @Environment(EnvType.CLIENT)
  final def registerClient() = {
    BlockEntityRendererRegistry.INSTANCE.register(
      BLOCK_ENTITY_TYPE,
      { new EntityRenderer(_).asInstanceOf[BlockEntityRenderer[BlockEntity]] }
    )
  }

  private final val META_WALL_NORTH_BIT: Int = 1
  private final val META_WALL_SOUTH_BIT: Int = 2
  private final val META_WALL_EAST_BIT: Int = 4
  private final val META_WALL_WEST_BIT: Int = 8

  final object Block extends BlockWithEntity((FabricBlockSettings of Material.WOOD).build()) {
    private final val SHAPE = McBlock.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0)
    override def createBlockEntity(view: BlockView) = new BlockEntity()
    override def getOutlineShape(state: BlockState, view: BlockView, pos: BlockPos, context: EntityContext) = Block.SHAPE

    override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult = {
      if (world.isClient) return ActionResult.SUCCESS

      for (
        be <- (world getBlockEntity pos).tryInstanceOf[BlockEntity];
        usedItem <- Option(player getStackInHand hand);
        bucket <- usedItem.getItem.tryInstanceOf[BucketItem];
        fluid <- bucket.getContainedFluid().filterEmptyType
      ) {
        val hasPoured = be.tryPour(fluid, 1000)
        if (hasPoured) {
          player.consumeHandheldBucketAt(pos, hand)
          return ActionResult.SUCCESS
        }
      }

      ActionResult.PASS
    }

    override def scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: ju.Random) = {
      for (be <- (world getBlockEntity pos).tryInstanceOf[BlockEntity]) {
        be.flow()
      }
    }
  }
  final class BlockEntity extends McBlockEntity(BLOCK_ENTITY_TYPE) {
    private var fluidAmount: Int = 0
    private var fluid: Option[Fluid] = None

    final def currentAmount = this.fluidAmount
    final def currentFluid = this.fluid

    /**
      * 液体を注いでみる
      *
      * @param fluid 液体
      * @param amount 量(mB単位)
      * @return 液体を注ぐことができたか？(例えば、容量が溢れてしまったり液体が合わない場合はfalseが帰る)
      */
    final def tryPour(fluid: Fluid, amount: Int): Boolean = {
      // タイプチェック
      if (!(this.currentFluid map (fluid matchesType _) getOrElse true)) return false
      // 容量チェック
      if (this.fluidAmount + amount > constants.WOOD_GUTTER_MAX_AMOUNT) return false

      // OK
      val firstTimePour = !this.fluid.isDefined
      this.fluid = Some(this.fluid getOrElse fluid)
      this.fluidAmount += amount
      if (firstTimePour) this.scheduleFlow()

      true
    }

    private final def scheduleFlow() = {
      for (f <- this.fluid) {
        this.world.getBlockTickScheduler().schedule(this.pos, Block, f getTickRate this.world)
      }
    }
    def flow() = {
      for (f <- this.fluid if Option(this.world) map { w => !w.isClient } getOrElse false) {
        // todo: flowing
        println("flow")
      }
      this.scheduleFlow()
    }

    override def toTag(tag: CompoundTag) = {
      super.toTag(tag)
      for (f <- this.fluid if this.fluidAmount > 0) {
        tag.putString("FluidID", Registry.FLUID.getId(f).toString())
        tag.putInt("FluidAmount", this.fluidAmount)
      }

      tag
    }
    override def fromTag(tag: CompoundTag) = {
      super.fromTag(tag)
      this.fluid = Option(tag getString "FluidID") filter (_ != "") map { idstr => Registry.FLUID get new Identifier(idstr) }
      this.fluidAmount = tag getInt "FluidAmount"

      this.scheduleFlow()
    }
  }
  @Environment(EnvType.CLIENT)
  final class EntityRenderer(dispatcher: BlockEntityRenderDispatcher) extends BlockEntityRenderer[BlockEntity](dispatcher) {
    override def render(blockEntity: BlockEntity, tickDelta: Float, matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) = {
      matrices.push()

      for (f <- blockEntity.currentFluid if blockEntity.currentAmount > 0) {
        val topLight = WorldRenderer.getLightmapCoordinates(blockEntity.getWorld, blockEntity.getPos.up)
        ClientMod.fluidRenderHelper.renderFluid(
          f,
          matrices,
          blockEntity.getWorld,
          blockEntity.getPos,
          vertexConsumers.getBuffer(RenderLayers.getFluidLayer(Blocks.WATER.getDefaultState.getFluidState)),
          topLight,
          blockEntity.currentAmount.asInstanceOf[Float] / constants.WOOD_GUTTER_MAX_AMOUNT.asInstanceOf[Float],
          utils.RENDER_SIDE_ALL
        )
      }

      matrices.pop()
    }
  }
}
