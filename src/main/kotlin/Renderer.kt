import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import kotlin.math.floor

/**
 * CHIP-8 Renderer emulation
 */
class Renderer(scale: Int) {
    val cols = 64
    val rows = 32
    val scale = if (scale > 0) scale else Configuration().scale

    internal var display = MutableList(cols * rows) { 0 }

    fun setPixel(x: Int, y: Int): Boolean {
        val pixelLoc = (x % cols) + ((y % rows) * cols)
        display[pixelLoc] = display[pixelLoc] xor 1

        return display[pixelLoc] == 0
    }

    fun getPixel(x: Int, y: Int): Int {
        return display[(x % cols) + ((y % rows) * cols)]
    }

    fun clear() {
        display = MutableList(cols * rows) { 0 }
    }

    fun render(drawer: Drawer) {
        (0 until display.size).forEach {
            val x = (it.toDouble() % cols) * scale
            val y = floor(it.toDouble() / cols) * scale

            drawer.fill = if (display[it] == 1) ColorRGBa.WHITE else ColorRGBa.BLACK
            drawer.rectangle(x, y, scale.toDouble(), scale.toDouble())
        }
    }

    fun testRender() {
        setPixel(0, 0)
        setPixel(0, 31)
        setPixel(31, 15)
        setPixel(63, 0)
        setPixel(63, 31)
    }
}