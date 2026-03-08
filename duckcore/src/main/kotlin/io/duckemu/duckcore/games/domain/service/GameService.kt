package io.duckemu.duckcore.games.domain.service

import io.duckemu.duckcore.games.controller.data.GameResponse
import io.duckemu.duckcore.games.controller.data.toResponse
import io.duckemu.duckcore.games.domain.model.Game
import io.duckemu.duckcore.games.repository.GameRepository
import org.springframework.stereotype.Service

@Service
class GameService(private val gameRepository: GameRepository) {

    fun findMostSimilar(name: String): GameResponse? =
        gameRepository.findMostSimilarByName(name)
            ?.toResponse()
}