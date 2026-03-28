package io.duckemu.emulator.repository.config

import io.duckemu.emulator.repository.game.Game
import io.duckemu.gbc.presentation.emulator.ControllerTheme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class ConfigRepository(
    private val store: KStore<DuckEmuConfig> = defaultStore
) {

    fun saveConfig() {

    }

    suspend fun getGames(): Map<String, List<GameConfig>> {
        return store.get()?.games ?: mapOf()
    }

    suspend fun importGame(console: String, game: Game) {
        handleFilePermission(game.path)
        val config = store.get()
        if (config == null)
            store.set(
                DuckEmuConfig(
                    games = mutableMapOf(
                        console to listOf(GameConfig(game))
                    )
                )
            )
        else
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

expect fun handleFilePermission(file: String)

expect fun appPath(): String?

val defaultStore: KStore<DuckEmuConfig> = storeOf(
    file = Path(appPath() + "/duck-emu.json")
)

object ControllerThemeStore {
    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(ControllerTheme::class) {
                subclass(ControllerTheme.GBC::class)
                subclass(ControllerTheme.GBA::class)
                subclass(ControllerTheme.N64::class)
            }
        }
    }

    private val stores = mutableMapOf<String, KStore<ControllerTheme>>()

    private fun defaultFor(consoleId: String): ControllerTheme = when (consoleId) {
        "gbc" -> ControllerTheme.GBC()
        "gba" -> ControllerTheme.GBA()
        "n64" -> ControllerTheme.N64()
        else -> error("Unknown console: $consoleId")
    }

    fun storeFor(consoleId: String) =
        stores.getOrPut(consoleId) {
            storeOf(
                file = Path(appPath() + "/controller_theme_$consoleId.json"),
                default = defaultFor(consoleId),
                json = json
            )
        }

    suspend fun get(consoleId: String) =
        storeFor(consoleId).get() ?: defaultFor(consoleId)

    suspend fun update(consoleId: String, theme: ControllerTheme) =
        storeFor(consoleId).set(theme)

    fun updates(consoleId: String) =
        storeFor(consoleId).updates
}
