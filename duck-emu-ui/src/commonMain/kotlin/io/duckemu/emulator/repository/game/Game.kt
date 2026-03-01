package io.duckemu.emulator.repository.game

import kotlinx.serialization.Serializable

data class Game(
    val name: String,
    val title: String,
    val coverPath: String?,
    val fileType: String,
    val size: String,
    val playTime: String?,
    val path: String,
    val coverImage: ByteArray?,
) {
    fun cropName(): String {
        val split = name.split(" ")
        var bucketSize = 0

        return split.filter {
            bucketSize += it.length
            bucketSize < 30
        }.joinToString(" ")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Game

        if (name != other.name) return false
        if (title != other.title) return false
        if (coverPath != other.coverPath) return false
        if (fileType != other.fileType) return false
        if (size != other.size) return false
        if (playTime != other.playTime) return false
        if (path != other.path) return false
        if (!coverImage.contentEquals(other.coverImage)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + coverPath.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + (playTime?.hashCode() ?: 0)
        result = 31 * result + path.hashCode()
        result = 31 * result + (coverImage?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
data class GameResponse(
    val name: String,
    val coverArt: String
)