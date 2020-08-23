package jp.ct2.mcfm115.items

import jp.ct2.mcfm115.{Mod, constants}
import net.minecraft.util.registry.Registry

object PolymerizedEnergyFluidPack {
  final val ID = Mod makeIdentifier "polymerized-energy-fluid-pack"
  final val HID = Mod makeIdentifier "high-polymerized-energy-fluid-pack"
  final def register() = {
    Registry.register(Registry.ITEM, ID, ITEM_OBJECT)
    Registry.register(Registry.ITEM, HID, ITEM_OBJECT_HIGH)
  }

  /**
    * Polymerized EnergyFluid Pack(持ち運び可能なエネルギー蓄電池)
    * @param containment 最大貯蓄量(mB)
    */
  class Item(containment: Int) extends net.minecraft.item.Item(new net.minecraft.item.Item.Settings() group Mod.ITEM_GROUP_MAIN maxDamage containment) {

  }
  final val ITEM_OBJECT = new Item(constants.POLYMERIZED_EF_PACK_AMOUNT)
  final val ITEM_OBJECT_HIGH = new Item(constants.HIGH_POLYMERIZED_EF_PACK_AMOUNT)
}
