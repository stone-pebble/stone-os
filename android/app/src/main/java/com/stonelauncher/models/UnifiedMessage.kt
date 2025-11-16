package com.stonelauncher.models

import java.util.UUID

/**
 * Unified message model for chat interface.
 *
 * TICKET_004: LiveKit Android SDK Integration
 *
 * Matches the React prototype's message structure:
 * - Chat messages (user or assistant)
 * - Transcriptions (voice input/output)
 * - Operation status messages
 * - System messages
 */
data class UnifiedMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val source: MessageSource = MessageSource.MANUAL,

    // Operation-specific fields
    val agent: String? = null,
    val busy: Boolean? = null,
    val operation: String? = null,
    val toolName: String? = null,
    val stepDescription: String? = null,
    val operationId: String? = null,
    val progressPercentage: Int? = null,

    // Typing indicator
    val isTyping: Boolean = false
)

/**
 * Message role (who sent the message).
 */
enum class MessageRole {
    USER,      // User message
    ASSISTANT, // AI assistant message
    SYSTEM     // System message
}

/**
 * Message type (how the message was created).
 */
enum class MessageType {
    CHAT,          // Text chat message
    TRANSCRIPTION, // Voice transcription
    OPERATION,     // Operation status update
    STATUS,        // System status message
    TYPING         // Typing indicator
}

/**
 * Message source (how the message was generated).
 */
enum class MessageSource {
    MANUAL,        // User typed manually
    TRANSCRIPTION, // Voice transcription
    AGENT,         // From AI agent
    SYSTEM         // System-generated
}
