package io.duckemu.duckcore.games.repository

import io.duckemu.duckcore.games.domain.model.Game
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GameRepository : JpaRepository<Game, Long> {

    @Query(
        value = "SELECT * FROM games WHERE similarity(name, :name) > 0.7 ORDER BY similarity(name, :name) DESC LIMIT 1",
        nativeQuery = true
    )
    fun findMostSimilarByName(@Param("name") name: String): Game?
}