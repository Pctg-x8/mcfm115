package com.cterm2.mcfm115.blocks

import com.cterm2.mcfm115.Mod
import com.cterm2.mcfm115.utils
import com.cterm2.mcfm115.constants
import com.cterm2.mcfm115.common.GenericIOSides
import net.minecraft.block.Material
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.world.BlockView
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier
import net.minecraft.util.DefaultedList
import net.minecraft.item.ItemStack
import net.minecraft.text.TranslatableText
import net.minecraft.block.entity.LootableContainerBlockEntity
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.util.Tickable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.container.Container
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.math.Direction
import net.minecraft.item.Item
import scala.util.Try
import net.minecraft.block.BlockState
import net.minecraft.util.{ActionResult, Hand}
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.client.gui.screen.ingame.ContainerScreen
import net.minecraft.text.Text
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.inventory.Inventory
import net.minecraft.container.Slot

object LargeCombiner extends BlockWithEntity(FabricBlockSettings.of(Material.METAL).build()) {
    final val ID = new Identifier(Mod.ID, "large-combiner")

    override def createBlockEntity(view: BlockView): BlockEntity = new LargeCombinerBlockEntity()
    override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult = {
        if (!world.isClient && world.getBlockEntity(pos).isInstanceOf[LargeCombinerBlockEntity]) {
            ContainerProviderRegistry.INSTANCE
                .openContainer(LargeCombiner.ID, player, buf => { buf writeBlockPos pos; () })
        }

        ActionResult.SUCCESS
    }
}

/**
  * いわゆるTileEntityのことかな？
  * Containers of a Large Combiner
  */
final class LargeCombinerBlockEntity extends LockableContainerBlockEntity(Mod.BLOCK_ENTITY_TYPE_LARGE_COMBINER) with SidedInventory with Tickable {
    /**
      * input / output
      */
    override final val getInvSize = 2
    private val inventory = DefaultedList.ofSize[ItemStack](this.getInvSize, ItemStack.EMPTY);

    /**
      * IO portのサイドとのマッピング
      *
      * @param globalSide グローバルなサイド定数(Directionのこと)
      * @return IO portを表すGenericIOSides
      */
    // todo: あとでブロックの設定や方向によって変化させる
    private def mapSide(globalSide: Direction): GenericIOSides = globalSide match {
        case Direction.DOWN => GenericIOSides.Output
        case Direction.UP => GenericIOSides.Input
        case _ => GenericIOSides.Invalid
    }
    private def mapSlotIndex(ios: GenericIOSides): Int = ios match {
        case GenericIOSides.Output => 0
        case GenericIOSides.Input => 1
        case _ => throw new IllegalArgumentException("no available slots for otherwise ios")
    }

    private def isValidForInput(item: Item) = true
    private def isValidForOutput(item: Item) = !utils.isItemBucket(item)

    override def tick() = {
        // todo: noop
    }

    override def clear() = { this.inventory.clear() }
    override def canPlayerUseInv(player: PlayerEntity) = utils.distanceFromBlockCentric(player, this.pos) <= constants.PLAYER_DISTANCE_CONTAINER_USABLE_THRESHOLD
    override def getInvStack(slot: Int) = this.inventory.get(slot)
    override val isInvEmpty = this.inventory.stream.allMatch(i => i.isEmpty)
    override def removeInvStack(slot: Int): ItemStack = Inventories.removeStack(this.inventory, slot)
    override def setInvStack(slot: Int, stack: ItemStack) = this.inventory.set(slot, stack)
    override def takeInvStack(slot: Int, amount: Int) = Inventories.splitStack(this.inventory, slot, amount)

    override def createContainer(i: Int, playerInventory: PlayerInventory) = new LargeCombinerContainer(i, playerInventory, this.asInstanceOf[Inventory])
    override val getContainerName = new TranslatableText("container.large-combiner")
    override def canExtractInvStack(slot: Int, stack: ItemStack, dir: Direction) = this.mapSide(dir) == GenericIOSides.Output && slot == 1 && !utils.isItemBucket(stack.getItem)
    override def canInsertInvStack(slot: Int, stack: ItemStack, dir: Direction) = this.mapSide(dir) match {
        case GenericIOSides.Output => this.isValidForOutput(stack.getItem)
        case GenericIOSides.Input => this.isValidForInput(stack.getItem)
        case _ => false
    }
    override def getInvAvailableSlots(side: Direction): Array[Int] = Try { Array(this.mapSlotIndex(this.mapSide(side))) }.getOrElse(Array())
}

final class LargeCombinerContainer(syncId: Int, playerInventory: PlayerInventory, private val inventory: Inventory) extends Container(null, syncId) {
    this.inventory onInvOpen this.playerInventory.player
    utils.makePlayerInventorySlots(this.playerInventory, 8, 103 + 18 + 18) foreach this.addSlot

    override def canUse(player: PlayerEntity) = this.inventory canPlayerUseInv player
    /**
      * Shift+クリックの動作
      *
      * @param player 操作プレイヤー
      * @param slotIndex 操作対象スロット
      */
    override def transferSlot(player: PlayerEntity, slotIndex: Int): ItemStack = {
        var newStack = ItemStack.EMPTY

        val slot = this.slots get slotIndex
        if (slot != null && slot.hasStack) {
            val orgStack = slot.getStack
            newStack = orgStack
            if (slotIndex < this.inventory.getInvSize) {
                if (!this.insertItem(orgStack, this.inventory.getInvSize, this.slots.size, true)) return ItemStack.EMPTY;
            } else if (!this.insertItem(orgStack, 0, this.inventory.getInvSize, false)) return ItemStack.EMPTY;

            if (orgStack.isEmpty) slot setStack ItemStack.EMPTY else slot.markDirty
        }

        newStack
    }
}

object LargeCombinerPanelView {
    final val TEXTURE = new Identifier(Mod.ID, "textures/view/container/large-combiner.png")
    final val TITLE = new TranslatableText("view.title.largeCombiner")
}
final class LargeCombinerPanelView(
    container: LargeCombinerContainer,
    playerInventory: PlayerInventory
) extends ContainerScreen[LargeCombinerContainer](container, playerInventory, LargeCombinerPanelView.TITLE) {
    this.containerHeight = 114 + 6 * 18

    override def drawForeground(mouseX: Int, mouseY: Int) = {
        this.font.draw(this.title.asFormattedString, 8.0f, 6.0f, constants.PRIMARY_VIEW_TEXT_COLOR);
        this.font.draw(this.playerInventory.getDisplayName.asFormattedString, 8.0f, (this.containerHeight - 96 + 2).asInstanceOf[Float], constants.PRIMARY_VIEW_TEXT_COLOR)
    }
    override def drawBackground(delta: Float, mouseX: Int, mouseY: Int) = {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        
        val viewOriginX = (this.width - this.containerWidth) / 2;
        val viewOriginY = (this.height - this.containerHeight) / 2;
        this.minecraft.getTextureManager bindTexture LargeCombinerPanelView.TEXTURE
        this.blit(viewOriginX, viewOriginY, 0, 0, this.containerHeight, 6 * 18 + 17)
        this.blit(viewOriginX, viewOriginY + 6 * 18 + 17, 0, 126, this.containerWidth, 96)
    }
}
