package io.duckemu.emulator.repository.game.http

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Failure(val errorMessage: String) : NetworkResult<Nothing>()
}

suspend inline fun <reified T> HttpResponse.handleResponse(): NetworkResult<T> {
    return when (status.value) {
        in 200..299 -> {
            try {
                val result = if (T::class == Unit::class) Unit as T else body<T>()
                NetworkResult.Success(result)
            } catch (e: SerializationException) {
                NetworkResult.Failure("Serialization error: ${e.message}")
            }
        }
        else -> {
            val errorBody = bodyAsText()
            NetworkResult.Failure("Error ${status.value}: $errorBody")
        }
    }
}

suspend inline fun <reified T> HttpClient.safeRequest(
    crossinline block: suspend HttpClient.() -> HttpResponse
): NetworkResult<T> {
    return try {
        val response = block()
        response.handleResponse<T>()
    } catch (e: Exception) {
        NetworkResult.Failure("Network error: ${e.message}")
    }
}