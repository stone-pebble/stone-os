package com.stonelauncher.models

/**
 * Data model for chat messages
 *
 * TICKET_003: Chat Interface UI
 *
 * Represents a single message in the chat conversation.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val isTyping: Boolean = false
)
