package com.cterm2.mcfm115.transports

import com.cterm2.mcfm115.Mod
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
    override def tick() = {
      // todo: nop
    }
  }
}
