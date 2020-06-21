package jp.ct2.mcfm115.utils

import net.minecraft.client.font.TextRenderer

package object TextRendererExt {
  implicit final class TextRendererExtWrapper(val renderer: TextRenderer) extends AnyVal {
    final def drawStringCentric(text: String, x: Float, y: Float, color: Int) = {
      val sw = renderer getStringWidth text
      this.renderer.draw(text, x - sw / 2.0f, y, color)
    }
  }
}
