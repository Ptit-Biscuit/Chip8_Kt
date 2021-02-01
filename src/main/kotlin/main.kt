import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

@ExperimentalUnsignedTypes
fun main() = application {
    program {
        val font = loadFont("src/main/resources/VCR_OSD_MONO_1.001.ttf", 20.0)
        val roms = File("roms/").list() ?: arrayOf("")

        val titles = roms.toMutableList()
        titles.addAll(arrayOf("", "quit"))

        val cells = ceil(sqrt(titles.size.toDouble()))
        var titleIndex = 0

        keyboard.keyDown.listen {
            titleIndex += when (it.key) {
                KEY_ARROW_DOWN -> if (titleIndex < titles.size - cells) cells.toInt() else 0
                KEY_ARROW_UP -> if (titleIndex >= cells) -cells.toInt() else 0
                KEY_ARROW_LEFT -> if (titleIndex > 0) -1 else 0
                KEY_ARROW_RIGHT -> if (titleIndex < titles.size - 1) 1 else 0
                else -> 0
            }

            if (titleIndex == titles.size - 2) {
                titleIndex += when (it.key) {
                    KEY_ARROW_DOWN -> -cells.toInt()
                    KEY_ARROW_LEFT -> -1
                    KEY_ARROW_RIGHT -> 1
                    else -> 0
                }
            }

            if (it.key == KEY_ENTER) {
                if (titleIndex == titles.size - 1)
                    application.exit()

                GlobalScope.launch {
                    chip8 {
                        configure {
                            keyboardLayout = KeyboardLayouts.FR
                        }

                        run("roms/${titles[titleIndex]}")
                    }
                }
            }
        }

        extend {
            drawer.fontMap = font

            titles.forEachIndexed { index, title ->
                drawer.fill = if (index != titleIndex) ColorRGBa.WHITE else ColorRGBa.GREEN
                drawer.text(
                    title,
                    width / cells * ((index % cells) + 0.1),
                    height / cells * (floor(index / cells) + 0.5)
                )
            }
        }
    }
}
