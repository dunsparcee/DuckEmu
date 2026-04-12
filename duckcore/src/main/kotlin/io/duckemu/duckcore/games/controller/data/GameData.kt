package io.duckemu.duckcore.games.controller.data

import io.duckemu.duckcore.games.domain.model.Game

data class GameResponse(
    val name: String,
    val coverArt: String
)

fun Game.toResponse(): GameResponse = GameResponse(
    name = this.name,
    coverArt = this.coverArt
)