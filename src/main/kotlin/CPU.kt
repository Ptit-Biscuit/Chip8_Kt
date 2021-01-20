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
    internal var pc: UShort = 0x0200u

    /** Stack pointer 16-bit register **/
    internal var sp: UShort = 0u

    /** Stack 16-bit registers **/
    internal val stack = UShortArray(16)

    /** Delay timer 8-bit register **/
    internal var delayTimer: UByte = 0u

    /** Sound timer 8-bit register **/
    internal var soundTimer: UByte = 0u

    private enum class Instruction(val mask: UShort, val pattern: UShort, val operation: (UShort, Chip8) -> Unit) {
        CLS(0xFFFFu, 0x00E0u, { _, chip8 ->
            chip8.renderer.clear()
        }),
        RET(0xFFFFu, 0x00EEu, { _, chip8 ->
            chip8.cpu.pc = chip8.cpu.stack[chip8.cpu.sp.toInt()]
            chip8.cpu.sp = (chip8.cpu.sp - 1u).toUShort()
        }),
        JP(0xF000u, 0x1000u, { opcode, chip8 ->
            chip8.cpu.pc = opcode and 0x0FFFu
        }),
        CALL(0xF000u, 0x2000u, { opcode, chip8 ->
            chip8.cpu.sp = (chip8.cpu.sp + 1u).toUShort()
            chip8.cpu.stack[chip8.cpu.sp.toInt()] = chip8.cpu.pc
            chip8.cpu.pc = opcode and 0x0FFFu
        }),
        SE_VX_BYTE(0xF000u, 0x3000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            if (chip8.cpu.vx[x] == (opcode and 0x00FFu).toUByte()) {
                chip8.cpu.pc = (chip8.cpu.pc + 2u).toUShort()
            }
        }),
        SNE_VX_BYTE(0xF000u, 0x4000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            if (chip8.cpu.vx[x] != (opcode and 0x00FFu).toUByte()) {
                chip8.cpu.pc = (chip8.cpu.pc + 2u).toUShort()
            }
        }),
        SE_VX_VY(0xF000u, 0x5000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            if (chip8.cpu.vx[x] == chip8.cpu.vx[y]) {
                chip8.cpu.pc = (chip8.cpu.pc + 2u).toUShort()
            }
        }),
        LD_VX_BYTE(0xF000u, 0x6000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.vx[x] = (opcode and 0x00FFu).toUByte()
        }),
        ADD_VX_BYTE(0xF000u, 0x7000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.vx[x] = (chip8.cpu.vx[x] + (opcode and 0x00FFu)).toUByte()
        }),
        LD_VX_VY(0xF00Fu, 0x8000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            chip8.cpu.vx[x] = chip8.cpu.vx[y]
        }),
        OR_VX_VY(0xF00Fu, 0x8001u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            chip8.cpu.vx[x] = chip8.cpu.vx[x] or chip8.cpu.vx[y]
        }),
        AND_VX_VY(0xF00Fu, 0x8002u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            chip8.cpu.vx[x] = chip8.cpu.vx[x] and chip8.cpu.vx[y]
        }),
        XOR_VX_VY(0xF00Fu, 0x8003u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            chip8.cpu.vx[x] = chip8.cpu.vx[x] xor chip8.cpu.vx[y]
        }),
        ADD_VX_VY(0xF00Fu, 0x8004u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            val sum = chip8.cpu.vx[x] + chip8.cpu.vx[y]

            chip8.cpu.vx[15] = if (sum > 0xFFu) 0x01u else 0x00u

            chip8.cpu.vx[x] = sum.toUByte()
        }),
        SUB_VX_VY(0xF00Fu, 0x8005u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            chip8.cpu.vx[15] = if (chip8.cpu.vx[x] > chip8.cpu.vx[y]) 0x01u else 0x00u

            chip8.cpu.vx[x] = (chip8.cpu.vx[x] - chip8.cpu.vx[y]).toUByte()
        }),
        SHR_VX_VY(0xF00Fu, 0x8006u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.vx[15] = chip8.cpu.vx[x] and 0x01u
            chip8.cpu.vx[x] = (chip8.cpu.vx[x].toInt() shr 1).toUByte()
        }),
        SUBN_VX_VY(0xF00Fu, 0x8007u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            chip8.cpu.vx[15] = if (chip8.cpu.vx[y] > chip8.cpu.vx[x]) 0x01u else 0x00u

            chip8.cpu.vx[x] = (chip8.cpu.vx[y] - chip8.cpu.vx[x]).toUByte()
        }),
        SHL_VX_VY(0xF00Fu, 0x800Eu, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.vx[15] = ((chip8.cpu.vx[x] and 0x80u).toInt() shr 7).toUByte()
            chip8.cpu.vx[x] = (chip8.cpu.vx[x].toInt() shl 1).toUByte()
        }),
        SNE_VX_VY(0xF000u, 0x9000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4

            if (chip8.cpu.vx[x] != chip8.cpu.vx[y]) {
                chip8.cpu.pc = (chip8.cpu.pc + 2u).toUShort()
            }
        }),
        LD_I_ADDR(0xF000u, 0xA000u, { opcode, chip8 ->
            chip8.cpu.i = opcode and 0x0FFFu
        }),
        JP_V0_ADDR(0xF000u, 0xB000u, { opcode, chip8 ->
            chip8.cpu.pc = ((opcode and 0x0FFFu) + chip8.cpu.vx[0]).toUShort()
        }),
        RND_VX_BYTE(0xF000u, 0xC000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            val value = (floor((Random.nextInt().toUByte() * 0xFFu).toDouble())).toUInt().toUShort()
            chip8.cpu.vx[x] = (value and (opcode and 0x00FFu)).toUByte()
        }),
        DRW_VX_VY_NIBBLE(0xF000u, 0xD000u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8
            val y = (opcode and 0x00F0u).toInt() shr 4
            chip8.cpu.vx[15] = 0x00u

            (0 until (opcode and 0x000Fu).toInt()).forEach { row ->
                var sprite = chip8.memory[chip8.cpu.i.toInt() + row]
                (0 until 8).forEach { col ->
                    if (sprite and 0x80u > 0x00u) {
                        if (chip8.renderer.setPixel(chip8.cpu.vx[x].toInt() + col, chip8.cpu.vx[y].toInt() + row)) {
                            chip8.cpu.vx[15] = 0x01u
                        }
                    }

                    sprite = (sprite.toInt() shl 1).toUByte()
                }
            }
        }),
        SKP_VX(0xF0FFu, 0xE09Eu, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            if (chip8.keyboard.isKeyPressed(chip8.cpu.vx[x].toInt())) {
                chip8.cpu.pc = (chip8.cpu.pc + 2u).toUShort()
            }
        }),
        SKNP_VX(0xF0FFu, 0xE0A1u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            if (!chip8.keyboard.isKeyPressed(chip8.cpu.vx[x].toInt())) {
                chip8.cpu.pc = (chip8.cpu.pc + 2u).toUShort()
            }
        }),
        LD_VX_DT(0xF0FFu, 0xF007u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.vx[x] = chip8.cpu.delayTimer
        }),
        LD_VX_K(0xF0FFu, 0xF00Au, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            if (chip8.keyboard.keyPressed != -1) {
                chip8.cpu.vx[x] = chip8.keyboard.keyPressed.toUByte()
            } else {
                chip8.cpu.pc = (chip8.cpu.pc - 2u).toUShort()
            }
        }),
        LD_DT_VX(0xF0FFu, 0xF015u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.delayTimer = chip8.cpu.vx[x]
        }),
        LD_ST_VX(0xF0FFu, 0xF018u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.soundTimer = chip8.cpu.vx[x]
        }),
        ADD_I_VX(0xF0FFu, 0xF01Eu, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.i = (chip8.cpu.i + chip8.cpu.vx[x]).toUShort()
        }),
        LD_F_VX(0xF0FFu, 0xF029u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.cpu.i = (chip8.cpu.vx[x] * 5u).toUShort()
        }),
        LD_B_VX(0xF0FFu, 0xF033u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            chip8.memory[chip8.cpu.i.toInt()] = (chip8.cpu.vx[x].toInt() / 100).toUByte()
            chip8.memory[chip8.cpu.i.toInt() + 1] = ((chip8.cpu.vx[x].toInt() % 100) / 10).toUByte()
            chip8.memory[chip8.cpu.i.toInt() + 2] = (chip8.cpu.vx[x].toInt() % 10).toUByte()
        }),
        LD_I_VX(0xF0FFu, 0xF055u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            (0..x).forEach { register -> chip8.memory[chip8.cpu.i.toInt() + register] = chip8.cpu.vx[register] }
            chip8.cpu.i = (chip8.cpu.i + x.toUInt()).toUShort()
        }),
        LD_VX_I(0xF0FFu, 0xF065u, { opcode, chip8 ->
            val x = (opcode and 0x0F00u).toInt() shr 8

            (0..x).forEach { register -> chip8.cpu.vx[register] = chip8.memory[chip8.cpu.i.toInt() + register] }
            chip8.cpu.i = (chip8.cpu.i + x.toUInt()).toUShort()
        }),
    }

    fun cycle(chip8: Chip8) {
        val opcode = ((chip8.memory[pc.toInt()].toInt() shl 8) or chip8.memory[pc.toInt() + 1].toInt()).toUShort()
        executeOpcode(opcode, chip8)
    }

    fun executeOpcode(opcode: UShort, chip8: Chip8) {
        pc = (pc + 2u).toUShort()
        Instruction.values().find { opcode and it.mask == it.pattern }?.operation?.invoke(opcode, chip8)
    }

    fun reset() {
        vx.fill(0x00u)
        i = 0u
        pc = 0x0200u
        sp = 0u
        stack.fill(0x0000u)
        delayTimer = 0u
        soundTimer = 0u
    }
}