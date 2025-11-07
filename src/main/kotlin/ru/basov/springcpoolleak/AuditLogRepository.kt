package ru.basov.springcpoolleak

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository


interface AuditLogRepository : CoroutineCrudRepository<AuditLogEntity, Long> {

}