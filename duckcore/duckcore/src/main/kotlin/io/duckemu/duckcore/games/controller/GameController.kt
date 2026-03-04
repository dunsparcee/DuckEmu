package io.duckemu.duckcore.games.controller

import io.duckemu.duckcore.games.controller.data.GameResponse
import io.duckemu.duckcore.games.domain.service.GameService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/games")
class GameController(private val gameService: GameService) {

    @GetMapping("/search")
    fun findSimilar(@RequestParam name: String): ResponseEntity<GameResponse> {
        return gameService.findMostSimilar(name)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}