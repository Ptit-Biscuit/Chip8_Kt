import org.openrndr.application
import org.openrndr.color.ColorRGBa
import java.io.File
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer
import kotlin.math.floor
import kotlin.random.Random

data class Configuration(
    var pcStart: Int = 0x200,
    var scale: Int = 16,
    var keyboardLayout: KeyboardLayouts = KeyboardLayouts.EN
)

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
    var i: Int = 0,
    var pc: Int = Configuration().pcStart,
    var sp: Int = 0,
    val vx: UByteArray = UByteArray(16),
    var delayTimer: UByte = 0u,
    var soundTimer: UByte = 0u,
    val stack: UIntArray = UIntArray(16)
)

data class Screen(
    val cols: Int = 64,
    val rows: Int = 32,
    var scale: Int = Configuration().scale,
    val display: MutableList<Int> = MutableList(cols * rows) { 0 }
)

@ExperimentalUnsignedTypes
class Chip8 internal constructor() {
    private val configuration = Configuration()

    /** 4KB of memory **/
    internal val memory = UByteArray(4096)
    internal val cpu = CPU()
    internal var keyboard = configuration.keyboardLayout
    internal var screen = Screen()

    var mute = false
    var pause = false
    var synth: Synthesizer = MidiSystem.getSynthesizer()

    fun configure(init: Configuration.() -> Unit) {
        configuration.apply { init() }
        cpu.pc = configuration.pcStart
        keyboard = configuration.keyboardLayout
        screen.scale = configuration.scale
    }

    fun emulate(opcode: Int) {
        cpu.pc += 2
        val x = (opcode and 0x0F00) shr 8
        val y = (opcode and 0x00F0) shr 4

        when (opcode and 0xF000) {
            0x0000 -> when (opcode) {
                0x00E0 -> screen.display.fill(0) // CLS
                0x00EE -> cpu.pc = cpu.stack[cpu.sp--].toInt() // RET
            }
            0x1000 -> cpu.pc = opcode and 0x0FFF // JP
            0x2000 -> { // CALL
                cpu.stack[++cpu.sp] = cpu.pc.toUInt()
                cpu.pc = opcode and 0x0FFF
            }
            0x3000 -> cpu.pc += if (cpu.vx[x].toInt() == opcode and 0x00FF) 2 else 0 // SE_VX_BYTE
            0x4000 -> cpu.pc += if (cpu.vx[x].toInt() != opcode and 0x00FF) 2 else 0 // SNE_VX_BYTE
            0x5000 -> cpu.pc += if (cpu.vx[x] == cpu.vx[y]) 2 else 0 // SE_VX_VY
            0x6000 -> cpu.vx[x] = (opcode and 0x00FF).toUByte() // LD_VX_BYTE
            0x7000 -> cpu.vx[x] = (cpu.vx[x] + ((opcode and 0x00FF).toUByte())).toUByte() // ADD_VX_BYTE
            0x8000 -> when (opcode and 0x000F) {
                0x0000 -> cpu.vx[x] = cpu.vx[y] // LD_VX_VY
                0x0001 -> cpu.vx[x] = cpu.vx[x] or cpu.vx[y] // OR_VX_VY
                0x0002 -> cpu.vx[x] = cpu.vx[x] and cpu.vx[y] // AND_VX_VY
                0x0003 -> cpu.vx[x] = cpu.vx[x] xor cpu.vx[y] // XOR_VX_VY
                0x0004 -> { // ADD_VX_VY
                    val sum = cpu.vx[x] + cpu.vx[y]

                    cpu.vx[0xF] = if (sum > 0xFFu) 0x01u else 0x00u
                    cpu.vx[x] = sum.toUByte()
                }
                0x0005 -> { // SUB_VX_VY
                    cpu.vx[0xF] = if (cpu.vx[x] > cpu.vx[y]) 0x01u else 0x00u
                    cpu.vx[x] = (cpu.vx[x] - cpu.vx[y]).toUByte()
                }
                0x0006 -> { // SHR_VX_VY
                    cpu.vx[0xF] = cpu.vx[x] and 0x01u
                    cpu.vx[x] = (cpu.vx[x].toInt() shr 1).toUByte()
                }
                0x0007 -> { // SUBN_VX_VY
                    cpu.vx[0xF] = if (cpu.vx[y] > cpu.vx[x]) 0x01u else 0x00u
                    cpu.vx[x] = (cpu.vx[y] - cpu.vx[x]).toUByte()
                }
                0x000E -> { // SHL_VX_VY
                    cpu.vx[0xF] = ((cpu.vx[x] and 0x80u).toInt() shr 7).toUByte()
                    cpu.vx[x] = (cpu.vx[x].toInt() shl 1).toUByte()
                }
            }
            0x9000 -> cpu.pc += if (cpu.vx[x] != cpu.vx[y]) 2 else 0 // SNE_VX_VY
            0xA000 -> cpu.i = opcode and 0x0FFF // LD_I_ADDR
            0xB000 -> cpu.pc = (opcode and 0x0FFF) + cpu.vx[0].toInt() // JP_V0_ADDR
            0xC000 -> cpu.vx[x] = Random.nextInt(opcode and 0x00FF).toUByte() // RND_VX_BYTE
            0xD000 -> { // DRW_VX_VY_NIBBLE
                cpu.vx[0xF] = 0x00u

                (0 until (opcode and 0x000F)).forEach { row ->
                    var sprite = memory[cpu.i + row]
                    (0 until 8).forEach { col ->
                        if (sprite and 0x80u > 0x00u) {
                            val pixelX = cpu.vx[x].toInt() + col % screen.cols
                            val pixelY = cpu.vx[y].toInt() + row % screen.rows
                            val pixelLoc = pixelX + pixelY * screen.cols

                            screen.display[pixelLoc] = screen.display[pixelLoc] xor 1

                            if (screen.display[pixelLoc] == 0)
                                cpu.vx[0xF] = 0x01u
                        }

                        sprite = (sprite.toInt() shl 1).toUByte()
                    }
                }
            }
            0xE000 -> when (opcode and 0x00FF) {
                0x009E -> cpu.pc += if (keyboard.keyPressed == cpu.vx[x].toInt()) 2 else 0 // SKP_VX
                0x00A1 -> cpu.pc += if (keyboard.keyPressed != cpu.vx[x].toInt()) 2 else 0 // SKNP_VX
            }
            0xF000 -> when (opcode and 0x00FF) {
                0x0007 -> cpu.vx[x] = cpu.delayTimer // LD_VX_DT
                0x000A -> { // LD_VX_K
                    if (keyboard.keyPressed != -1)
                        cpu.vx[x] = keyboard.keyPressed.toUByte()
                    else
                        cpu.pc -= 2
                }
                0x0015 -> cpu.delayTimer = cpu.vx[x] // LD_DT_VX
                0x0018 -> cpu.soundTimer = cpu.vx[x] // LD_ST_VX
                0x001E -> cpu.i += cpu.vx[x].toInt() // ADD_I_VX
                0x0029 -> cpu.i = (cpu.vx[x] * 0x05u).toInt() // LD_F_VX
                0x0033 -> { // LD_B_VX
                    memory[cpu.i] = (cpu.vx[x].toInt() / 100).toUByte()
                    memory[cpu.i + 1] = ((cpu.vx[x].toInt() % 100) / 10).toUByte()
                    memory[cpu.i + 2] = (cpu.vx[x].toInt() % 10).toUByte()
                }
                0x0055 -> { // LD_I_VX
                    (0..x).forEach { register -> memory[cpu.i + register] = cpu.vx[register] }
                    cpu.i += x
                }
                0x0065 -> { // LD_VX_I
                    (0..x).forEach { register -> cpu.vx[register] = memory[cpu.i + register] }
                    cpu.i += x
                }
            }
        }
    }

