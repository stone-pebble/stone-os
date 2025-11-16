package com.stonelauncher.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Tool calling message data classes for LiveKit data channel communication.
 *
 * TICKET_007: Tool Calling Integration
 *
 * These messages follow JSON-RPC style communication pattern:
 * - Agent sends ToolCallMessage via "device_command" topic
 * - Android executes the tool and sends ToolResultMessage back
 * - Android can send StatusUpdateMessage during long operations
 */

/**
 * Tool call request from agent to Android device.
 *
 * Example:
 * {
 *   "tool": "openApp",
 *   "params": {
 *     "appName": "spotify"
 *   }
 * }
 */
@Serializable
data class ToolCallMessage(
    val tool: String,
    val params: Map<String, JsonElement>
)

/**
 * Tool execution result sent back to agent.
 *
 * Success example:
 * {
 *   "success": true,
 *   "result": {
 *     "app_opened": true,
 *     "package_name": "com.spotify.music"
 *   }
 * }
 *
 * Error example:
 * {
 *   "success": false,
 *   "error": {
 *     "code": "APP_NOT_INSTALLED",
 *     "message": "Spotify is not installed"
 *   }
 * }
 */
@Serializable
data class ToolResultMessage(
    val success: Boolean,
    val result: Map<String, JsonElement>? = null,
    val error: ToolError? = null
)

/**
 * Error information for failed tool calls.
 */
@Serializable
data class ToolError(
    val code: String,
    val message: String
)

/**
 * Status update message for long-running operations.
 *
 * Example:
 * {
 *   "status": "in_progress",
 *   "message": "Opening Maps app...",
 *   "progress": 50
 * }
 */
@Serializable
data class StatusUpdateMessage(
    val status: String, // "pending", "in_progress", "completed", "failed"
    val message: String,
    val progress: Int? = null // 0-100
)
