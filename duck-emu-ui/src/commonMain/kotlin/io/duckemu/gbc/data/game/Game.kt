package io.duckemu.gbc.data.game

data class Game(
    val name: String,
    val iconPath: String,
    val fileType: String,
    val size: String,
    val playTime: String?,
)

data class GameDirectory(
    val path: String,
    val games: List<Game>
)