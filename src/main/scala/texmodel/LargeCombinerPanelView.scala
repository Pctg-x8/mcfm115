package com.cterm2.mcfm115.texmodel

import com.cterm2.mcfm115.Mod
import net.minecraft.client.gui.DrawableHelper
import net.fabricmc.api.{Environment, EnvType}

@Environment(EnvType.CLIENT)
object LargeCombinerPanelView {
    final val PANEL_WIDTH = 176
    final val PANEL_HEIGHT = 173
    final val PLAYER_INVENTORY_START_Y = 91
    final val ARROW_SIZE = 20
    final val ARROW_OVERLAY_X = 78
    final val ARROW_OVERLAY_Y = 36
    final val EC_OVERLAY_X = 86
    final val EC_OVERLAY_Y = 37
    final val EC_WIDTH = 4
    final val EC_HEIGHT = 18
    final val SLOT_POSITION_INGREDIENT_X = 55
    final val SLOT_POSITION_OUTPUT_X = 105
    final val SLOT_POSITION_PROCESSLINE_Y = 38
    final val SLOT_POSITION_EC_X = 80
    final val SLOT_POSITION_EC_Y = 17
    final val SLOT_POSITION_SLUGOUT_X = 105
    final val SLOT_POSITION_SLUGOUT_Y = 64

    final val MAIN_TEXTURE = Mod makeIdentifier "textures/view/container/large-combiner.png"

    def blitMainPanel(renderer: DrawableHelper, x: Int, y: Int) =
        renderer.blit(x, y, 0, 0, PANEL_WIDTH, PANEL_HEIGHT)
    def blitProgressArrowOverlay(renderer: DrawableHelper, x: Int, y: Int, rate: Float) =
        renderer.blit(x, y, PANEL_WIDTH + 1, 0, (ARROW_SIZE * rate).asInstanceOf[Int], ARROW_SIZE)
    def blitEnergyCellBase(renderer: DrawableHelper, x: Int, y: Int) =
        renderer.blit(x, y, EC_OVERLAY_X, EC_OVERLAY_Y, EC_WIDTH, EC_HEIGHT)
}