    fun run(rom: String) {
        // Basics 5 bytes sprites stored in the interpreter section of memory starting at 0x00
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

        val romFile = File(rom)
        if (romFile.isFile) {
            romFile.readBytes().forEachIndexed { index, byte ->
                // Most Chip-8 programs start at 0x200 (512) but some can start at 0x600 (1536)
                memory[configuration.pcStart + index] = byte.toUByte()
            }

            synth.open() // Activate sound before showing GUI

            // Setting GUI application
            application {
                configure {
                    width = screen.cols * screen.scale
                    height = screen.rows * screen.scale
                }

                program {
                    keyboard.keyUp.listen { this@Chip8.keyboard.keyPressed = -1 }
                    keyboard.keyDown.listen {
                        this@Chip8.keyboard.keyPressed = this@Chip8.keyboard.mapper[it.name] ?: -1

                        when (it.name) {
                            this@Chip8.keyboard.controls["mute"] -> mute = !mute
                            this@Chip8.keyboard.controls["pause"] -> pause = !pause
                            this@Chip8.keyboard.controls["quit"] -> application.exit()
                        }
                    }

                    extend {
                        if (!pause)
                            emulate((memory[cpu.pc].toInt() shl 8) or memory[cpu.pc + 1].toInt())

                        if (frameCount % 60 == 0) {
                            if (cpu.delayTimer > 0u)
                                cpu.delayTimer--

                            if (cpu.soundTimer > 0u)
                                cpu.soundTimer--
                        }

                        if (!mute and (cpu.soundTimer > 0u))
                            synth.channels[0].noteOn(60, 127)
                        else
                            synth.channels[0].noteOff(60, 127)

                        // Render emulated frame
                        (0 until screen.display.size).forEach {
                            val x = (it.toDouble() % screen.cols) * screen.scale
                            val y = floor(it.toDouble() / screen.cols) * screen.scale

                            drawer.fill = if (screen.display[it] == 1) ColorRGBa.WHITE else ColorRGBa.BLACK
                            drawer.rectangle(x, y, screen.scale.toDouble(), screen.scale.toDouble())
                        }
                    }
                }
            }

            synth.close() // Deactivate sound after GUI has been closed
        }
    }
}

@ExperimentalUnsignedTypes
fun chip8(build: Chip8.() -> Unit) {
    Chip8().apply { build() }
}