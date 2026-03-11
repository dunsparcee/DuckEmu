package io.duckemu.duckcore.games.config.graalvm

import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import java.util.UUID

@Configuration
@ImportRuntimeHints(NativeHints::class)
class NativeHintsConfig

class NativeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.reflection()
            .registerType(Array<UUID>::class.java) { it.withMembers() }
    }
}