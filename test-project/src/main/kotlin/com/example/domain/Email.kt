package com.example.domain

data class Email(val value: String) {
    init {
        require(value.contains("@")) { "Invalid email format" }
    }
    
    fun getDomain(): String {
        return value.substringAfter("@")
    }
}