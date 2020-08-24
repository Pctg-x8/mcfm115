package jp.ct2.mcfm115.machines

import jp.ct2.mcfm115.Mod
import jp.ct2.mcfm115.utils.SerializeHelper._
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.Material
import net.minecraft.util.registry.Registry
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.world.BlockView
import net.minecraft.item.ItemUsageContext
import net.minecraft.util.ActionResult
import net.minecraft.block.AbstractFurnaceBlock
import net.minecraft.block.BlockState
import net.minecraft.item.ItemPlacementContext
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.item.BlockItem
import net.minecraft.util.math.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.state.StateManager
import net.minecraft.entity.EntityContext
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.util.BooleanBiFunction
import net.minecraft.block.MaterialColor

/**
 * An Anchor Flag. A MiniFlags port with Chunk Loader functionality.
 */
object AnchorFlag {
  final val ID_LOCAL_BASE = "anchor-flag"
  private final def makeIdentifier(suffix: String) = Mod makeIdentifier (ID_LOCAL_BASE + "-" + suffix)
  final def register() = {
    for ((c, s) <- List(
      (MaterialColor.WHITE, "white"),
      (MaterialColor.ORANGE, "orange"),
      (MaterialColor.MAGENTA, "magenta"),
      (MaterialColor.LIGHT_BLUE, "light-blue"),
      (MaterialColor.YELLOW, "yellow"),
      (MaterialColor.LIME, "lime"),
      (MaterialColor.PINK, "pink"),
      (MaterialColor.GRAY, "gray"),
      (MaterialColor.LIGHT_GRAY, "light-gray"),
      (MaterialColor.CYAN, "cyan"),
      (MaterialColor.PURPLE, "purple"),
      (MaterialColor.BLUE, "blue"),
      (MaterialColor.BROWN, "brown"),
      (MaterialColor.GREEN, "green"),
      (MaterialColor.RED, "red"),
      (MaterialColor.BLACK, "black")
    )) {
      val block = new Block(c)
      val item = new BlockItem(block, new net.minecraft.item.Item.Settings maxCount 1 maxDamage 0 group Mod.ITEM_GROUP_MAIN)

      Registry.register(Registry.ITEM, makeIdentifier(s), item)
      Registry.register(Registry.BLOCK, makeIdentifier(s), block)
    }
  }
  /**
    * Design Metrics(0.0 - 1.0)
    */
  object Metrics {
    /**
      * Margin from default block bound
      */
    final val Space = 0.125f
    /**
      * Inverted margin from default block bound
      */
    final val InvSpace = 1.0f - Space
    /**
      * Flag base plate height
      */
    final val BaseHeight = 0.125f
    /**
      * Margin to pole face
      */
    final val Pole = 0.5f - 1.5f / 16.0f
    /**
      * Flag thickness
      */
    final val FlagThickness = 0.75f / 16.0f
  }

  class Block(
    val color: MaterialColor
  ) extends net.minecraft.block.Block((FabricBlockSettings of Material.STONE materialColor color hardness 0.0f).build()) {
    private[this] final lazy val PROP_FACING = HorizontalFacingBlock.FACING
    this.setDefaultState(this.stateManager.getDefaultState() `with` (PROP_FACING, Direction.NORTH))

    override def getPlacementState(ctx: ItemPlacementContext) = this.getDefaultState `with` (PROP_FACING, ctx.getPlayerFacing.getOpposite)
    override protected def appendProperties(builder: StateManager.Builder[net.minecraft.block.Block, BlockState]) = builder add PROP_FACING

    private[this] final val OUTLINE_SHAPE = VoxelShapes.cuboid(Metrics.Space, 0.0, Metrics.Space, Metrics.InvSpace, 1.0, Metrics.InvSpace)
    private[this] final val COLLISION_SHAPE = VoxelShapes.combine(
      VoxelShapes.cuboid(Metrics.Space, 0.0, Metrics.Space, Metrics.InvSpace, Metrics.BaseHeight, Metrics.InvSpace),
      VoxelShapes.cuboid(Metrics.Pole, 0.0, Metrics.Pole, 1.0 - Metrics.Pole, 1.0, 1.0 - Metrics.Pole),
      BooleanBiFunction.OR
    )
    override def getOutlineShape(state: BlockState, view: BlockView, pos: BlockPos, context: EntityContext) = OUTLINE_SHAPE
    override def getCollisionShape(state: BlockState, view: BlockView, pos: BlockPos, context: EntityContext) = COLLISION_SHAPE
  }

  /*
  final class BlockEntity extends net.minecraft.block.entity.BlockEntity(BLOCK_ENTITY_TYPE) {
    final def hashID = BlockEntity makeID this.getPos

    private[this] var name: Option[String] = None
    final def hasCustomName = this.name.isDefined
    
    override def fromTag(tag: CompoundTag) = {
      super.fromTag(tag)
      this.name = Option(tag getString "CustomName") filter { !_.isEmpty }
    }
    override def toTag(tag: CompoundTag) = {
      super.toTag(tag)
      for (n <- this.name) tag.putString("CustomName", n)
      tag
    }
  }
  */
  object BlockEntity {
    // Packing location to Long: y(8bit) x(28bit) z(28bit)
    final def makeID(pos: BlockPos) = ((pos.getY.toLong & 0xff) << 56) | ((pos.getX.toLong & 0xfffffff) << 28) | (pos.getZ.toLong & 0xfffffff)
    final def xCoordFromID(id: Long) = toIntWithSignificantExtension((id >> 28).toInt & 0xfffffff)
    final def yCoordFromID(id: Long) = ((id >> 56).toInt & 0xff).toInt
    final def zCoordFromID(id: Long) = toIntWithSignificantExtension(id.toInt & 0xfffffff)
    private final def toIntWithSignificantExtension(x: Int) = if((x & 0x8000000) != 0) -(x & 0x7ffffff) else x
  }
}
