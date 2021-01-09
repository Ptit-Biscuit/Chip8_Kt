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

    fun cycle(memory: UByteArray, renderer: Renderer) {
        val opcode = ((memory[pc.toInt()].toInt() shl 8) or memory[pc.toInt() + 1].toInt()).toUShort()
        executeOpcode(opcode, renderer)
    }

    fun executeOpcode(opcode: UShort, renderer: Renderer) {
        pc = (pc + 2u).toUShort()

        val x = ((opcode and toUShort("0F00")).toInt() shr 8).toUByte()
        val y = ((opcode and toUShort("00F0")).toInt() shr 4).toUByte()

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
                if (vx[x.toInt()] == (opcode and toUShort("00FF")).toUByte()) {
                    pc = (pc + 2u).toUShort()
                }
            }
            toUShort("4000") -> {
                if (vx[x.toInt()] != (opcode and toUShort("00FF")).toUByte()) {
                    pc = (pc + 2u).toUShort()
                }
            }
            toUShort("5000") -> {
                if (vx[x.toInt()] == vx[y.toInt()]) {
                    pc = (pc + 2u).toUShort()
                }
            }
            toUShort("6000") -> {
                vx[x.toInt()] = (opcode and toUShort("00FF")).toUByte()
            }
            toUShort("7000") -> {
                vx[x.toInt()] = (vx[x.toInt()] + (opcode and toUShort("00FF"))).toUByte()
            }
            toUShort("8000") -> when (opcode and toUShort("000F")) {
                toUShort("0000") -> {
                    vx[x.toInt()] = vx[y.toInt()]
                }
                toUShort("0001") -> {
                    vx[x.toInt()] = vx[x.toInt()] or vx[y.toInt()]
                }
                toUShort("0002") -> {
                    vx[x.toInt()] = vx[x.toInt()] and vx[y.toInt()]
                }
                toUShort("0003") -> {
                    vx[x.toInt()] = vx[x.toInt()] xor vx[y.toInt()]
                }
                toUShort("0004") -> {
                    val sum = vx[x.toInt()] + vx[y.toInt()]

                    vx[15] = if (sum > toUByte("FF")) toUByte("01") else toUByte("00")

                    vx[x.toInt()] = sum.toUByte()
                }
                toUShort("0005") -> {
                    vx[15] = if (vx[x.toInt()] > vx[y.toInt()]) toUByte("01") else toUByte("00")

                    vx[x.toInt()] = (vx[x.toInt()] - vx[y.toInt()]).toUByte()
                }
                toUShort("0006") -> {
                }
                toUShort("0007") -> {
                }
                toUShort("000E") -> {
                }
            }
            toUShort("9000") -> {
            }
            toUShort("A000") -> {
            }
            toUShort("B000") -> {
            }
            toUShort("C000") -> {
            }
            toUShort("D000") -> {
            }
            toUShort("E000") -> when (opcode and 255u) {
                toUShort("009E") -> {
                }
                toUShort("00A1") -> {
                }
            }
            toUShort("F000") -> when (opcode and 255u) {
                toUShort("0007") -> {
                }
                toUShort("000A") -> {
                }
                toUShort("0015") -> {
                }
                toUShort("0018") -> {
                }
                toUShort("001E") -> {
                }
                toUShort("0029") -> {
                }
                toUShort("0033") -> {
                }
                toUShort("0055") -> {
                }
                toUShort("0065") -> {
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