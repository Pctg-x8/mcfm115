package com.cterm2.mcfm115.machines

import com.cterm2.mcfm115.Mod
import com.cterm2.mcfm115.{utils, constants, texmodel, items}
import com.cterm2.mcfm115.common.GenericIOSides
import com.cterm2.mcfm115.utils.SerializeHelper._
import com.cterm2.mcfm115.utils.ItemStackExt._
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.block.{Material, BlockWithEntity, BlockState}
import net.minecraft.block.entity.{BlockEntity, BlockEntityType, LockableContainerBlockEntity}
import net.minecraft.world.{World, BlockView}
import net.minecraft.util.{DefaultedList, Tickable, ActionResult, Hand}
import net.minecraft.util.math.{Direction, BlockPos}
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.item.{Item, ItemStack, BlockItem}
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
import com.cterm2.mcfm115.utils.TextRendererExt._
import net.minecraft.nbt.CompoundTag
import net.minecraft.recipe.{RecipeType, Recipe => IBaseRecipe, Ingredient, RecipeSerializer => IRecipeSerializer}
import net.minecraft.util.{Identifier, PacketByteBuf}
import com.google.gson.JsonObject
import scala.jdk.OptionConverters._
import net.minecraft.container.PropertyDelegate

object LargeCombiner {
  final val ID = Mod makeIdentifier "large-combiner"
  final val RECIPE_ID = Mod makeIdentifier "large_combine"
  private var BLOCK_ENTITY_TYPE: BlockEntityType[BlockEntity] = null
  private var RECIPE_TYPE: RecipeType[Recipe] = null
  private var RECIPE_SERIALIZER: RecipeSerializer = null
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
    this.RECIPE_TYPE = Registry.register(Registry.RECIPE_TYPE, RECIPE_ID, new RecipeType[Recipe] {})
    this.RECIPE_SERIALIZER = Registry.register(Registry.RECIPE_SERIALIZER, RECIPE_ID, new RecipeSerializer())
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
  final val PD_INDEX_PROCESS_TIME = 0
  final val PD_INDEX_PROCESS_FULL = 1
  final val PD_INDEX_ENERGY_LEFT = 2
  final val PD_INDEX_ENERGY_FULL = 3

  final case class Recipe(
    final val input: Ingredient,
    final val output: ItemStack,
    final val processTime: Int,
    final val requireCount: Int
  ) extends IBaseRecipe[BlockEntity] {
    override def matches(inv: BlockEntity, world: World) = {
      val target = inv getInvStack INVENTORY_INDEX_INPUT
      (this.input test target) && target.getCount() >= this.requireCount
    }
    override def craft(inv: BlockEntity) = this.output.copy
    override final val getId = RECIPE_ID
    override def getSerializer() = RECIPE_SERIALIZER
    override def getType() = RECIPE_TYPE
    override final val getOutput = this.output
    @Environment(EnvType.CLIENT)
    override def fits(width: Int, height: Int) = true
  }
  final class RecipeSerializer extends IRecipeSerializer[Recipe] {
    override def read(id: Identifier, json: JsonObject) = {
      val ingredient = json getIngredient "ingredient"
      val output = json getItemStack "result"
      val processTime = json getInt "processTime"
      val requireCount = json getIntOpt "requireCount" getOrElse 1

      Recipe(ingredient, output, processTime, requireCount)
    }

    override def read(id: Identifier, buf: PacketByteBuf) = {
      val ingredient = Ingredient fromPacket buf
      val output = buf.readItemStack()
      val processTime = buf.readVarInt()
      val requireCount = buf.readVarInt()

      Recipe(ingredient, output, processTime, requireCount)
    }
    override def write(buf: PacketByteBuf, recipe: Recipe) = {
      recipe.input write buf
      buf writeItemStack recipe.output
      buf writeVarInt recipe.processTime
      buf writeVarInt recipe.requireCount
    }
  }

