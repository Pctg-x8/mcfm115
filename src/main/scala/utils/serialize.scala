package com.cterm2.mcfm115.utils

import com.google.gson.JsonObject
import net.minecraft.util.JsonHelper
import net.minecraft.util.registry.Registry
import net.minecraft.util.Identifier
import com.google.gson.JsonSyntaxException
import com.google.gson.JsonParseException
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient

package object SerializeHelper {
  implicit final class JsonDeserializeHelper(private val obj: JsonObject) extends AnyVal {
    final def getObject(key: String) = JsonHelper.getObject(this.obj, key)
    final def getString(key: String) = JsonHelper.getString(this.obj, key)
    final def getIntOpt(key: String) = if (this.obj has key) Some(JsonHelper.asInt(this.obj.get(key), key)) else None
    final def getInt(key: String) = JsonHelper.getInt(this.obj, key)

    final def getItemStack(key: String) = deserializeItemStack(this getObject key)
    final def getIngredient(key: String) = Ingredient.fromJson(this getObject key)
  }
  
  final def deserializeItemStack(json: JsonObject) = {
    val itemName = json getString "item"
    val count = json getIntOpt "count" getOrElse 1

    val item = Registry.ITEM getOrEmpty new Identifier(itemName) orElseThrow {
      () => new JsonSyntaxException(f"Unknown Item '${itemName}'")
    }
    new ItemStack(item, count)
  }
}
