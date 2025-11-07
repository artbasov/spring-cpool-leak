package ru.basov.springcpoolleak

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "entity")
data class Entity(
    @Id val id: Long? = null,
    @Column("name")
    val name: String,
)