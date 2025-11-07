package ru.basov.springcpoolleak

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.transaction.support.DefaultTransactionDefinition

@Service
class Service(
    val txManager: R2dbcTransactionManager,
    val entityRepository: EntityRepository,
    val auditLogRepository: AuditLogRepository,
) {
    private val log = KotlinLogging.logger { }

    @Transactional
    suspend fun findEntities(delayMs: Long) : List<Entity> {
        try {
            val sleepSec = delayMs.div(1000.0)
            log.info { "Getting entries with pg_sleep for $sleepSec" }
            return entityRepository.findAllWithDelay(sleepSec)
        } catch (e: Exception) {
            log.warn(e) { "Failed" }
            withContext(NonCancellable) {
                log.info { "Saving audit" }
                TransactionalOperator.create(
                    txManager,
                    DefaultTransactionDefinition().apply {
                        isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
                        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
                    },
                )
                    .executeAndAwait {
                        auditLogRepository.save(AuditLogEntity(method = "post"))
                    }
            }
            throw e
        }
    }
}