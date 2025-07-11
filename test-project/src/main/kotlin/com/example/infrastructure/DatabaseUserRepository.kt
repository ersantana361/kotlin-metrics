package com.example.infrastructure

import com.example.domain.User
import com.example.domain.UserRepository
import java.util.UUID

class DatabaseUserRepository : UserRepository {
    private val users = mutableMapOf<UUID, User>()
    
    override fun findById(id: UUID): User? {
        return users[id]
    }
    
    override fun findByEmail(email: String): User? {
        return users.values.find { it.email == email }
    }
    
    override fun save(user: User): User {
        users[user.id] = user
        return user
    }
    
    override fun delete(id: UUID) {
        users.remove(id)
    }
    
    override fun findAll(): List<User> {
        return users.values.toList()
    }
    
    override fun count(): Long {
        return users.size.toLong()
    }
    
    override fun exists(id: UUID): Boolean {
        return users.containsKey(id)
    }
}