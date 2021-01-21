import org.openrndr.application
import java.io.File
import javax.sound.midi.MidiSystem

@ExperimentalUnsignedTypes
class Chip8 internal constructor(configuration: Configuration) {
    /** 4KB of memory **/
    internal val memory = UByteArray(4096)

    /** CPU emulation **/
    internal val cpu: CPU = CPU()

    /** Keyboard mapper **/
    internal val keyboard = Keyboard()

    /** Graphics renderer **/
    internal val renderer = Renderer(configuration.scale)

    /** MIDI sound synthesizer **/
    private val synth = MidiSystem.getSynthesizer()

    /** Pause the emulation **/
    private var paused = false

    /** Mute the emulation **/
    private var muted = false

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
        ).forEachIndexed { index, byte ->
            memory[index] = byte.toUByte()
        }

        // Load ROM into memory if it exists.
        // Most CHIP-8 programs start at 0x200 (512)
        val romFile = File("roms/$rom")
        if (romFile.isFile) {
            romFile.readBytes().forEachIndexed { index, byte ->
                memory[512 + index] = byte.toUByte()
            }
        }

        // Activate sound before showing GUI
        synth.open()

        // Setting GUI application
        application {
            configure {
                width = renderer.cols * renderer.scale
                height = renderer.rows * renderer.scale
            }

            program {
                // Prepare keyboard listeners
                keyboard.keyDown.listen(this@Chip8.keyboard::onKeyDown)
                keyboard.keyDown.listen {
                    // Add 'p' to pause
                    if (it.name == "p") {
                        paused = !paused
                    }

                    // Add 'm' to mute
                    if (it.name == "m") {
                        muted = !muted
                    }

                    // Add 'escape' to reset rom
                    if (it.name == "escape") {
                        renderer.clear()
                        cpu.reset()
                    }
                }
                keyboard.keyUp.listen(this@Chip8.keyboard::onKeyUp)

                extend {
                    // Run cpu cycle if not paused
                    if (!paused) {
                        cpu.cycle(this@Chip8)
                    }

                    // Every 60 frames we decrease timers
                    if (frameCount % 60 == 0) {
                        if (cpu.delayTimer > 0u) {
                            cpu.delayTimer = cpu.delayTimer.dec()
                        }

                        if (cpu.soundTimer > 0u) {
                            cpu.soundTimer = cpu.soundTimer.dec()
                        }
                    }

                    // Render emulated frame
                    renderer.render(drawer)

                    // Play a note if not muted and sound timer is greater than 0
                    if (!muted and (cpu.soundTimer > 0u)) {
                        synth.channels[0].noteOn(60, 127)
                    } else {
                        synth.channels[0].noteOff(60, 127)
                    }
                }
            }
        }

        // Deactivate sound
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