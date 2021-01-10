import kotlin.math.floor
import kotlin.random.Random

/**
 * CHIP-8 CPU emulation
 */
@ExperimentalUnsignedTypes
class CPU {
    /** General purpose 8-bit registers (referred to as 'Vx' - x being hex value 0 to F) **/
    internal val vx = UByteArray(16)

    /** Memory address 16-bit register **/
    internal var i: UShort = 0u

    /** Program counter 16-bit register **/
    internal var pc: UShort = 512u

    /** Stack pointer 16-bit register **/
    internal var sp: UShort = 0u

    /** Stack 16-bit registers **/
    internal val stack = UShortArray(16)

    /** Delay timer 8-bit register **/
    internal var delayTimer: UByte = 0u

    /** Sound timer 8-bit register **/
    internal var soundTimer: UByte = 0u

    fun cycle(memory: UByteArray, renderer: Renderer, keyboard: Keyboard) {
        val opcode = ((memory[pc.toInt()].toInt() shl 8) or memory[pc.toInt() + 1].toInt()).toUShort()
        executeOpcode(opcode, memory, renderer, keyboard)
    }

    fun executeOpcode(opcode: UShort, memory: UByteArray, renderer: Renderer, keyboard: Keyboard) {
        pc = (pc + 2u).toUShort()

        val x = (opcode and toUShort("0F00")).toInt() shr 8
        val y = (opcode and toUShort("00F0")).toInt() shr 4

        when (opcode and toUShort("F000")) {
            toUShort("0000") -> when (opcode) {
                toUShort("00E0") -> {
                    renderer.clear()
                }
                toUShort("00EE") -> {
                    pc = stack[sp.toInt()]
                    sp = (sp - 1u).toUShort()
                }
            }
            toUShort("1000") -> {
                pc = opcode and toUShort("0FFF")
            }
            toUShort("2000") -> {
                sp = (sp + 1u).toUShort()
                stack[sp.toInt()] = pc
                pc = opcode and toUShort("0FFF")
            }
            toUShort("3000") -> {
                if (vx[x] == (opcode and toUShort("00FF")).toUByte()) {
                    pc = (pc + 2u).toUShort()
                }
            }
            toUShort("4000") -> {
                if (vx[x] != (opcode and toUShort("00FF")).toUByte()) {
                    pc = (pc + 2u).toUShort()
                }
            }
            toUShort("5000") -> {
                if (vx[x] == vx[y]) {
                    pc = (pc + 2u).toUShort()
                }
            }
            toUShort("6000") -> {
                vx[x] = (opcode and toUShort("00FF")).toUByte()
            }
            toUShort("7000") -> {
                vx[x] = (vx[x] + (opcode and toUShort("00FF"))).toUByte()
            }
            toUShort("8000") -> when (opcode and toUShort("000F")) {
                toUShort("0000") -> {
                    vx[x] = vx[y]
                }
                toUShort("0001") -> {
                    vx[x] = vx[x] or vx[y]
                }
                toUShort("0002") -> {
                    vx[x] = vx[x] and vx[y]
                }
                toUShort("0003") -> {
                    vx[x] = vx[x] xor vx[y]
                }
                toUShort("0004") -> {
                    val sum = vx[x] + vx[y]

                    vx[15] = if (sum > toUByte("FF")) toUByte("01") else toUByte("00")

                    vx[x] = sum.toUByte()
                }
                toUShort("0005") -> {
                    vx[15] = if (vx[x] > vx[y]) toUByte("01") else toUByte("00")

                    vx[x] = (vx[x] - vx[y]).toUByte()
                }
                toUShort("0006") -> {
                    vx[15] = vx[x] and toUByte("01")
                    vx[x] = (vx[x].toInt() shr 1).toUByte()
                }
                toUShort("0007") -> {
                    vx[15] = if (vx[y] > vx[x]) toUByte("01") else toUByte("00")

                    vx[x] = (vx[y] - vx[x]).toUByte()
                }
                toUShort("000E") -> {
                    vx[15] = ((vx[x] and toUByte("80")).toInt() shr 7).toUByte()
                    vx[x] = (vx[x].toInt() shl 1).toUByte()
                }
            }
            toUShort("9000") -> {
                if (vx[x] != vx[y]) {
                    pc = (pc + 2u).toUShort()
                }
            }
            toUShort("A000") -> {
                i = opcode and toUShort("0FFF")
            }
            toUShort("B000") -> {
                pc = ((opcode and toUShort("0FFF")) + vx[0]).toUShort()
            }
            toUShort("C000") -> {
                val rand = (floor((Random.nextInt().toUByte() * toUByte("FF")).toDouble())).toUInt().toUShort()
                vx[x] = (rand and (opcode and toUShort("00FF"))).toUByte()
            }
            toUShort("D000") -> {
                (0 until (opcode and toUShort("000F")).toInt()).forEach { row ->
                    var sprite = memory[i.toInt() + row]
                    (0 until 8).forEach { col ->
                        if (sprite and toUByte("80") > toUByte("00")) {
                            if (renderer.setPixel(vx[x].toInt() + col, vx[y].toInt() + row)) {
                                vx[15] = toUByte("01")
                            }
                        }

                        sprite = (sprite.toInt() shl 1).toUByte()
                    }
                }
            }
            toUShort("E000") -> when (opcode and toUShort("00FF")) {
                toUShort("009E") -> {
                    if (keyboard.isKeyPressed(vx[x].toInt())) {
                        pc = (pc + 2u).toUShort()
                    }
                }
                toUShort("00A1") -> {
                    if (!keyboard.isKeyPressed(vx[x].toInt())) {
                        pc = (pc + 2u).toUShort()
                    }
                }
            }
            toUShort("F000") -> when (opcode and toUShort("00FF")) {
                toUShort("0007") -> {
                    vx[x] = delayTimer
                }
                toUShort("000A") -> {
                    if (keyboard.keyPressed != -1) {
                        vx[x] = keyboard.keyPressed.toUByte()
                    } else {
                        pc = (pc - 2u).toUShort()
                    }
                }
                toUShort("0015") -> {
                    delayTimer = vx[x]
                }
                toUShort("0018") -> {
                    soundTimer = vx[x]
                }
                toUShort("001E") -> {
                    i = (i + vx[x]).toUShort()
                }
                toUShort("0029") -> {
                    i = (vx[x] * 5u).toUShort()
                }
                toUShort("0033") -> {
                    memory[i.toInt()] = (vx[x].toInt() / 100).toUByte()
                    memory[i.toInt() + 1] = ((vx[x].toInt() % 100) / 10).toUByte()
                    memory[i.toInt() + 2] = (vx[x].toInt() % 10).toUByte()
                }
                toUShort("0055") -> {
                    (0..x).forEach { register -> memory[i.toInt() + register] = vx[register] }
                    i = (i + x.toUInt()).toUShort()
                }
                toUShort("0065") -> {
                    (0..x).forEach { register -> vx[register] = memory[i.toInt() + register] }
                    i = (i + x.toUInt()).toUShort()
                }
            }
            else -> {
            }
        }
    }

    companion object {
        fun toUShort(s: String): UShort {
            return s.toInt(16).toUShort()
        }

        fun toUByte(s: String): UByte {
            return toUShort(s).toUByte()
        }
    }
}