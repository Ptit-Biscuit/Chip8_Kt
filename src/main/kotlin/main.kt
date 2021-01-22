@ExperimentalUnsignedTypes
fun main() = chip8 {
    configure {
        scale = 16
        keyboardLayout = KeyboardLayouts.FR
    }

    run("wipeoff")
}
