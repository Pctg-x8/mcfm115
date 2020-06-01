package com.cterm2.mcfm115.utils

import net.minecraft.client.font.TextRenderer

package object drawHelper {
    def drawStringCentric(renderer: TextRenderer, text: String, x: Float, y: Float, color: Int) = {
        val sw = renderer getStringWidth text
        renderer.draw(text, x - sw / 2.0f, y, color)
    }
}
