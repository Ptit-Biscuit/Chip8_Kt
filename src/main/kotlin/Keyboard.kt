import org.openrndr.KeyEvent

/**
 * CHIP-8 Keyboard emulation
 */
class Keyboard {
    /**
     * Map for key codes from modern keyboards to CHIP-8 key codes
     */
    private val keymap = mapOf(
        49 to 0x01, // 1
        50 to 0x02, // 2
        51 to 0x03, // 3
        52 to 0x0C, // 4
        81 to 0x04, // Q
        87 to 0x05, // W
        69 to 0x06, // E
        82 to 0x0D, // R
        65 to 0x07, // A
        83 to 0x08, // S
        68 to 0x09, // D
        70 to 0x0E, // F
        90 to 0x0A, // Z
        88 to 0x00, // X
        67 to 0x0B, // C
        86 to 0x0F  // V
    )

    /**
     * Set of all key pressed
     */
    private val keyPressed = mutableSetOf<Int>()

    /** Some Chip-8 instructions require waiting for the next keypress.
    We initialize this function elsewhere when needed. **/
    var onNextKeyPress: ((Int) -> Unit)? = null

    /**
     * Checks if a CHIP-8 key is pressed
     */
    fun isKeyPressed(key: Int): Boolean {
        return keyPressed.contains(key)
    }

    /**
     * Listener for CHIP-8 key down events
     */
    fun onKeyDown(event: KeyEvent) {
        keymap[event.key]?.let { mappedKey ->
            keyPressed.add(mappedKey)

            onNextKeyPress?.invoke(mappedKey)
            onNextKeyPress = null
        }
    }

    /**
     * Listener for CHIP-8 key up events
     */
    fun onKeyUp(event: KeyEvent) {
        keymap[event.key]?.let { mappedKey -> keyPressed.remove(mappedKey) }
    }
}