package io.duckemu.emulator.repository.game

import io.duckemu.emulator.repository.game.http.NetworkResult
import io.duckemu.emulator.repository.game.http.createHttpClient
import io.duckemu.emulator.repository.game.http.safeRequest
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodedPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext


class GameRepository(
    private val client: HttpClient = createHttpClient()
) {

    suspend fun toGame(directory: PlatformFile): Game = withContext(Dispatchers.IO) {
        getGameInfo(directory)
    }


    private suspend fun getGameInfo(file: PlatformFile): Game {
        val title = getTitle(file.readBytes())

        val response: NetworkResult<GameResponse> = client.safeRequest {
            get("http://localhost:8080/api/games/search") {
                url {
                    parameters.append("name", file.nameWithoutExtension)
                }
                accept(ContentType.Application.Json)
            }
        }

        val image : Pair<String, ByteArray?>? = when (response) {
            is NetworkResult.Success -> {
                response.data.coverArt.let { url ->
                    val encodedUrl = URLBuilder(url).apply {
                        val filename = pathSegments.last().encodeURLPathPart()
                        encodedPath = encodedPath.substringBeforeLast("/") + "/$filename"
                    }.buildString()
                    url to client.get(encodedUrl).body<ByteArray?>()
                }
            }

            is NetworkResult.Failure -> {
                null
            }
        }

        return Game(
            name = file.nameWithoutExtension,
            title = title,
            coverPath = image?.first,
            fileType = file.extension,
            size = "${file.size() / 1000 / 1000} mb",
            playTime = null,
            path = file.path,
            coverImage = image?.second
        )
    }

    fun getTitle(data: ByteArray): String {
        return data.slice(0x134 until 0x144)
            .map { b -> val c = b.toInt() and 0xFF; if (c < 128) c.toChar() else ' ' }
            .filter { it.code != 0 }
            .joinToString("")
    }
}