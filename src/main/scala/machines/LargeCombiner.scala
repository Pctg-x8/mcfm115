package com.cterm2.mcfm115.machines

import com.cterm2.mcfm115.Mod
import com.cterm2.mcfm115.{utils, constants, texmodel}
import com.cterm2.mcfm115.common.GenericIOSides
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.block.{Material, BlockWithEntity, BlockState}
import net.minecraft.block.entity.{BlockEntity, BlockEntityType, LockableContainerBlockEntity}
import net.minecraft.world.{World, BlockView}
import net.minecraft.util.{Identifier, DefaultedList, Tickable, ActionResult, Hand}
import net.minecraft.util.math.{Direction, BlockPos}
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.text.{Text, TranslatableText}
import net.minecraft.inventory.{Inventories, Inventory, SidedInventory}
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.container.Slot
import net.minecraft.client.gui.screen.ingame.ContainerScreen
import scala.util.Try
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.{Environment, EnvType}
import net.minecraft.util.registry.Registry
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.item.BlockItem

object LargeCombiner {
    final val ID = new Identifier(Mod.ID, "large-combiner")
    private var BLOCK_ENTITY_TYPE: BlockEntityType[BlockEntity] = null
    final def register = {
        val blockEntityTypeInstance = BlockEntityType.Builder.create(() => new BlockEntity(), Block).build(null)
        this.BLOCK_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, ID, blockEntityTypeInstance)
        ContainerProviderRegistry.INSTANCE.registerFactory(
            ID,
            (sid, ident, player, buf) => {
                val world = player.world
                val pos = buf.readBlockPos

                world.getBlockState(pos).createContainerFactory(world, pos).createMenu(sid, player.inventory, player)
            }
        )

