package io.duckemu.duckcore.games.config.graalvm

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
@RegisterReflectionForBinding(Array<UUID>::class)
class NativeHints