  object Block extends BlockWithEntity(FabricBlockSettings.of(Material.METAL).build()) {
    override def createBlockEntity(view: BlockView): BlockEntity = new BlockEntity()
    override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult = {
      if (!world.isClient && world.getBlockEntity(pos).isInstanceOf[BlockEntity]) {
        ContainerProviderRegistry.INSTANCE.openContainer(ID, player, buf => { buf writeBlockPos pos; () })
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
    private[this] val inventory = DefaultedList.ofSize[ItemStack](this.getInvSize, ItemStack.EMPTY)
    private val ec = new ContainedEnergyCell(128 * 1000)
    private var currentProcessingRecipe: Option[Recipe] = None

    private var processTime: Int = 0
    private var oldInputItem: ItemStack = ItemStack.EMPTY

    def getProcessTime = this.processTime
    def getProcessFullTime = this.currentProcessingRecipe map (_.processTime) getOrElse 0
    def getEnergyCell = this.ec

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

    private def findMatchingRecipe() = this.world.getRecipeManager.getFirstMatch(
      RECIPE_TYPE, this, this.world
    ).toScala

    /**
      * レシピを更新する
      *
      * @return レシピに更新が入ったか？
      */
    private final def updateCurrentRecipe(): Boolean = {
      val oldRecipe = this.currentProcessingRecipe
      this.currentProcessingRecipe = this.findMatchingRecipe()
      oldRecipe != this.currentProcessingRecipe
    }

    override def tick(): Unit = {
      if (this.world.isClient) return

      // レシピ更新
      if (!(this.slotItemInput isEqual this.oldInputItem)) {
        val hasUpdated = this.updateCurrentRecipe()
        if (hasUpdated) {
          this.processTime = 0
          this.markDirty()
        }
        this.oldInputItem = this.slotItemInput.copy()
      }

      for (r <- this.currentProcessingRecipe if this canAcceptRecipeOutput r) {
        this.processTime += 1
        if (this.processTime >= r.processTime) {
          this.processTime -= r.processTime
          this.doCraft(r)
          this.updateCurrentRecipe()
        }
        this.markDirty()
      }
    }

    private final def doCraft(recipe: Recipe) = {
      if (this.canAcceptRecipeOutput(recipe)) {
        if (this.slotItemOutput.isEmpty()) {
          this.inventory.set(INVENTORY_INDEX_OUTPUT, recipe.getOutput.copy())
        } else {
          this.slotItemOutput increment recipe.getOutput.getCount()
        }

        this.slotItemInput decrement recipe.requireCount
      }
    }

    final def slotItemInput = this.inventory get INVENTORY_INDEX_INPUT
    final def slotItemOutput = this.inventory get INVENTORY_INDEX_OUTPUT

    private final def canAcceptRecipeOutput(recipe: Recipe) = {
      val afterStackCount = this.slotItemOutput.getCount() + recipe.getOutput.getCount()

      (this.slotItemOutput canBeMerged recipe.getOutput) && afterStackCount <= this.getInvMaxStackAmount()
    }

    override def clear() = { this.inventory.clear() }
    override def canPlayerUseInv(player: PlayerEntity) = utils.distanceFromBlockCentric(player, this.pos) <= constants.PLAYER_DISTANCE_CONTAINER_USABLE_THRESHOLD
    override def getInvStack(slot: Int) = this.inventory.get(slot)
    override val isInvEmpty = this.inventory.stream.allMatch(i => i.isEmpty)
    override def removeInvStack(slot: Int) = Inventories.removeStack(this.inventory, slot)
    override def setInvStack(slot: Int, stack: ItemStack) = {
      this.inventory.set(slot, stack)
    }
    override def takeInvStack(slot: Int, amount: Int) = Inventories.splitStack(this.inventory, slot, amount)

    override def createContainer(i: Int, playerInventory: PlayerInventory) = new Container(i, playerInventory, this.asInstanceOf[Inventory], this)
    override val getContainerName = new TranslatableText("container.large-combiner")
    override def canExtractInvStack(slot: Int, stack: ItemStack, dir: Direction) = this.mapSide(dir) == GenericIOSides.Output && slot == 1 && !utils.isItemBucket(stack.getItem)
    override def canInsertInvStack(slot: Int, stack: ItemStack, dir: Direction) = this.mapSide(dir) match {
      case GenericIOSides.Output => false
      case GenericIOSides.Input => true
      case _ => false
    }
    override def getInvAvailableSlots(side: Direction): Array[Int] = Try { Array(this.mapSlotIndex(this.mapSide(side))) }.getOrElse(Array())

    private[this] final val TAG_KEY_PROCESS_TIME = "LargeCombinerProcessTime"
    override def toTag(tag: CompoundTag): CompoundTag = {
      super.toTag(tag)
      Inventories.toTag(tag, this.inventory)
      this.ec toTag tag
      tag.putInt(TAG_KEY_PROCESS_TIME, this.processTime)

      tag
    }
    override def fromTag(tag: CompoundTag) = {
      super.fromTag(tag)
      this.inventory.clear()
      Inventories.fromTag(tag, this.inventory)
      this.ec fromTag tag
      this.processTime = tag getInt TAG_KEY_PROCESS_TIME
    }
  }
  
  final class Container(
    syncId: Int,
    playerInventory: PlayerInventory,
    private[this] val inventory: Inventory,
    private[this] val blockEntity: BlockEntity
  ) extends net.minecraft.container.Container(null, syncId) {
    this.inventory onInvOpen this.playerInventory.player
    this addSlot new Slot(
      this.inventory, INVENTORY_INDEX_INPUT,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_INGREDIENT_X,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_PROCESSLINE_Y
    )
    this addSlot new utils.OutputOnlySlot(
      this.inventory, INVENTORY_INDEX_OUTPUT,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_OUTPUT_X,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_PROCESSLINE_Y
    )
    this addSlot new Slot(
      this.inventory, INVENTORY_INDEX_EC_INPUT,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_EC_X,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_EC_Y
    )
    this addSlot new Slot(
      this.inventory, INVENTORY_INDEX_SLUG_OUTPUT,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_SLUGOUT_X,
      texmodel.LargeCombinerPanelView.SLOT_POSITION_SLUGOUT_Y
    )
    utils.makePlayerInventorySlots(this.playerInventory, 8, texmodel.LargeCombinerPanelView.PLAYER_INVENTORY_START_Y) foreach this.addSlot

    private[this] final val pd = if (this.blockEntity.getWorld.isClient) {
      new PropertyDelegate {
        var processTime: Int = 0
        var processFullTime: Int = 0
        var energyLeft: Int = 0
        var energyFull: Int = 0

        override final val size = PD_INDEX_ENERGY_FULL + 1

        override def get(index: Int) = index match {
          case PD_INDEX_PROCESS_TIME => this.processTime
          case PD_INDEX_PROCESS_FULL => this.processFullTime
          case PD_INDEX_ENERGY_LEFT => this.energyLeft
          case PD_INDEX_ENERGY_FULL => this.energyFull
          case _ => 0
        }
        override def set(index: Int, value: Int) = index match {
          case PD_INDEX_PROCESS_TIME => { this.processTime = value }
          case PD_INDEX_PROCESS_FULL => { this.processFullTime = value }
          case PD_INDEX_ENERGY_LEFT => { this.energyLeft = value }
          case PD_INDEX_ENERGY_FULL => { this.energyFull = value }
          case _ => ()
        }
      }
    } else {
      new PropertyDelegate {
        override final val size = PD_INDEX_ENERGY_FULL + 1

        override def get(index: Int) = index match {
          case PD_INDEX_PROCESS_TIME => Container.this.blockEntity.getProcessTime
          case PD_INDEX_PROCESS_FULL => Container.this.blockEntity.getProcessFullTime
          case PD_INDEX_ENERGY_LEFT => Container.this.blockEntity.getEnergyCell.left
          case PD_INDEX_ENERGY_FULL => Container.this.blockEntity.getEnergyCell.max
          case _ => 0
        }
        override def set(index: Int, value: Int) = 
          throw new IllegalArgumentException(f"Cannot set property in server-side object: ${index} <- ${value}")
      }
    }
    this addProperties pd
    @Environment(EnvType.CLIENT)
    final def processRate = {
      val total = this.pd get PD_INDEX_PROCESS_FULL
      // System.out.println("processRate!" + total + "/" + blockEntity.propertyDelegate.get(PD_INDEX_PROCESS_TIME))
      if (total == 0) 0.0f else this.pd.get(PD_INDEX_PROCESS_TIME).asInstanceOf[Float] / total.asInstanceOf[Float]
    }
    @Environment(EnvType.CLIENT)
    final def energyRate = {
      val total = this.pd get PD_INDEX_ENERGY_FULL
      if (total == 0) 0.0f else this.pd.get(PD_INDEX_ENERGY_LEFT).asInstanceOf[Float] / total.asInstanceOf[Float]
    }

    override def canUse(player: PlayerEntity) = this.inventory canPlayerUseInv player
    /**
     * Shift+クリックの動作
     *
     * @param player 操作プレイヤー
     * @param slotIndex 操作対象スロット
     */
    override def transferSlot(player: PlayerEntity, slotIndex: Int): ItemStack = {
      Option(this.slots get slotIndex) filter (_.hasStack) flatMap {
        slot => if (slotIndex < this.inventory.getInvSize) {
          /// Machine Local Slots -> Player Inventory Slots
          this.tryTransferSlot(slot, this.inventory.getInvSize, this.slots.size, true)
        } else {
          /// Player Inventory Slots -> Machine Input Slot
          this.tryTransferSlot(slot, 0, 1, false)
        }
      } getOrElse ItemStack.EMPTY
    }

    /**
      * スロットグループへのアイテム転送(Shift+クリック)
      *
      * @param orgSlot 元スロット
      * @param targetSlotIndexStart 対象スロットグループの最初のインデックス(Inclusive)
      * @param targetSlotIndexEnd 対象スロットグループの最後のインデックス(Exclusive)
      * @param tryFromLast 後ろから試す場合(Player Inventoryに対しての場合とか)はtrue
      * @return 転送に成功した場合は元スロットのItemStack、失敗したらNone
      */
    private[this] final def tryTransferSlot(orgSlot: Slot, targetSlotIndexStart: Int, targetSlotIndexEnd: Int, tryFromLast: Boolean) = {
      val orgStack = orgSlot.getStack
      val newStack = orgStack.copy()

      val inserted = this.insertItem(orgStack, targetSlotIndexStart, targetSlotIndexEnd, tryFromLast)
      if (inserted) {
        if (orgStack.isEmpty) orgSlot.setStack(ItemStack.EMPTY) else orgSlot.markDirty()
        Some(newStack) filter (_.getCount() != orgStack.getCount())
      } else {
        None
      }
    }
  }

  @Environment(EnvType.CLIENT)
  object PanelView {
    final val TITLE = new TranslatableText("view.title.large-combiner")
  }
  @Environment(EnvType.CLIENT)
  final class PanelView(
    private[this] val container: Container,
    playerInventory: PlayerInventory
  ) extends ContainerScreen[Container](container, playerInventory, PanelView.TITLE) {
    this.containerWidth = texmodel.LargeCombinerPanelView.PANEL_WIDTH
    this.containerHeight = texmodel.LargeCombinerPanelView.PANEL_HEIGHT

    override def render(mouseX: Int, mouseY: Int, delta: Float) = {
      // ちゃんとオーバーライドしてrenderBackgroundしてあげないと背景が暗くなってくれないらしい(なんだそれは)
      this.renderBackground()
      super.render(mouseX, mouseY, delta)
      this.drawMouseoverTooltip(mouseX, mouseY)
    }
    override def drawForeground(mouseX: Int, mouseY: Int) = {
      this.font.drawStringCentric(this.title.asFormattedString, this.containerWidth / 2.0f, 6.0f, constants.PRIMARY_VIEW_TEXT_COLOR);
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
        this.container.processRate
      )

      texmodel.LargeCombinerPanelView.blitEnergyCellBase(
        this,
        viewOriginX + texmodel.LargeCombinerPanelView.EC_OVERLAY_X,
        viewOriginY + texmodel.LargeCombinerPanelView.EC_OVERLAY_Y
      )
    }
  }
}
