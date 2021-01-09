class Chip8Builder internal constructor() {
    private var configuration = Configuration()

    fun configure(init: Configuration.() -> Unit) {
        configuration.apply { init() }
    }

    @ExperimentalUnsignedTypes
    fun run(rom: String) {
        val chip8 = Chip8()
        chip8.setup(configuration, rom)
        chip8.run()
    }
}

fun chip8(build: Chip8Builder.() -> Unit) {
    Chip8Builder().apply { build() }
}