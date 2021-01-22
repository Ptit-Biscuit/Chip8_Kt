import org.openrndr.application
import org.openrndr.color.ColorRGBa
import java.io.File
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer
import kotlin.math.floor

data class Configuration(var scale: Int = 16, var keyboardLayout: KeyboardLayouts = KeyboardLayouts.EN)

/**
 * __Indexes__
 * - `i`: Memory address - _16-bit_
 * - `pc`: Program counter - _16-bit_
 * - `sp`: Stack pointer - _16-bit_
 *
 * __Registers__
 * - `vx`: 16 addresses for general purpose (V0 to VF) - _8-bit_
 * - `delayTimer`: Delay timer - _8-bit_
 * - `soundTimer`: Sound timer - _8-bit_
 *
 * __Stack__
 * - `stack`: 16 addresses for stack - _16-bit_
 */
@ExperimentalUnsignedTypes
data class CPU(
    var i: UInt = 0u,
    var pc: UInt = 0x0200u,
    var sp: UInt = 0u,
    val vx: UByteArray = UByteArray(16),
    var delayTimer: UByte = 0u,
    var soundTimer: UByte = 0u,
    val stack: UIntArray = UIntArray(16)
)

data class Renderer(
    val cols: Int = 64,
    val rows: Int = 32,
    var scale: Int = Configuration().scale,
    val display: MutableList<Int> = MutableList(cols * rows) { 0 }
)

@ExperimentalUnsignedTypes
class Chip8 internal constructor() {
    /** 4KB of memory **/
    internal val memory = UByteArray(4096)

    internal val cpu = CPU()
    internal var keyboard = KeyboardLayouts.EN
    internal var renderer = Renderer()

    /** Pause the emulation **/
    var paused = false

    /** Mute the emulation **/
    var muted = false

    /** MIDI sound synthesizer **/
    var synth: Synthesizer = MidiSystem.getSynthesizer()

    fun configure(init: Configuration.() -> Unit) {
        val configuration = Configuration().apply { init() }
        keyboard = configuration.keyboardLayout
        renderer.apply { scale = configuration.scale }
    }

    fun emulate(opcode: UInt) {
        cpu.pc += 2u
        Instructions.values().find { opcode and it.mask == it.pattern }?.operation?.invoke(opcode, this)
    }

    fun run(rom: String) {
        // Array of hex values for each 5 bytes sprite
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

        // Load ROM into memory if it exists
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
                keyboard.keyUp.listen { this@Chip8.keyboard.keyPressed = -1 }
                keyboard.keyDown.listen {
                    this@Chip8.keyboard.mapper[it.name]?.let { mappedKey -> this@Chip8.keyboard.keyPressed = mappedKey }

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
                        renderer.display.fill(0)

                        cpu.vx.fill(0x00u)
                        cpu.i = 0u
                        cpu.pc = 0x0200u
                        cpu.sp = 0u
                        cpu.stack.fill(0x0000u)
                        cpu.delayTimer = 0u
                        cpu.soundTimer = 0u
                    }
                }

                extend {
                    // Run cpu cycle if not paused
                    if (!paused) {
                        emulate((memory[cpu.pc.toInt()].toUInt() shl 8) or memory[cpu.pc.toInt() + 1].toUInt())
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
                    (0 until renderer.display.size).forEach {
                        val x = (it.toDouble() % renderer.cols) * renderer.scale
                        val y = floor(it.toDouble() / renderer.cols) * renderer.scale

                        drawer.fill = if (renderer.display[it] == 1) ColorRGBa.WHITE else ColorRGBa.BLACK
                        drawer.rectangle(x, y, renderer.scale.toDouble(), renderer.scale.toDouble())
                    }

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

@ExperimentalUnsignedTypes
fun chip8(build: Chip8.() -> Unit) {
    Chip8().apply { build() }
}