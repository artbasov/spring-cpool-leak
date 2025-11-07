package ru.basov.springcpoolleak

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table(name = "audit_log")
data class AuditLogEntity(
    @Id val id: Long? = null,
    @Column("method")
    val method: String,

    @Column("create_time")
    @CreatedDate
    val createTime: Instant = Instant.now(),
)