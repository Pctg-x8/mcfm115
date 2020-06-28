package jp.ct2.mcfm115

import jp.ct2.mcfm115.constants
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.item.{Item, Items}
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.container.Slot
import net.fabricmc.api.{Environment, EnvType}

package object utils {
  def distanceFromBlockCentric(target: Entity, bpos: BlockPos) =
    target.squaredDistanceTo(bpos.getX() + 0.5, bpos.getY() + 0.5, bpos.getZ() + 0.5)
    
  def isItemBucket(item: Item) = item == Items.WATER_BUCKET || item == Items.BUCKET

  /**
    * Playerのインベントリ用のスロット位置
    * (ローカル座標なので、左上座標は適当に足して使う)
    *
    * @return (slotX, slotY, slotIndex)
    */
  lazy final val playerInventorySlotLocalPositions = for(
    y <- 0 until constants.DEFAULT_PLAYER_INVENTORY_HIDDEN_SLOT_ROWS;
    x <- 0 until constants.DEFAULT_PLAYER_INVENTORY_SLOT_COLS
  ) yield (
    x * constants.DEFAULT_PLAYER_INVENTORY_SLOT_SPACING,
    y * constants.DEFAULT_PLAYER_INVENTORY_SLOT_SPACING,
    (y + 1) * constants.DEFAULT_PLAYER_INVENTORY_SLOT_COLS + x
  )
  /**
    * Playerの手持ちインベントリ用のスロット位置
    * (ローカル座標なので、左上座標は適当に足して使う)
    *
    * @return (slotX, slotIndex)
    */
  lazy final val playerHandheldInventorySlotLocalPositions = for(
    x <- 0 until constants.DEFAULT_PLAYER_INVENTORY_SLOT_COLS
  ) yield (x * constants.DEFAULT_PLAYER_INVENTORY_SLOT_SPACING, x)
  /**
   * Playerの手持ちインベントリ用のスロットをデフォルトな感じで生成
   * 
   * @param playerInventory プレイヤーのインベントリ
   * @param left 左端位置
   * @param top 上端位置
   */
  def makePlayerInventorySlots(playerInventory: PlayerInventory, left: Int, top: Int) = {
    val handheldOffset = constants.DEFAULT_PLAYER_INVENTORY_HIDDEN_SLOT_ROWS *
      constants.DEFAULT_PLAYER_INVENTORY_SLOT_SPACING + 4
    
    val slots = for ((x, y, index) <- utils.playerInventorySlotLocalPositions)
      yield new Slot(playerInventory, index, left + x, top + y)
    val handheldSlots = for ((x, index) <- utils.playerHandheldInventorySlotLocalPositions)
      yield new Slot(playerInventory, index, left + x, top + handheldOffset)

    slots.concat(handheldSlots)
  }
  
  /**
    * サイド描画フラグ
    *
    * @param value 内部値
    */
  @Environment(EnvType.CLIENT)
  final class RenderSide(private final val value: Int) extends AnyVal {
    def contains(other: RenderSide) = (this.value & other.value) != 0
    def and(other: RenderSide) = new RenderSide(this.value | other.value)
    def excluding(other: RenderSide) = new RenderSide(this.value & ~other.value)
  }
  /**
    * North(-Z)
    */
  @Environment(EnvType.CLIENT)
  final val RENDER_SIDE_NORTH: RenderSide = new RenderSide(1)
  /**
    * South(+Z)
    */
  @Environment(EnvType.CLIENT)
  final val RENDER_SIDE_SOUTH: RenderSide = new RenderSide(2)
  /**
    * East(+X)
    */
  @Environment(EnvType.CLIENT)
  final val RENDER_SIDE_EAST: RenderSide = new RenderSide(4)
  /**
    * West(-X)
    */
  @Environment(EnvType.CLIENT)
  final val RENDER_SIDE_WEST: RenderSide = new RenderSide(8)
  @Environment(EnvType.CLIENT)
  final val RENDER_SIDE_ALL: RenderSide = RENDER_SIDE_NORTH and RENDER_SIDE_SOUTH and RENDER_SIDE_EAST and RENDER_SIDE_WEST
  @Environment(EnvType.CLIENT)
  final val RENDER_SIDE_EMPTY: RenderSide = new RenderSide(0)
}
