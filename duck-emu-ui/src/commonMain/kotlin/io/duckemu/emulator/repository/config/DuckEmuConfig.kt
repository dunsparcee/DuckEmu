package io.duckemu.emulator.repository.config

import io.duckemu.emulator.repository.game.Game
import kotlinx.serialization.Serializable

@Serializable
data class DuckEmuConfig(
    val version: String = "1.0.0",
    val games: Map<String, List<GameConfig>>
)

@Serializable
data class GameConfig(val game: Game, val states: List<String> = listOf())