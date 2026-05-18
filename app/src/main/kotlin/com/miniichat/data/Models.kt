package com.miniichat.data

import kotlinx.serialization.Serializable

enum class Role { user, assistant, system }

@Serializable
data class Message(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
