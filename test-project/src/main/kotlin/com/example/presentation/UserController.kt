package com.example.presentation

import com.example.application.UserService
import com.example.domain.User
import java.util.UUID

class UserController(
    private val userService: UserService
) {
    fun createUser(request: CreateUserRequest): CreateUserResponse {
        return try {
            val user = userService.createUser(request.email, request.name)
            CreateUserResponse(
                success = true,
                userId = user.id,
                message = "User created successfully"
            )
        } catch (e: Exception) {
            CreateUserResponse(
                success = false,
                userId = null,
                message = "Failed to create user: ${e.message}"
            )
        }
    }
    
    fun activateUser(userId: String): ActivateUserResponse {
        return try {
            val uuid = UUID.fromString(userId)
            val user = userService.activateUser(uuid)
            
            if (user != null) {
                ActivateUserResponse(
                    success = true,
                    message = "User activated successfully"
                )
            } else {
                ActivateUserResponse(
                    success = false,
                    message = "User not found"
                )
            }
        } catch (e: Exception) {
            ActivateUserResponse(
                success = false,
                message = "Invalid user ID format"
            )
        }
    }
}

data class CreateUserRequest(
    val email: String,
    val name: String
)

data class CreateUserResponse(
    val success: Boolean,
    val userId: UUID?,
    val message: String
)

data class ActivateUserResponse(
    val success: Boolean,
    val message: String
)