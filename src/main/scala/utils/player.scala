package jp.ct2.mcfm115.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.item.{ItemStack, Items}
import net.minecraft.sound.{SoundEvents, SoundCategory}

package object player {
  implicit final class PlayerOpHelper(final val p: PlayerEntity) extends AnyVal {
    final def consumeHandheldBucketAt(pos: BlockPos, hand: Hand) = {
      if (!this.p.abilities.creativeMode) {
        this.p.setStackInHand(hand, new ItemStack(Items.BUCKET))
      }
      this.p.world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f)
    }
  }
}
