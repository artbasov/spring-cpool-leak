package ru.basov.springcpoolleak

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    val service: Service
) {
    private val log = KotlinLogging.logger { }

    @GetMapping(path = ["/entities"])
    suspend fun getEntities(
        @RequestParam(name = "delay_ms", required = false) delayMs: Long = 2000,
    ): List<Entity> {
        log.info { "Got request with delay=$delayMs" }
            return service.findEntities( delayMs)
    }
}