import CPU.Companion.toUByte
import CPU.Companion.toUShort
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CPUInstructions {
    @Test
    fun `00E0 - CLS - Clear the display`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            renderer.testRender()
            cpu.executeOpcode(toUShort("00E0"), renderer)
            assertEquals(0, renderer.display.sum())
        }
    }

    @Test
    fun `00EE - RET - Return from subroutine`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.sp = toUShort("0002")
            cpu.stack[0] = toUShort("0082")
            cpu.stack[1] = toUShort("0528")
            cpu.stack[2] = toUShort("0F11")

            cpu.executeOpcode(toUShort("00EE"), renderer)

            assertEquals(toUShort("0F11"), cpu.pc)
            assertEquals(toUShort("0001"), cpu.sp)
        }
    }

    @Test
    fun `1nnn - JP - The interpreter sets the program counter to nnn`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.executeOpcode(toUShort("1222"), renderer)
            assertEquals(toUShort("0222"), cpu.pc)
        }
    }

    @Test
    fun `2nnn - CALL - Call subroutine at nnn`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.pc = toUShort("0203")
            cpu.sp = toUShort("0002")
            cpu.stack[0] = toUShort("0082")
            cpu.stack[1] = toUShort("0528")
            cpu.stack[2] = toUShort("0F11")

            cpu.executeOpcode(toUShort("2AF2"), renderer)

            assertEquals(toUShort("0AF2"), cpu.pc)
            assertEquals(toUShort("0003"), cpu.sp)
            assertEquals(toUShort("0205"), cpu.stack[cpu.sp.toInt()])
        }
    }

    @Test
    fun `3xkk - SE Vx, byte - Skip next instruction if Vx == kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("3133"), renderer)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `3xkk - SE Vx, byte - Don't skip next instruction if Vx != kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("3131"), renderer)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Skip next instruction if Vx != kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("4131"), renderer)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Don't skip next instruction if Vx == kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("4133"), renderer)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `5xy0 - SE Vx, Vy - Skip next instruction if Vx == Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")
            cpu.vx[10] = toUByte("33")

            cpu.executeOpcode(toUShort("51A0"), renderer)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `5xy0 - SE Vx, Vy - Don't skip next instruction if Vx != Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("31")
            cpu.vx[10] = toUByte("33")

            cpu.executeOpcode(toUShort("51A0"), renderer)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `6xkk - LD Vx, byte - Set Vx = kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("6112"), renderer)

            assertEquals(toUByte("12"), cpu.vx[1])
        }
    }

    @Test
    fun `7xkk - ADD Vx, byte - Set Vx = Vx + kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[8] = toUByte("14")

            cpu.executeOpcode(toUShort("7816"), renderer)

            assertEquals(toUByte("2A"), cpu.vx[8])
        }
    }

    @Test
    fun `8xy0 - LD Vx, Vy - Set Vx = Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("AD")
            cpu.vx[8] = toUByte("14")

            cpu.executeOpcode(toUShort("8180"), renderer)

            assertEquals(toUByte("14"), cpu.vx[1])
        }
    }

    @Test
    fun `8xy1 - OR Vx, Vy - Set Vx = Vx OR Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("AD")
            cpu.vx[8] = toUByte("14")

            cpu.executeOpcode(toUShort("8181"), renderer)

            assertEquals(toUByte("BD"), cpu.vx[1])
        }
    }

    @Test
    fun `8xy2 - AND Vx, Vy - Set Vx = Vx AND Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("AD")
            cpu.vx[8] = toUByte("14")

            cpu.executeOpcode(toUShort("8182"), renderer)

            assertEquals(toUByte("04"), cpu.vx[1])
        }
    }

    @Test
    fun `8xy3 - XOR Vx, Vy - Set Vx = Vx XOR Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("AD")
            cpu.vx[8] = toUByte("14")

            cpu.executeOpcode(toUShort("8183"), renderer)

            assertEquals(toUByte("B9"), cpu.vx[1])
        }
    }

    @Test
    fun `8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, carry flag set VF = 1`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("AC")
            cpu.vx[8] = toUByte("DC")

            cpu.executeOpcode(toUShort("8184"), renderer)

            assertEquals(toUByte("88"), cpu.vx[1])
            assertEquals(toUByte("01"), cpu.vx[15])
        }
    }

    @Test
    fun `8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, carry flag not set VF = 0`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("AC")
            cpu.vx[8] = toUByte("30")

            cpu.executeOpcode(toUShort("8184"), renderer)

            assertEquals(toUByte("DC"), cpu.vx[1])
            assertEquals(toUByte("00"), cpu.vx[15])
        }
    }

    @Test
    fun `8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = NOT borrow`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("20")
            cpu.vx[8] = toUByte("03")

            cpu.executeOpcode(toUShort("8185"), renderer)

            assertEquals(toUByte("1D"), cpu.vx[1])
            assertEquals(toUByte("01"), cpu.vx[15])
        }
    }

    @Test
    fun `8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = borrow`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("09")
            cpu.vx[8] = toUByte("0F")

            cpu.executeOpcode(toUShort("8185"), renderer)

            assertEquals(toUByte("FA"), cpu.vx[1])
            assertEquals(toUByte("00"), cpu.vx[15])
        }
    }
}