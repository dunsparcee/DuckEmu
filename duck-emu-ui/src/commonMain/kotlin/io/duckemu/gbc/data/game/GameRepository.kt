package io.duckemu.gbc.data.game

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext


class GameRepository {

    suspend fun scanDir(directory: PlatformFile): List<Game> = withContext(Dispatchers.IO) {
        directory.list()
            .filter { it.extension in listOf("gba", "gbc", "gb") }
            .map { file -> parseGameFile(file) }
    }

    suspend fun toGame(directory: PlatformFile): Game = withContext(Dispatchers.IO) {
        parseGameFile(directory)
    }

    private fun parseGameFile(file: PlatformFile): Game {
        return Game(
            name = file.nameWithoutExtension,
            iconPath = "https://i.imgur.com/t8jCyxe.png",
            fileType = file.extension,
            size = "${file.size() / 1000 / 1000} mb",
            playTime = null,
        )
    }

    suspend fun saveDir(directory: PlatformFile) = withContext(Dispatchers.IO) {
    }
}