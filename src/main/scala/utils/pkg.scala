package jp.ct2.mcfm115

import jp.ct2.mcfm115.constants
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.container.Slot

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
}