        val item = new BlockItem(Block, new Item.Settings().group(Mod.ITEM_GROUP_MAIN).maxCount(1))
        Registry.register(Registry.BLOCK, ID, Block)
        Registry.register(Registry.ITEM, ID, item)
    }
    final def registerClient = {
        ScreenProviderRegistry.INSTANCE.registerFactory[Container](
            ID,
            c => new PanelView(c, MinecraftClient.getInstance.player.inventory)
        )
    }
    
    final val INVENTORY_INDEX_INPUT = 0
    final val INVENTORY_INDEX_OUTPUT = 1
    final val INVENTORY_INDEX_EC_INPUT = 2
    final val INVENTORY_INDEX_SLUG_OUTPUT = 3
    final val INVENTORY_COUNT = INVENTORY_INDEX_SLUG_OUTPUT + 1

    object Block extends BlockWithEntity(FabricBlockSettings.of(Material.METAL).build()) {
        override def createBlockEntity(view: BlockView): BlockEntity = new BlockEntity()
        override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult = {
            if (!world.isClient && world.getBlockEntity(pos).isInstanceOf[BlockEntity]) {
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
    final class BlockEntity extends LockableContainerBlockEntity(BLOCK_ENTITY_TYPE) with SidedInventory with Tickable {
        override final val getInvSize = INVENTORY_COUNT
        private val inventory = DefaultedList.ofSize[ItemStack](this.getInvSize, ItemStack.EMPTY);

        /**
         * IO portのサイドとのマッピング
         *
         * @param globalSide グローバルなサイド定数(Directionのこと)
         * @return IO portを表すGenericIOSides
         */
        private def mapSide(globalSide: Direction): GenericIOSides = globalSide match {
            // todo: あとでブロックの設定や方向によって変化させる
            case Direction.WEST => GenericIOSides.Output
            case Direction.EAST => GenericIOSides.Input
            case Direction.UP => GenericIOSides.EnergyCellInput
            case Direction.DOWN => GenericIOSides.SlugOutput
            case _ => GenericIOSides.Invalid
        }
        private def mapSlotIndex(ios: GenericIOSides): Int = ios match {
            case GenericIOSides.Output => INVENTORY_INDEX_OUTPUT
            case GenericIOSides.Input => INVENTORY_INDEX_INPUT
            case GenericIOSides.EnergyCellInput => INVENTORY_INDEX_EC_INPUT
            case GenericIOSides.SlugOutput => INVENTORY_INDEX_SLUG_OUTPUT
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

        override def createContainer(i: Int, playerInventory: PlayerInventory) = new Container(i, playerInventory, this.asInstanceOf[Inventory])
        override val getContainerName = new TranslatableText("container.large-combiner")
        override def canExtractInvStack(slot: Int, stack: ItemStack, dir: Direction) = this.mapSide(dir) == GenericIOSides.Output && slot == 1 && !utils.isItemBucket(stack.getItem)
        override def canInsertInvStack(slot: Int, stack: ItemStack, dir: Direction) = this.mapSide(dir) match {
            case GenericIOSides.Output => this.isValidForOutput(stack.getItem)
            case GenericIOSides.Input => this.isValidForInput(stack.getItem)
            case _ => false
        }
        override def getInvAvailableSlots(side: Direction): Array[Int] = Try { Array(this.mapSlotIndex(this.mapSide(side))) }.getOrElse(Array())
    }
    
    final class Container(syncId: Int, playerInventory: PlayerInventory, private val inventory: Inventory) extends net.minecraft.container.Container(null, syncId) {
        this.inventory onInvOpen this.playerInventory.player
        this addSlot new Slot(
            this.inventory, INVENTORY_INDEX_INPUT,
            texmodel.LargeCombinerPanelView.SLOT_POSITION_INGREDIENT_X, texmodel.LargeCombinerPanelView.SLOT_POSITION_PROCESSLINE_Y
        )
        this addSlot new Slot(
            this.inventory, INVENTORY_INDEX_OUTPUT,
            texmodel.LargeCombinerPanelView.SLOT_POSITION_OUTPUT_X, texmodel.LargeCombinerPanelView.SLOT_POSITION_PROCESSLINE_Y
        )
        this addSlot new Slot(
            this.inventory, INVENTORY_INDEX_EC_INPUT,
            texmodel.LargeCombinerPanelView.SLOT_POSITION_EC_X, texmodel.LargeCombinerPanelView.SLOT_POSITION_EC_Y
        )
        this addSlot new Slot(
            this.inventory, INVENTORY_INDEX_SLUG_OUTPUT,
            texmodel.LargeCombinerPanelView.SLOT_POSITION_SLUGOUT_X, texmodel.LargeCombinerPanelView.SLOT_POSITION_SLUGOUT_Y
        )
        utils.makePlayerInventorySlots(this.playerInventory, 8, texmodel.LargeCombinerPanelView.PLAYER_INVENTORY_START_Y) foreach this.addSlot

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

    @Environment(EnvType.CLIENT)
    object PanelView {
        final val TITLE = new TranslatableText("view.title.large-combiner")
    }
    @Environment(EnvType.CLIENT)
    final class PanelView(
        container: Container,
        playerInventory: PlayerInventory
    ) extends ContainerScreen[Container](container, playerInventory, PanelView.TITLE) {
        this.containerWidth = texmodel.LargeCombinerPanelView.PANEL_WIDTH
        this.containerHeight = texmodel.LargeCombinerPanelView.PANEL_HEIGHT

        override def render(mouseX: Int, mouseY: Int, delta: Float) = {
            // ちゃんとオーバーライドしてrenderBackgroundしてあげないと背景が暗くなってくれないらしい(なんだそれは)
            this.renderBackground()
            super.render(mouseX, mouseY, delta)
        }
        override def drawForeground(mouseX: Int, mouseY: Int) = {
            utils.drawHelper.drawStringCentric(this.font, this.title.asFormattedString, this.containerWidth / 2.0f, 6.0f, constants.PRIMARY_VIEW_TEXT_COLOR);
            this.font.draw(this.playerInventory.getDisplayName.asFormattedString, 8.0f, (texmodel.LargeCombinerPanelView.PLAYER_INVENTORY_START_Y - 8 - 4).asInstanceOf[Float], constants.PRIMARY_VIEW_TEXT_COLOR)
        }
        override def drawBackground(delta: Float, mouseX: Int, mouseY: Int) = {
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
            val viewOriginX = (this.width - this.containerWidth) / 2;
            val viewOriginY = (this.height - this.containerHeight) / 2;

            this.minecraft.getTextureManager bindTexture texmodel.LargeCombinerPanelView.MAIN_TEXTURE
            texmodel.LargeCombinerPanelView.blitMainPanel(this, viewOriginX, viewOriginY)
            texmodel.LargeCombinerPanelView.blitProgressArrowOverlay(
                this,
                viewOriginX + texmodel.LargeCombinerPanelView.ARROW_OVERLAY_X,
                viewOriginY + texmodel.LargeCombinerPanelView.ARROW_OVERLAY_Y,
                0.6f
            )

            texmodel.LargeCombinerPanelView.blitEnergyCellBase(
                this,
                viewOriginX + texmodel.LargeCombinerPanelView.EC_OVERLAY_X,
                viewOriginY + texmodel.LargeCombinerPanelView.EC_OVERLAY_Y
            )
        }
    }
}
