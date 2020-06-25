package jp.ct2.mcfm115.transports

import jp.ct2.mcfm115.Mod
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
    private final lazy val SHAPE = McBlock.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0)
    override def createBlockEntity(view: BlockView) = new BlockEntity()
    override def getOutlineShape(state: BlockState, view: BlockView, pos: BlockPos, context: EntityContext) = Block.SHAPE
  }
  final class BlockEntity extends McBlockEntity(BLOCK_ENTITY_TYPE) with Tickable {
    private var currentFluidAmount: Int = 0

    override def tick() = {
      // todo: nop
    }
  }
  @Environment(EnvType.CLIENT)
  final class EntityRenderer(dispatcher: BlockEntityRenderDispatcher) extends BlockEntityRenderer[BlockEntity](dispatcher) {
    override def render(blockEntity: BlockEntity, tickDelta: Float, matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) = {
      matrices.push()

      val topLight = WorldRenderer.getLightmapCoordinates(blockEntity.getWorld, blockEntity.getPos.up)
      ClientMod.fluidRenderHelper.renderNormalWater(matrices, blockEntity.getWorld, blockEntity.getPos, vertexConsumers.getBuffer(RenderLayers.getFluidLayer(Blocks.WATER.getDefaultState.getFluidState)), topLight, 0.5f)

      matrices.pop()
    }
  }
}
