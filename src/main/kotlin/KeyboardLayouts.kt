enum class KeyboardLayouts(val mapper: Map<String, Int>, var keyPressed: Int = -1) {
    EN(
        mapOf(
            "1" to 0x01,
            "2" to 0x02,
            "3" to 0x03,
            "4" to 0x0C,
            "q" to 0x04,
            "w" to 0x05,
            "e" to 0x06,
            "r" to 0x0D,
            "a" to 0x07,
            "s" to 0x08,
            "d" to 0x09,
            "f" to 0x0E,
            "z" to 0x0A,
            "x" to 0x00,
            "c" to 0x0B,
            "v" to 0x0F
        )
    ),
    FR(
        mapOf(
            "1" to 0x01,
            "2" to 0x02,
            "3" to 0x03,
            "4" to 0x0C,
            "a" to 0x04,
            "z" to 0x05,
            "e" to 0x06,
            "r" to 0x0D,
            "q" to 0x07,
            "s" to 0x08,
            "d" to 0x09,
            "f" to 0x0E,
            "w" to 0x0A,
            "x" to 0x00,
            "c" to 0x0B,
            "v" to 0x0F
        )
    )
}