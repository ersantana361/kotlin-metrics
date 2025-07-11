package com.example.domain

import java.time.LocalDateTime
import java.util.UUID

data class UserCreatedEvent(
    val userId: UUID,
    val email: String,
    val name: String,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)