package com.cterm2.mcfm115.machines

import com.cterm2.mcfm115.Mod
import com.cterm2.mcfm115.{utils, constants, texmodel, items}
import com.cterm2.mcfm115.common.GenericIOSides
import com.cterm2.mcfm115.utils.SerializeHelper._
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

  final case class Recipe(final val input: Ingredient, final val output: ItemStack, final val processTime: Int) extends IBaseRecipe[BlockEntity] {
    override def matches(inv: BlockEntity, world: World) = this.input test inv.getInvStack(0)
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

      Recipe(ingredient, output, processTime)
    }

    override def read(id: Identifier, buf: PacketByteBuf) = {
      val ingredient = Ingredient fromPacket buf
      val output = buf.readItemStack()
      val processTime = buf.readVarInt()

      Recipe(ingredient, output, processTime)
    }
    override def write(buf: PacketByteBuf, recipe: Recipe) = {
      recipe.input write buf
      buf writeItemStack recipe.output
      buf writeVarInt recipe.processTime
    }
  }

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
    private[this] var ignoreInputEvents = false
    private[this] val inventory = DefaultedList.ofSize[ItemStack](this.getInvSize, ItemStack.EMPTY)
    private[this] val ec = new ContainedEnergyCell(128 * 1000)
    private[this] var currentProcessingRecipe: Option[Recipe] = None

    private[this] var processTime: Int = 0

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

    private def findMatchingRecipe() = this.world.getRecipeManager.getFirstMatch(RECIPE_TYPE, this, this.world).toScala

    private final def onInputChanged(newItem: ItemStack) = {
      val oldRecipe = this.currentProcessingRecipe
      System.out.println("onInputChanged! "+this.world)
      this.currentProcessingRecipe = this.findMatchingRecipe()
      if (oldRecipe != this.currentProcessingRecipe) {
        this.processTime = 0
        this.markDirty()
      }
    }

    override def tick() = {
      for (Recipe(_, o, time) <- this.currentProcessingRecipe) {
        this.processTime += 1
        if (this.processTime >= time) {
          this.processTime -= time
          System.out.println("Processed!")
        }
        this.markDirty()
      }
    }

    final def slotItemInput = this.inventory get INVENTORY_INDEX_INPUT
    final def processProgress: Float = this.currentProcessingRecipe map { r => this.processTime.asInstanceOf[Float] / r.processTime.asInstanceOf[Float] } getOrElse 0.0f

    override def clear() = { this.inventory.clear() }
    override def canPlayerUseInv(player: PlayerEntity) = utils.distanceFromBlockCentric(player, this.pos) <= constants.PLAYER_DISTANCE_CONTAINER_USABLE_THRESHOLD
    override def getInvStack(slot: Int) = this.inventory.get(slot)
    override val isInvEmpty = this.inventory.stream.allMatch(i => i.isEmpty)
    override def removeInvStack(slot: Int) = Inventories.removeStack(this.inventory, slot)
    override def setInvStack(slot: Int, stack: ItemStack) = {
      this.inventory.set(slot, stack)
      if (!this.world.isClient && !this.ignoreInputEvents && slot == INVENTORY_INDEX_INPUT) {
        this.onInputChanged(this.inventory get slot)
      }
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
      this.ignoreInputEvents = true

      super.fromTag(tag)
      this.inventory.clear()
      Inventories.fromTag(tag, this.inventory)
      this.ec fromTag tag
      this.processTime = tag getInt TAG_KEY_PROCESS_TIME

      this.ignoreInputEvents = false
    }
  }
  
  final class Container(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val inventory: Inventory,
    val blockEntity: BlockEntity
  ) extends net.minecraft.container.Container(null, syncId) {
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
        val inserted = if (slotIndex < this.inventory.getInvSize) {
          this.insertItem(orgStack, this.inventory.getInvSize, this.slots.size, true)
        } else {
          this.insertItem(orgStack, 0, this.inventory.getInvSize, false)
        }
        if (!inserted) return ItemStack.EMPTY

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
    private[this] val container: Container,
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
        this.container.blockEntity.processProgress
      )

      texmodel.LargeCombinerPanelView.blitEnergyCellBase(
        this,
        viewOriginX + texmodel.LargeCombinerPanelView.EC_OVERLAY_X,
        viewOriginY + texmodel.LargeCombinerPanelView.EC_OVERLAY_Y
      )
    }
  }
}
