package ru.basov.springcpoolleak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCpoolLeakApplication

fun main(args: Array<String>) {
    runApplication<SpringCpoolLeakApplication>(*args)
}
