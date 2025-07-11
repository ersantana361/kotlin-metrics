package com.example.domain

import java.util.UUID

interface UserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun delete(id: UUID)
    fun findAll(): List<User>
    fun count(): Long
    fun exists(id: UUID): Boolean
}