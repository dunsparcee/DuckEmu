package io.duckemu.gbc.data.game

data class Game(
    val name: String,
    val iconPath: String,
    val fileType: String,
    val size: String,
    val playTime: String?,
    val path: String,
) {
    fun cropName(): String {
        val split = name.split(" ")
        var bucketSize = 0

        return split.filter {
            bucketSize += it.length
            bucketSize < 30
        }
            .joinToString(" ")
    }
}

data class GameDirectory(
    val path: String,
    val games: List<Game>
)