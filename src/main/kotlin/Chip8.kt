import org.openrndr.application
import java.io.File
import javax.sound.midi.MidiSystem

@ExperimentalUnsignedTypes
class Chip8 internal constructor(configuration: Configuration) {
    /** 4KB of memory **/
    internal val memory = UByteArray(4096)

    internal val cpu: CPU = CPU()
    internal val keyboard = Keyboard()
    internal val renderer = Renderer(configuration.scale)

    private val synth = MidiSystem.getSynthesizer()

    fun run(rom: String) {
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

        synth.open()

        application {
            configure {
                width = renderer.cols * renderer.scale
                height = renderer.rows * renderer.scale
            }

            program {
                keyboard.keyDown.listen(this@Chip8.keyboard::onKeyDown)
                keyboard.character.listen {
                    if (it.character == 'p') {
                        renderer.clear()
                        cpu.reset()
                    }
                }
                keyboard.keyUp.listen(this@Chip8.keyboard::onKeyUp)

                extend {
                    cpu.cycle(this@Chip8)
                    renderer.render(drawer)

                    if (frameCount % 60 == 0) {
                        if (cpu.delayTimer > 0u) {
                            cpu.delayTimer = cpu.delayTimer.dec()
                        }

                        if (cpu.soundTimer > 0u) {
                            cpu.soundTimer = cpu.soundTimer.dec()
                            synth.channels[0].noteOn(60, 127)
                        } else {
                            synth.channels[0].noteOff(60, 127)
                        }
                    }
                }
            }
        }

        synth.close()
    }
}

class Chip8Builder internal constructor() {
    private val configuration = Configuration()

    fun configure(conf: Configuration.() -> Unit) {
        configuration.apply { conf() }
    }

    @ExperimentalUnsignedTypes
    fun run(rom: String) {
        Chip8(configuration).run(rom)
    }
}

@ExperimentalUnsignedTypes
fun chip8(build: Chip8Builder.() -> Unit) {
    Chip8Builder().apply { build() }
}