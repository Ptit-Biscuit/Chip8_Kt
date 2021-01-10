import CPU.Companion.toUByte
import CPU.Companion.toUShort
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalUnsignedTypes
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CPUInstructions {
    @Test
    fun `00E0 - CLS - Clear the display`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            renderer.testRender()
            cpu.executeOpcode(toUShort("00E0"), memory, renderer, keyboard)
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

            cpu.executeOpcode(toUShort("00EE"), memory, renderer, keyboard)

            assertEquals(toUShort("0F11"), cpu.pc)
            assertEquals(toUShort("0001"), cpu.sp)
        }
    }

    @Test
    fun `1nnn - JP - The interpreter sets the program counter to nnn`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.executeOpcode(toUShort("1222"), memory, renderer, keyboard)
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

            cpu.executeOpcode(toUShort("2AF2"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("3133"), memory, renderer, keyboard)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `3xkk - SE Vx, byte - Don't skip next instruction if Vx != kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("3131"), memory, renderer, keyboard)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Skip next instruction if Vx != kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("4131"), memory, renderer, keyboard)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Don't skip next instruction if Vx == kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("4133"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("51A0"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("51A0"), memory, renderer, keyboard)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `6xkk - LD Vx, byte - Set Vx = kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")

            cpu.executeOpcode(toUShort("6112"), memory, renderer, keyboard)

            assertEquals(toUByte("12"), cpu.vx[1])
        }
    }

    @Test
    fun `7xkk - ADD Vx, byte - Set Vx = Vx + kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[8] = toUByte("14")

            cpu.executeOpcode(toUShort("7816"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8180"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8181"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8182"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8183"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8184"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8184"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8185"), memory, renderer, keyboard)

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

            cpu.executeOpcode(toUShort("8185"), memory, renderer, keyboard)

            assertEquals(toUByte("FA"), cpu.vx[1])
            assertEquals(toUByte("00"), cpu.vx[15])
        }
    }

    @Test
    fun `8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1 - with even number`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("20")

            cpu.executeOpcode(toUShort("8106"), memory, renderer, keyboard)

            assertEquals(toUByte("10"), cpu.vx[1])
            assertEquals(toUByte("00"), cpu.vx[15])
        }
    }

    @Test
    fun `8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1 - with odd number`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("31")

            cpu.executeOpcode(toUShort("8106"), memory, renderer, keyboard)

            assertEquals(toUByte("18"), cpu.vx[1])
            assertEquals(toUByte("01"), cpu.vx[15])
        }
    }

    @Test
    fun `8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set VF = NOT borrow`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("20")
            cpu.vx[8] = toUByte("03")

            cpu.executeOpcode(toUShort("8187"), memory, renderer, keyboard)

            assertEquals(toUByte("E3"), cpu.vx[1])
            assertEquals(toUByte("00"), cpu.vx[15])
        }
    }

    @Test
    fun `8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set VF = borrow`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("03")
            cpu.vx[8] = toUByte("20")

            cpu.executeOpcode(toUShort("8187"), memory, renderer, keyboard)

            assertEquals(toUByte("1D"), cpu.vx[1])
            assertEquals(toUByte("01"), cpu.vx[15])
        }
    }

    @Test
    fun `8xyE - SHL Vx {, Vy} - Set Vx = Vx SHL 1 - with even number`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("20")

            cpu.executeOpcode(toUShort("810E"), memory, renderer, keyboard)

            assertEquals(toUByte("40"), cpu.vx[1])
            assertEquals(toUByte("00"), cpu.vx[15])
        }
    }

    @Test
    fun `8xyE - SHL Vx {, Vy} - Set Vx = Vx SHL 1 - with odd number`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("98")

            cpu.executeOpcode(toUShort("810E"), memory, renderer, keyboard)

            assertEquals(toUByte("30"), cpu.vx[1])
            assertEquals(toUByte("01"), cpu.vx[15])
        }
    }

    @Test
    fun `9xy0 - SNE Vx, Vy - Skip next instruction if Vx != Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")
            cpu.vx[10] = toUByte("A5")

            cpu.executeOpcode(toUShort("91A0"), memory, renderer, keyboard)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `9xy0 - SNE Vx, Vy - Don't skip next instruction if Vx == Vy`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[1] = toUByte("33")
            cpu.vx[10] = toUByte("33")

            cpu.executeOpcode(toUShort("91A0"), memory, renderer, keyboard)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `Annn - LD I, addr - Set I = nnn`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.executeOpcode(toUShort("A315"), memory, renderer, keyboard)

            assertEquals(toUShort("0315"), cpu.i)
        }
    }

    @Test
    fun `Bnnn - JP V0, addr - Jump to location nnn + V0`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[0] = toUByte("20")

            cpu.executeOpcode(toUShort("B231"), memory, renderer, keyboard)

            assertEquals(toUShort("0251"), cpu.pc)
        }
    }

    @Test
    fun `Cxkk - RND Vx, byte - Set Vx = random byte AND kk`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("00")

            cpu.executeOpcode(toUShort("C51A"), memory, renderer, keyboard)

            // assertNotEquals(toUByte("00"), cpu.vx[5])
            assertTrue {
                cpu.vx[5] <= toUByte("1A")
            }
        }
    }

    @Test
    fun `Dxyn - DRW Vx, Vy, nibble - Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            val addr = 0
            val posX = 0
            val posY = 0

            memory[addr] = toUByte("00FF")
            memory[addr + 1] = toUByte("00FF")
            memory[addr + 2] = toUByte("00FF")
            memory[addr + 3] = toUByte("00FF")
            memory[addr + 4] = toUByte("00FF")
            memory[addr + 5] = toUByte("00FF")

            cpu.i = addr.toUShort()
            cpu.vx[1] = posX.toUByte()
            cpu.vx[2] = posY.toUByte()

            cpu.executeOpcode(toUShort("D126"), memory, renderer, keyboard)

            assertEquals(toUByte("00"), cpu.vx[15])

            (posX until posX + 8).forEach { x ->
                (posY until posY + 6).forEach { y ->
                    assertEquals(1, renderer.getPixel(x, y))
                }
            }

            //Redraw to test xor
            cpu.executeOpcode(toUShort("D126"), memory, renderer, keyboard)

            assertEquals(toUByte("01"), cpu.vx[15])

            (posX until posX + 8).forEach { x ->
                (posY until posY + 6).forEach { y ->
                    assertEquals(0, renderer.getPixel(x, y))
                }
            }
        }
    }

    @Test
    fun `Ex9E - SKP Vx - Skip next instruction if key with the value of Vx is pressed`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("02")
            keyboard.keyPressed = 0x02

            cpu.executeOpcode(toUShort("E59E"), memory, renderer, keyboard)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `Ex9E - SKP Vx - Don't skip next instruction if key with the value of Vx is NOT pressed`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("02")
            keyboard.keyPressed = 0x03

            cpu.executeOpcode(toUShort("E59E"), memory, renderer, keyboard)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `ExA1 - SKNP Vx - Skip next instruction if key with the value of Vx is NOT pressed`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("02")
            keyboard.keyPressed = 0x03

            cpu.executeOpcode(toUShort("E5A1"), memory, renderer, keyboard)

            assertEquals(toUShort("0204"), cpu.pc)
        }
    }

    @Test
    fun `ExA1 - SKNP Vx - Don't skip next instruction if key with the value of Vx is pressed`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("02")
            keyboard.keyPressed = 0x02

            cpu.executeOpcode(toUShort("E5A1"), memory, renderer, keyboard)

            assertEquals(toUShort("0202"), cpu.pc)
        }
    }

    @Test
    fun `Fx07 - LD Vx, DT - Set Vx = delay timer value`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.delayTimer = toUByte("15")
            cpu.vx[5] = toUByte("02")

            cpu.executeOpcode(toUShort("F507"), memory, renderer, keyboard)

            assertEquals(toUByte("15"), cpu.vx[5])
        }
    }

    @Test
    fun `Fx0A - LD Vx, K - Wait for a key press, store the value of the key in Vx - key pressed`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            keyboard.keyPressed = 0x08
            cpu.vx[5] = toUByte("02")

            cpu.executeOpcode(toUShort("F50A"), memory, renderer, keyboard)

            assertEquals(toUByte("08"), cpu.vx[5])
        }
    }

    @Test
    fun `Fx0A - LD Vx, K - Wait for a key press, store the value of the key in Vx - no key pressed`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            keyboard.keyPressed = -1
            cpu.vx[5] = toUByte("02")

            cpu.executeOpcode(toUShort("F50A"), memory, renderer, keyboard)

            assertEquals(toUByte("02"), cpu.vx[5])
            assertEquals(toUShort("0200"), cpu.pc)
        }
    }

    @Test
    fun `Fx15 - LD DT, Vx - Set delay timer = Vx`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("08")

            cpu.executeOpcode(toUShort("F515"), memory, renderer, keyboard)

            assertEquals(toUByte("08"), cpu.delayTimer)
        }
    }

    @Test
    fun `Fx18 - LD ST, Vx - Set sound timer = Vx`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("08")

            cpu.executeOpcode(toUShort("F518"), memory, renderer, keyboard)

            assertEquals(toUByte("08"), cpu.soundTimer)
        }
    }

    @Test
    fun `Fx1E - ADD I, Vx - Set I = I + Vx`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.i = toUShort("0315")
            cpu.vx[5] = toUByte("DC")

            cpu.executeOpcode(toUShort("F51E"), memory, renderer, keyboard)

            assertEquals(toUShort("03F1"), cpu.i)
        }
    }

    @Test
    fun `Fx29 - LD F, Vx - Set I = location of sprite for digit Vx`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.vx[5] = toUByte("03")

            cpu.executeOpcode(toUShort("F529"), memory, renderer, keyboard)

            assertEquals(toUShort("000F"), cpu.i)
        }
    }

    @Test
    fun `Fx33 - LD B, Vx - Store BCD representation of Vx in memory locations I, I+1, and I+2`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.i = toUShort("0420")
            cpu.vx[5] = toUByte("7B")

            cpu.executeOpcode(toUShort("F533"), memory, renderer, keyboard)

            assertEquals(toUByte("01"), memory[0x420])
            assertEquals(toUByte("02"), memory[0x421])
            assertEquals(toUByte("03"), memory[0x422])
        }
    }

    @Test
    fun `Fx55 - LD {I}, Vx - Store registers V0 through Vx in memory starting at location I`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.i = toUShort("0420")
            cpu.vx[0] = toUByte("05")
            cpu.vx[1] = toUByte("54")
            cpu.vx[2] = toUByte("A8")
            cpu.vx[3] = toUByte("D1")
            cpu.vx[4] = toUByte("DC")

            cpu.executeOpcode(toUShort("F455"), memory, renderer, keyboard)

            assertEquals(toUShort("0424"), cpu.i)
            assertEquals(toUByte("05"), memory[0x420])
            assertEquals(toUByte("54"), memory[0x421])
            assertEquals(toUByte("A8"), memory[0x422])
            assertEquals(toUByte("D1"), memory[0x423])
            assertEquals(toUByte("DC"), memory[0x424])
        }
    }

    @Test
    fun `Fx65 - LD Vx, {I} - Read registers V0 through Vx from memory starting at location I`() {
        val chip8 = Chip8()
        chip8.setup(Configuration().apply { headless = true }, "")
        chip8.apply {
            cpu.i = toUShort("0420")
            memory[0x420] = toUByte("05")
            memory[0x421] = toUByte("54")
            memory[0x422] = toUByte("A8")
            memory[0x423] = toUByte("D1")
            memory[0x424] = toUByte("DC")

            cpu.executeOpcode(toUShort("F465"), memory, renderer, keyboard)

            assertEquals(toUShort("0424"), cpu.i)
            assertEquals(toUByte("05"), cpu.vx[0])
            assertEquals(toUByte("54"), cpu.vx[1])
            assertEquals(toUByte("A8"), cpu.vx[2])
            assertEquals(toUByte("D1"), cpu.vx[3])
            assertEquals(toUByte("DC"), cpu.vx[4])
        }
    }
}