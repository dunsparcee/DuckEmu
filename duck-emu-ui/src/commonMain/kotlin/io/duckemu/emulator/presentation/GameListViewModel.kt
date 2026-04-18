package io.duckemu.emulator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.duckemu.emulator.repository.config.ConfigRepository
import io.duckemu.emulator.repository.game.Game
import io.duckemu.emulator.repository.game.GameRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.extension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameLibraryViewModel(
    private val repository: GameRepository,
    private val config: ConfigRepository? = null
) : ViewModel() {

    private val _console = MutableStateFlow(listOf("gb", "gbc", "gba", "nes", "sms", "gg"))
    val console: StateFlow<List<String>> = _console.asStateFlow()

    private val _gamesByConsole = MutableStateFlow<Map<String, List<Game>>>(emptyMap())
    val gamesByConsole: StateFlow<Map<String, List<Game>>> = _gamesByConsole.asStateFlow()

    fun loadConfig() {
        viewModelScope.launch {
            val games = config?.getGames()
            _gamesByConsole.value = games
                ?.mapValues { entry -> entry.value.map { it.game } }
                ?: emptyMap()
        }
    }

    fun addNewFiles() {
        viewModelScope.launch {
            FileKit.openFilePicker(mode = FileKitMode.Multiple())?.let { selectedFiles ->
                val updatedMap = _gamesByConsole.value.toMutableMap()

                console.value.forEach { console ->
                    val newGames = selectedFiles
                        .filter { it.extension.equals(console, ignoreCase = true) }
                        .map { repository.toGame(it) }

                    if (newGames.isNotEmpty()) {
                        val existingGames = updatedMap[console] ?: emptyList()
                        updatedMap[console] = existingGames + newGames
                    }
                }

                _gamesByConsole.value = updatedMap
            }
        }
    }

    fun addGame(selectedFile: PlatformFile) {
        viewModelScope.launch {
            val updatedMap = _gamesByConsole.value.toMutableMap()

            console.value.forEach { console ->
                val game = repository.toGame(selectedFile)

                if (console == selectedFile.extension) {
                    val existingGames = updatedMap[console] ?: emptyList()
                    updatedMap[console] = existingGames + game
                    config?.importGame(console, game)
                }
            }

            _gamesByConsole.value = updatedMap
        }
    }
}