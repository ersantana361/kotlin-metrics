package com.example.application

import com.example.domain.User
import com.example.domain.UserRepository
import com.example.domain.Email
import java.util.UUID

class UserService(
    private val userRepository: UserRepository
) {
    fun createUser(email: String, name: String): User {
        val validatedEmail = Email(email)
        val user = User(
            id = UUID.randomUUID(),
            email = validatedEmail.value,
            name = name
        )
        return userRepository.save(user)
    }
    
    fun activateUser(userId: UUID): User? {
        val user = userRepository.findById(userId)
        return if (user != null) {
            user.activate()
            userRepository.save(user)
        } else {
            null
        }
    }
    
    fun validateEmail(email: String): Boolean {
        return try {
            Email(email)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    fun processUserBatch(users: List<User>): List<User> {
        return users.filter { it.isActive }
            .map { user ->
                if (user.email.contains("@temp")) {
                    user.copy(isActive = false)
                } else {
                    user
                }
            }
    }
}