import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalUnsignedTypes
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CPUInstructions {
    @Test
    fun `00E0 - CLS - Clear the display`() {
        Chip8(Configuration().apply { headless = true }).apply {
            renderer.testRender()
            cpu.executeOpcode(0x00E0u, this)
            assertEquals(0, renderer.display.sum())
        }
    }

    @Test
    fun `00EE - RET - Return from subroutine`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.sp = 0x0002u
            cpu.stack[0] = 0x0082u
            cpu.stack[1] = 0x0528u
            cpu.stack[2] = 0x0F11u

            cpu.executeOpcode(0x00EEu, this)

            assertEquals(0x0F11u, cpu.pc)
            assertEquals(0x0001u, cpu.sp)
        }
    }

    @Test
    fun `1nnn - JP - The interpreter sets the program counter to nnn`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.executeOpcode(0x1222u, this)
            assertEquals(0x0222u, cpu.pc)
        }
    }

    @Test
    fun `2nnn - CALL - Call subroutine at nnn`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.pc = 0x0203u
            cpu.sp = 0x0002u
            cpu.stack[0] = 0x0082u
            cpu.stack[1] = 0x0528u
            cpu.stack[2] = 0x0F11u

            cpu.executeOpcode(0x2AF2u, this)

            assertEquals(0x0AF2u, cpu.pc)
            assertEquals(0x0003u, cpu.sp)
            assertEquals(0x0205u, cpu.stack[cpu.sp.toInt()])
        }
    }

    @Test
    fun `3xkk - SE Vx, byte - Skip next instruction if Vx == kk`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u

            cpu.executeOpcode(0x3133u, this)

            assertEquals(0x0204u, cpu.pc)
        }
    }

    @Test
    fun `3xkk - SE Vx, byte - Don't skip next instruction if Vx != kk`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u

            cpu.executeOpcode(0x3131u, this)

            assertEquals(0x0202u, cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Skip next instruction if Vx != kk`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u

            cpu.executeOpcode(0x4131u, this)

            assertEquals(0x0204u, cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Don't skip next instruction if Vx == kk`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u

            cpu.executeOpcode(0x4133u, this)

            assertEquals(0x0202u, cpu.pc)
        }
    }

    @Test
    fun `5xy0 - SE Vx, Vy - Skip next instruction if Vx == Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u
            cpu.vx[10] = 0x33u

            cpu.executeOpcode(0x51A0u, this)

            assertEquals(0x0204u, cpu.pc)
        }
    }

    @Test
    fun `5xy0 - SE Vx, Vy - Don't skip next instruction if Vx != Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x31u
            cpu.vx[10] = 0x33u

            cpu.executeOpcode(0x51A0u, this)

            assertEquals(0x0202u, cpu.pc)
        }
    }

    @Test
    fun `6xkk - LD Vx, byte - Set Vx = kk`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u

            cpu.executeOpcode(0x6112u, this)

            assertEquals(0x12u, cpu.vx[1])
        }
    }

    @Test
    fun `7xkk - ADD Vx, byte - Set Vx = Vx + kk`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[8] = 0x14u

            cpu.executeOpcode(0x7816u, this)

            assertEquals(0x2Au, cpu.vx[8])
        }
    }

    @Test
    fun `8xy0 - LD Vx, Vy - Set Vx = Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            cpu.executeOpcode(0x8180u, this)

            assertEquals(0x14u, cpu.vx[1])
        }
    }

    @Test
    fun `8xy1 - OR Vx, Vy - Set Vx = Vx OR Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            cpu.executeOpcode(0x8181u, this)

            assertEquals(0xBDu, cpu.vx[1])
        }
    }

    @Test
    fun `8xy2 - AND Vx, Vy - Set Vx = Vx AND Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            cpu.executeOpcode(0x8182u, this)

            assertEquals(0x04u, cpu.vx[1])
        }
    }

    @Test
    fun `8xy3 - XOR Vx, Vy - Set Vx = Vx XOR Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            cpu.executeOpcode(0x8183u, this)

            assertEquals(0xB9u, cpu.vx[1])
        }
    }

    @Test
    fun `8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, carry flag set VF = 1`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0xACu
            cpu.vx[8] = 0xDCu

            cpu.executeOpcode(0x8184u, this)

            assertEquals(0x88u, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, carry flag not set VF = 0`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0xACu
            cpu.vx[8] = 0x30u

            cpu.executeOpcode(0x8184u, this)

            assertEquals(0xDCu, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = NOT borrow`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x20u
            cpu.vx[8] = 0x03u

            cpu.executeOpcode(0x8185u, this)

            assertEquals(0x1Du, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = borrow`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x09u
            cpu.vx[8] = 0x0Fu

            cpu.executeOpcode(0x8185u, this)

            assertEquals(0xFAu, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1 - with even number`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x20u

            cpu.executeOpcode(0x8106u, this)

            assertEquals(0x10u, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1 - with odd number`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x31u

            cpu.executeOpcode(0x8106u, this)

            assertEquals(0x18u, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set VF = NOT borrow`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x20u
            cpu.vx[8] = 0x03u

            cpu.executeOpcode(0x8187u, this)

            assertEquals(0xE3u, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set VF = borrow`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x03u
            cpu.vx[8] = 0x20u

            cpu.executeOpcode(0x8187u, this)

            assertEquals(0x1Du, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xyE - SHL Vx {, Vy} - Set Vx = Vx SHL 1 - with even number`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x20u

            cpu.executeOpcode(0x810Eu, this)

            assertEquals(0x40u, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xyE - SHL Vx {, Vy} - Set Vx = Vx SHL 1 - with odd number`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x98u

            cpu.executeOpcode(0x810Eu, this)

            assertEquals(0x30u, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `9xy0 - SNE Vx, Vy - Skip next instruction if Vx != Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u
            cpu.vx[10] = 0xA5u

            cpu.executeOpcode(0x91A0u, this)

            assertEquals(0x0204u, cpu.pc)
        }
    }

    @Test
    fun `9xy0 - SNE Vx, Vy - Don't skip next instruction if Vx == Vy`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[1] = 0x33u
            cpu.vx[10] = 0x33u

            cpu.executeOpcode(0x91A0u, this)

            assertEquals(0x0202u, cpu.pc)
        }
    }

    @Test
    fun `Annn - LD I, addr - Set I = nnn`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.executeOpcode(0xA315u, this)

            assertEquals(0x0315u, cpu.i)
        }
    }

    @Test
    fun `Bnnn - JP V0, addr - Jump to location nnn + V0`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[0] = 0x20u

            cpu.executeOpcode(0xB231u, this)

            assertEquals(0x0251u, cpu.pc)
        }
    }

    @Test
    fun `Cxkk - RND Vx, byte - Set Vx = random byte AND kk`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x00u

            cpu.executeOpcode(0xC51Au, this)

            assertTrue {
                cpu.vx[5] <= 0x1Au
            }
        }
    }

    @Test
    fun `Dxyn - DRW Vx, Vy, nibble - Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision`() {
        Chip8(Configuration().apply { headless = true }).apply {
            val addr = 0
            val posX = 0
            val posY = 0

            memory[addr] = 0x00FFu
            memory[addr + 1] = 0x00FFu
            memory[addr + 2] = 0x00FFu
            memory[addr + 3] = 0x00FFu
            memory[addr + 4] = 0x00FFu
            memory[addr + 5] = 0x00FFu

            cpu.i = addr.toUShort()
            cpu.vx[1] = posX.toUByte()
            cpu.vx[2] = posY.toUByte()

            cpu.executeOpcode(0xD126u, this)

            assertEquals(0x00u, cpu.vx[15])

            (posX until posX + 8).forEach { x ->
                (posY until posY + 6).forEach { y ->
                    assertEquals(1, renderer.getPixel(x, y))
                }
            }

            //Redraw to test xor
            cpu.executeOpcode(0xD126u, this)

            assertEquals(0x01u, cpu.vx[15])

            (posX until posX + 8).forEach { x ->
                (posY until posY + 6).forEach { y ->
                    assertEquals(0, renderer.getPixel(x, y))
                }
            }
        }
    }

    @Test
    fun `Ex9E - SKP Vx - Skip next instruction if key with the value of Vx is pressed`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x02

            cpu.executeOpcode(0xE59Eu, this)

            assertEquals(0x0204u, cpu.pc)
        }
    }

    @Test
    fun `Ex9E - SKP Vx - Don't skip next instruction if key with the value of Vx is NOT pressed`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x03

            cpu.executeOpcode(0xE59Eu, this)

            assertEquals(0x0202u, cpu.pc)
        }
    }

    @Test
    fun `ExA1 - SKNP Vx - Skip next instruction if key with the value of Vx is NOT pressed`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x03

            cpu.executeOpcode(0xE5A1u, this)

            assertEquals(0x0204u, cpu.pc)
        }
    }

    @Test
    fun `ExA1 - SKNP Vx - Don't skip next instruction if key with the value of Vx is pressed`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x02

            cpu.executeOpcode(0xE5A1u, this)

            assertEquals(0x0202u, cpu.pc)
        }
    }

    @Test
    fun `Fx07 - LD Vx, DT - Set Vx = delay timer value`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.delayTimer = 0x15u
            cpu.vx[5] = 0x02u

            cpu.executeOpcode(0xF507u, this)

            assertEquals(0x15u, cpu.vx[5])
        }
    }

    @Test
    fun `Fx0A - LD Vx, K - Wait for a key press, store the value of the key in Vx - key pressed`() {
        Chip8(Configuration().apply { headless = true }).apply {
            keyboard.keyPressed = 0x08
            cpu.vx[5] = 0x02u

            cpu.executeOpcode(0xF50Au, this)

            assertEquals(0x08u, cpu.vx[5])
        }
    }

    @Test
    fun `Fx0A - LD Vx, K - Wait for a key press, store the value of the key in Vx - no key pressed`() {
        Chip8(Configuration().apply { headless = true }).apply {
            keyboard.keyPressed = -1
            cpu.vx[5] = 0x02u

            cpu.executeOpcode(0xF50Au, this)

            assertEquals(0x02u, cpu.vx[5])
            assertEquals(0x0200u, cpu.pc)
        }
    }

    @Test
    fun `Fx15 - LD DT, Vx - Set delay timer = Vx`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x08u

            cpu.executeOpcode(0xF515u, this)

            assertEquals(0x08u, cpu.delayTimer)
        }
    }

    @Test
    fun `Fx18 - LD ST, Vx - Set sound timer = Vx`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x08u

            cpu.executeOpcode(0xF518u, this)

            assertEquals(0x08u, cpu.soundTimer)
        }
    }

    @Test
    fun `Fx1E - ADD I, Vx - Set I = I + Vx`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.i = 0x0315u
            cpu.vx[5] = 0xDCu

            cpu.executeOpcode(0xF51Eu, this)

            assertEquals(0x03F1u, cpu.i)
        }
    }

    @Test
    fun `Fx29 - LD F, Vx - Set I = location of sprite for digit Vx`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.vx[5] = 0x03u

            cpu.executeOpcode(0xF529u, this)

            assertEquals(0x000Fu, cpu.i)
        }
    }

    @Test
    fun `Fx33 - LD B, Vx - Store BCD representation of Vx in memory locations I, I+1, and I+2`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.i = 0x0420u
            cpu.vx[5] = 0x7Bu

            cpu.executeOpcode(0xF533u, this)

            assertEquals(0x01u, memory[0x420])
            assertEquals(0x02u, memory[0x421])
            assertEquals(0x03u, memory[0x422])
        }
    }

    @Test
    fun `Fx55 - LD {I}, Vx - Store registers V0 through Vx in memory starting at location I`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.i = 0x0420u
            cpu.vx[0] = 0x05u
            cpu.vx[1] = 0x54u
            cpu.vx[2] = 0xA8u
            cpu.vx[3] = 0xD1u
            cpu.vx[4] = 0xDCu

            cpu.executeOpcode(0xF455u, this)

            assertEquals(0x0424u, cpu.i)
            assertEquals(0x05u, memory[0x420])
            assertEquals(0x54u, memory[0x421])
            assertEquals(0xA8u, memory[0x422])
            assertEquals(0xD1u, memory[0x423])
            assertEquals(0xDCu, memory[0x424])
        }
    }

    @Test
    fun `Fx65 - LD Vx, {I} - Read registers V0 through Vx from memory starting at location I`() {
        Chip8(Configuration().apply { headless = true }).apply {
            cpu.i = 0x0420u
            memory[0x420] = 0x05u
            memory[0x421] = 0x54u
            memory[0x422] = 0xA8u
            memory[0x423] = 0xD1u
            memory[0x424] = 0xDCu

            cpu.executeOpcode(0xF465u, this)

            assertEquals(0x0424u, cpu.i)
            assertEquals(0x05u, cpu.vx[0])
            assertEquals(0x54u, cpu.vx[1])
            assertEquals(0xA8u, cpu.vx[2])
            assertEquals(0xD1u, cpu.vx[3])
            assertEquals(0xDCu, cpu.vx[4])
        }
    }
}