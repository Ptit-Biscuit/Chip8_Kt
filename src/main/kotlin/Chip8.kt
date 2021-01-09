import org.openrndr.application
import org.openrndr.color.ColorRGBa
import java.io.File

@ExperimentalUnsignedTypes
class Chip8 {
    /** 4KB of memory **/
    internal val memory = UByteArray(4096)

    internal var cpu: CPU = CPU()
    internal lateinit var renderer: Renderer
    internal lateinit var keyboard: Keyboard

    private lateinit var configuration: Configuration

    fun setup(configuration: Configuration, rom: String) {
        this.configuration = configuration
        keyboard = Keyboard()
        renderer = Renderer(configuration.scale)

        // Array of hex values for each sprite. Each sprite is 5 bytes.
        // Sprites are stored in the interpreter section of memory starting at 0x00
        listOf(
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        ).forEachIndexed { index, sprite ->
            memory[index] = sprite.toUByte()
        }

        // Load ROM into memory if it exists
        val romFile = File("roms/$rom")
        if (romFile.isFile) {
            romFile.readBytes().forEachIndexed { index, byte ->
                memory[512 + index] = byte.toUByte()
            }
        }
    }

    fun run() {
        if (configuration.headless) {
            cpu.cycle(memory, renderer)
        } else {
            application {
                configure {
                    width = renderer.cols * renderer.scale
                    height = renderer.rows * renderer.scale
                }

                program {
                    keyboard.keyDown.listeners.add(this@Chip8.keyboard::onKeyDown)
                    keyboard.keyUp.listeners.add(this@Chip8.keyboard::onKeyUp)

                    drawer.stroke = ColorRGBa.WHITE
                    drawer.fill = ColorRGBa.WHITE

                    extend {
                        cpu.cycle(memory, renderer)
                        renderer.render(drawer)
                    }
                }
            }
        }
    }
}