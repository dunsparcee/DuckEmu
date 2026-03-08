package io.duckemu.emulator.repository.config

import io.duckemu.emulator.repository.game.Game
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path

class ConfigRepository(
    private val store: KStore<DuckEmuConfig> = defaultStore
) {

    fun saveConfig() {

    }


    suspend fun getGames(): Map<String, List<GameConfig>> {
        return store.get()?.games ?: mapOf()
    }

    suspend fun importGame(console: String, game: Game) {
        val config = store.get()
        if (config == null)
            store.set(
                DuckEmuConfig(
                    games = mutableMapOf(
                        console to listOf(GameConfig(game))
                    )
                )
            )

        store.update { config ->
            if (config?.games[console] == null) {
                config?.copy(
                    games = config.games + (console to listOf(GameConfig(game)))
                )
            } else {
                config.copy(
                    games = config.games.toMutableMap().apply {
                        this[console] = (this[console] ?: emptyList()) + GameConfig(game)
                    }
                )
            }
        }
    }
}

expect fun provideStorePath(): String?

val defaultStore: KStore<DuckEmuConfig> = storeOf(
    file = Path(provideStorePath() + "/duck-emu.json")
)