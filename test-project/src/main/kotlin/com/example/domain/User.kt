package com.example.domain

import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val name: String,
    var isActive: Boolean = true
) {
    fun activate() {
        isActive = true
    }
    
    fun deactivate() {
        isActive = false
    }
    
    fun updateName(newName: String): User {
        return copy(name = newName)
    }
}