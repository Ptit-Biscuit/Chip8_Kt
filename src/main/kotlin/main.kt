@ExperimentalUnsignedTypes
fun main() = chip8 {
    configure {
        keyboardLayout = KeyboardLayouts.FR
    }

    run("roms/wipeoff")
}
