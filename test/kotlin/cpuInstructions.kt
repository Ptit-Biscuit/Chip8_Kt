import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalUnsignedTypes
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CPUInstructions {
    @Test
    fun `00E0 - CLS - Clear the display`() {
        Chip8().apply {
            mapOf(
                0 to 0,
                0 to 31,
                31 to 15,
                63 to 0,
                63 to 31
            ).forEach { (x, y) ->
                val pixelLoc = (x % screen.cols) + ((y % screen.rows) * screen.cols)
                screen.display[pixelLoc] = screen.display[pixelLoc] xor 1
            }

            emulate(0x00E0)
            assertEquals(0, screen.display.sum())
        }
    }

    @Test
    fun `00EE - RET - Return from subroutine`() {
        Chip8().apply {
            cpu.sp = 2
            cpu.stack[0] = 0x0082u
            cpu.stack[1] = 0x0528u
            cpu.stack[2] = 0x0F11u

            emulate(0x00EE)
            assertEquals(0x0F11, cpu.pc)
            assertEquals(1, cpu.sp)
        }
    }

    @Test
    fun `1nnn - JP - The interpreter sets the program counter to nnn`() {
        Chip8().apply {
            emulate(0x1222)
            assertEquals(0x0222, cpu.pc)
        }
    }

    @Test
    fun `2nnn - CALL - Call subroutine at nnn`() {
        Chip8().apply {
            cpu.pc = 0x0203
            cpu.sp = 2
            cpu.stack[0] = 0x0082u
            cpu.stack[1] = 0x0528u
            cpu.stack[2] = 0x0F11u

            emulate(0x2AF2)
            assertEquals(0x0AF2, cpu.pc)
            assertEquals(3, cpu.sp)
            assertEquals(0x0205u, cpu.stack[cpu.sp])
        }
    }

    @Test
    fun `3xkk - SE Vx, byte - Skip next instruction if Vx == kk`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u

            emulate(0x3133)
            assertEquals(0x0204, cpu.pc)
        }
    }

    @Test
    fun `3xkk - SE Vx, byte - Don't skip next instruction if Vx != kk`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u

            emulate(0x3131)
            assertEquals(0x0202, cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Skip next instruction if Vx != kk`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u

            emulate(0x4131)
            assertEquals(0x0204, cpu.pc)
        }
    }

    @Test
    fun `4xkk - SNE Vx, byte - Don't skip next instruction if Vx == kk`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u

            emulate(0x4133)
            assertEquals(0x0202, cpu.pc)
        }
    }

    @Test
    fun `5xy0 - SE Vx, Vy - Skip next instruction if Vx == Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u
            cpu.vx[10] = 0x33u

            emulate(0x51A0)
            assertEquals(0x0204, cpu.pc)
        }
    }

    @Test
    fun `5xy0 - SE Vx, Vy - Don't skip next instruction if Vx != Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0x31u
            cpu.vx[10] = 0x33u

            emulate(0x51A0)
            assertEquals(0x0202, cpu.pc)
        }
    }

    @Test
    fun `6xkk - LD Vx, byte - Set Vx = kk`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u

            emulate(0x6112)
            assertEquals(0x12u, cpu.vx[1])
        }
    }

    @Test
    fun `7xkk - ADD Vx, byte - Set Vx = Vx + kk`() {
        Chip8().apply {
            cpu.vx[8] = 0x14u

            emulate(0x7816)
            assertEquals(0x2Au, cpu.vx[8])
        }
    }

    @Test
    fun `8xy0 - LD Vx, Vy - Set Vx = Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            emulate(0x8180)
            assertEquals(0x14u, cpu.vx[1])
        }
    }

    @Test
    fun `8xy1 - OR Vx, Vy - Set Vx = Vx OR Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            emulate(0x8181)
            assertEquals(0xBDu, cpu.vx[1])
        }
    }

    @Test
    fun `8xy2 - AND Vx, Vy - Set Vx = Vx AND Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            emulate(0x8182)
            assertEquals(0x04u, cpu.vx[1])
        }
    }

    @Test
    fun `8xy3 - XOR Vx, Vy - Set Vx = Vx XOR Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0xADu
            cpu.vx[8] = 0x14u

            emulate(0x8183)
            assertEquals(0xB9u, cpu.vx[1])
        }
    }

    @Test
    fun `8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, carry flag set VF = 1`() {
        Chip8().apply {
            cpu.vx[1] = 0xACu
            cpu.vx[8] = 0xDCu

            emulate(0x8184)
            assertEquals(0x88u, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, carry flag not set VF = 0`() {
        Chip8().apply {
            cpu.vx[1] = 0xACu
            cpu.vx[8] = 0x30u

            emulate(0x8184)
            assertEquals(0xDCu, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = NOT borrow`() {
        Chip8().apply {
            cpu.vx[1] = 0x20u
            cpu.vx[8] = 0x03u

            emulate(0x8185)
            assertEquals(0x1Du, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = borrow`() {
        Chip8().apply {
            cpu.vx[1] = 0x09u
            cpu.vx[8] = 0x0Fu

            emulate(0x8185)
            assertEquals(0xFAu, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1 - with even number`() {
        Chip8().apply {
            cpu.vx[1] = 0x20u

            emulate(0x8106)
            assertEquals(0x10u, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1 - with odd number`() {
        Chip8().apply {
            cpu.vx[1] = 0x31u

            emulate(0x8106)
            assertEquals(0x18u, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set VF = NOT borrow`() {
        Chip8().apply {
            cpu.vx[1] = 0x20u
            cpu.vx[8] = 0x03u

            emulate(0x8187)
            assertEquals(0xE3u, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set VF = borrow`() {
        Chip8().apply {
            cpu.vx[1] = 0x03u
            cpu.vx[8] = 0x20u

            emulate(0x8187)
            assertEquals(0x1Du, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `8xyE - SHL Vx {, Vy} - Set Vx = Vx SHL 1 - with even number`() {
        Chip8().apply {
            cpu.vx[1] = 0x20u

            emulate(0x810E)
            assertEquals(0x40u, cpu.vx[1])
            assertEquals(0x00u, cpu.vx[15])
        }
    }

    @Test
    fun `8xyE - SHL Vx {, Vy} - Set Vx = Vx SHL 1 - with odd number`() {
        Chip8().apply {
            cpu.vx[1] = 0x98u

            emulate(0x810E)
            assertEquals(0x30u, cpu.vx[1])
            assertEquals(0x01u, cpu.vx[15])
        }
    }

    @Test
    fun `9xy0 - SNE Vx, Vy - Skip next instruction if Vx != Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u
            cpu.vx[10] = 0xA5u

            emulate(0x91A0)
            assertEquals(0x0204, cpu.pc)
        }
    }

    @Test
    fun `9xy0 - SNE Vx, Vy - Don't skip next instruction if Vx == Vy`() {
        Chip8().apply {
            cpu.vx[1] = 0x33u
            cpu.vx[10] = 0x33u

            emulate(0x91A0)
            assertEquals(0x0202, cpu.pc)
        }
    }

    @Test
    fun `Annn - LD I, addr - Set I = nnn`() {
        Chip8().apply {
            emulate(0xA315)
            assertEquals(0x0315, cpu.i)
        }
    }

    @Test
    fun `Bnnn - JP V0, addr - Jump to location nnn + V0`() {
        Chip8().apply {
            cpu.vx[0] = 0x20u

            emulate(0xB231)
            assertEquals(0x0251, cpu.pc)
        }
    }

    @Test
    fun `Cxkk - RND Vx, byte - Set Vx = random byte AND kk`() {
        Chip8().apply {
            (0..100).forEach { _ ->
                cpu.vx[5] = 0x00u

                emulate(0xC51A)
                assertTrue {
                    cpu.vx[5] <= 0x1Au
                }
            }
        }
    }

    @Test
    fun `Dxyn - DRW Vx, Vy, nibble - Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision`() {
        Chip8().apply {
            cpu.i = 0
            cpu.vx[1] = 0x00u
            cpu.vx[2] = 0x00u
            memory[0] = 0xFFu
            memory[1] = 0xFFu
            memory[2] = 0xFFu
            memory[3] = 0xFFu
            memory[4] = 0xFFu
            memory[5] = 0xFFu

            emulate(0xD126)
            assertEquals(0x00u, cpu.vx[15])

            (0 until 8).forEach { x ->
                (0 until 6).forEach { y ->
                    val pixel = screen.display[(x % screen.cols) + ((y % screen.rows) * screen.cols)]
                    assertEquals(1, pixel)
                }
            }

            // Redraw to test xor
            emulate(0xD126)
            assertEquals(0x01u, cpu.vx[15])

            (0 until 8).forEach { x ->
                (0 until 6).forEach { y ->
                    val pixel = screen.display[(x % screen.cols) + ((y % screen.rows) * screen.cols)]
                    assertEquals(0, pixel)
                }
            }
        }
    }

    @Test
    fun `Ex9E - SKP Vx - Skip next instruction if key with the value of Vx is pressed`() {
        Chip8().apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x02

            emulate(0xE59E)
            assertEquals(0x0204, cpu.pc)
        }
    }

    @Test
    fun `Ex9E - SKP Vx - Don't skip next instruction if key with the value of Vx is NOT pressed`() {
        Chip8().apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x03

            emulate(0xE59E)
            assertEquals(0x0202, cpu.pc)
        }
    }

    @Test
    fun `ExA1 - SKNP Vx - Skip next instruction if key with the value of Vx is NOT pressed`() {
        Chip8().apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x03

            emulate(0xE5A1)
            assertEquals(0x0204, cpu.pc)
        }
    }

    @Test
    fun `ExA1 - SKNP Vx - Don't skip next instruction if key with the value of Vx is pressed`() {
        Chip8().apply {
            cpu.vx[5] = 0x02u
            keyboard.keyPressed = 0x02

            emulate(0xE5A1)
            assertEquals(0x0202, cpu.pc)
        }
    }

    @Test
    fun `Fx07 - LD Vx, DT - Set Vx = delay timer value`() {
        Chip8().apply {
            cpu.delayTimer = 0x15u
            cpu.vx[5] = 0x02u

            emulate(0xF507)
            assertEquals(0x15u, cpu.vx[5])
        }
    }

    @Test
    fun `Fx0A - LD Vx, K - Wait for a key press, store the value of the key in Vx - key pressed`() {
        Chip8().apply {
            keyboard.keyPressed = 0x08
            cpu.vx[5] = 0x02u

            emulate(0xF50A)
            assertEquals(0x08u, cpu.vx[5])
        }
    }

    @Test
    fun `Fx0A - LD Vx, K - Wait for a key press, store the value of the key in Vx - no key pressed`() {
        Chip8().apply {
            keyboard.keyPressed = -1
            cpu.vx[5] = 0x02u

            emulate(0xF50A)
            assertEquals(0x02u, cpu.vx[5])
            assertEquals(0x0200, cpu.pc)
        }
    }

    @Test
    fun `Fx15 - LD DT, Vx - Set delay timer = Vx`() {
        Chip8().apply {
            cpu.vx[5] = 0x08u

            emulate(0xF515)
            assertEquals(0x08u, cpu.delayTimer)
        }
    }

    @Test
    fun `Fx18 - LD ST, Vx - Set sound timer = Vx`() {
        Chip8().apply {
            cpu.vx[5] = 0x08u

            emulate(0xF518)
            assertEquals(0x08u, cpu.soundTimer)
        }
    }

    @Test
    fun `Fx1E - ADD I, Vx - Set I = I + Vx`() {
        Chip8().apply {
            cpu.i = 0x0315
            cpu.vx[5] = 0xDCu

            emulate(0xF51E)
            assertEquals(0x03F1, cpu.i)
        }
    }

    @Test
    fun `Fx29 - LD F, Vx - Set I = location of sprite for digit Vx`() {
        Chip8().apply {
            cpu.vx[5] = 0x03u

            emulate(0xF529)
            assertEquals(0x000F, cpu.i)
        }
    }

    @Test
    fun `Fx33 - LD B, Vx - Store BCD representation of Vx in memory locations I, I+1, and I+2`() {
        Chip8().apply {
            cpu.i = 0x0420
            cpu.vx[5] = 0x7Bu

            emulate(0xF533)
            assertEquals(0x01u, memory[0x420])
            assertEquals(0x02u, memory[0x421])
            assertEquals(0x03u, memory[0x422])
        }
    }

    @Test
    fun `Fx55 - LD {I}, Vx - Store registers V0 through Vx in memory starting at location I`() {
        Chip8().apply {
            cpu.i = 0x0420
            cpu.vx[0] = 0x05u
            cpu.vx[1] = 0x54u
            cpu.vx[2] = 0xA8u
            cpu.vx[3] = 0xD1u
            cpu.vx[4] = 0xDCu

            emulate(0xF455)
            assertEquals(0x0424, cpu.i)
            assertEquals(0x05u, memory[0x420])
            assertEquals(0x54u, memory[0x421])
            assertEquals(0xA8u, memory[0x422])
            assertEquals(0xD1u, memory[0x423])
            assertEquals(0xDCu, memory[0x424])
        }
    }

    @Test
    fun `Fx65 - LD Vx, {I} - Read registers V0 through Vx from memory starting at location I`() {
        Chip8().apply {
            cpu.i = 0x0420
            memory[0x420] = 0x05u
            memory[0x421] = 0x54u
            memory[0x422] = 0xA8u
            memory[0x423] = 0xD1u
            memory[0x424] = 0xDCu

            emulate(0xF465)
            assertEquals(0x0424, cpu.i)
            assertEquals(0x05u, cpu.vx[0])
            assertEquals(0x54u, cpu.vx[1])
            assertEquals(0xA8u, cpu.vx[2])
            assertEquals(0xD1u, cpu.vx[3])
            assertEquals(0xDCu, cpu.vx[4])
        }
    }
}