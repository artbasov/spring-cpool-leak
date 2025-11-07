package ru.basov.springcpoolleak

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository


interface EntityRepository : CoroutineCrudRepository<Entity, Long> {

    @Query("SELECT pg_sleep(:delaySec), id, name FROM entity")
    suspend fun findAllWithDelay(delaySec: Double): List<Entity>
}