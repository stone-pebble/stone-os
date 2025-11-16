package com.stonelauncher.tools

import android.content.Context
import android.util.Log
import com.stonelauncher.controllers.AppController
import com.stonelauncher.controllers.NavigationController
import com.stonelauncher.controllers.SettingsController
import com.stonelauncher.controllers.TelephonyController
import kotlinx.serialization.json.*

/**
 * Main tool execution engine for device commands from AI agents.
 *
 * TICKET_007: Tool Calling Integration
 *
 * Routes tool calls from LiveKit data channel to appropriate controllers.
 * Handles validation, permission checks, and result formatting.
 *
 * Supported tools:
 * - openApp: Launch applications
 * - setWifi: Control WiFi state
 * - setBrightness: Adjust screen brightness
 * - setVolume: Adjust volume levels
 * - makeCall: Make phone calls
 * - sendMessage: Send SMS messages
 * - navigate: Start navigation in Maps
 * - showLocation: Show location on map
 */
class ToolExecutor(private val context: Context) {

    companion object {
        private const val TAG = "ToolExecutor"

        // Tool categories for rate limiting
        private val HIGH_RISK_TOOLS = setOf("makeCall", "sendMessage")
        private val MEDIUM_RISK_TOOLS = setOf("setWifi", "setBrightness", "setVolume")
    }

    private val appController = AppController(context)
    private val settingsController = SettingsController(context)
    private val telephonyController = TelephonyController(context)
    private val navigationController = NavigationController(context)

    /**
     * Execute a tool call and return the result.
     *
     * @param toolCall Tool call message from agent
     * @return Tool result message
     */
    suspend fun executeTool(toolCall: ToolCallMessage): ToolResultMessage {
        Log.d(TAG, "Executing tool: ${toolCall.tool}")

        return try {
            // Route to appropriate handler
            val result = when (toolCall.tool) {
                "openApp" -> handleOpenApp(toolCall.params)
                "setWifi" -> handleSetWifi(toolCall.params)
                "setBrightness" -> handleSetBrightness(toolCall.params)
                "setVolume" -> handleSetVolume(toolCall.params)
                "makeCall" -> handleMakeCall(toolCall.params)
                "sendMessage" -> handleSendMessage(toolCall.params)
                "navigate" -> handleNavigate(toolCall.params)
                "showLocation" -> handleShowLocation(toolCall.params)
                // Placeholder tools (to be implemented in future tickets)
                "setAlarm", "setTimer" -> handleNotImplemented(toolCall.tool)
                "playMusic" -> handleNotImplemented(toolCall.tool)
                else -> return ToolResultMessage(
                    success = false,
                    error = ToolError(
                        code = "UNKNOWN_TOOL",
                        message = "Unknown tool: ${toolCall.tool}"
                    )
                )
            }

            // Convert controller result to ToolResultMessage
            result.fold(
                onSuccess = { data ->
                    Log.i(TAG, "Tool executed successfully: ${toolCall.tool}")
                    ToolResultMessage(
                        success = true,
                        result = data.mapToJsonElements()
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Tool execution failed: ${toolCall.tool}", error)
                    ToolResultMessage(
                        success = false,
                        error = ToolError(
                            code = getErrorCode(error),
                            message = error.message ?: "Unknown error"
                        )
                    )
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error executing tool: ${toolCall.tool}", e)
            ToolResultMessage(
                success = false,
                error = ToolError(
                    code = "EXECUTION_ERROR",
                    message = e.message ?: "Unexpected error"
                )
            )
        }
    }

    // ========== Tool Handlers ==========

    private fun handleOpenApp(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val appName = params["appName"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("Missing parameter: appName"))

        return appController.openApp(appName)
    }

    private fun handleSetWifi(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val enabled = params["enabled"]?.jsonPrimitive?.booleanOrNull
            ?: return Result.failure(IllegalArgumentException("Missing parameter: enabled"))

        return settingsController.setWifiEnabled(enabled)
    }

    private fun handleSetBrightness(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val brightness = params["brightness"]?.jsonPrimitive?.intOrNull
            ?: return Result.failure(IllegalArgumentException("Missing parameter: brightness"))

        return settingsController.setBrightness(brightness)
    }

    private fun handleSetVolume(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val streamType = params["streamType"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("Missing parameter: streamType"))

        val level = params["level"]?.jsonPrimitive?.intOrNull
            ?: return Result.failure(IllegalArgumentException("Missing parameter: level"))

        return settingsController.setVolume(streamType, level)
    }

    private fun handleMakeCall(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val phoneNumber = params["phoneNumber"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("Missing parameter: phoneNumber"))

        return telephonyController.makeCall(phoneNumber)
    }

    private fun handleSendMessage(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val phoneNumber = params["phoneNumber"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("Missing parameter: phoneNumber"))

        val message = params["message"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("Missing parameter: message"))

        return telephonyController.sendSMS(phoneNumber, message)
    }

    private fun handleNavigate(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val destination = params["destination"]?.jsonPrimitive?.content
            ?: return Result.failure(IllegalArgumentException("Missing parameter: destination"))

        val mode = params["mode"]?.jsonPrimitive?.content ?: "driving"

        return navigationController.navigateTo(destination, mode)
    }

    private fun handleShowLocation(params: Map<String, JsonElement>): Result<Map<String, Any>> {
        val query = params["query"]?.jsonPrimitive?.content

        if (query != null) {
            return navigationController.showLocation(query)
        }

        // Check for coordinates
        val latitude = params["latitude"]?.jsonPrimitive?.doubleOrNull
        val longitude = params["longitude"]?.jsonPrimitive?.doubleOrNull

        if (latitude != null && longitude != null) {
            val label = params["label"]?.jsonPrimitive?.content
            return navigationController.showCoordinates(latitude, longitude, label)
        }

        return Result.failure(
            IllegalArgumentException("Missing parameters: need either 'query' or 'latitude'/'longitude'")
        )
    }

    private fun handleNotImplemented(toolName: String): Result<Map<String, Any>> {
        return Result.failure(
            NotImplementedError("Tool '$toolName' not yet implemented. Will be added in future tickets.")
        )
    }

    // ========== Helper Functions ==========

    /**
     * Get error code from exception type.
     */
    private fun getErrorCode(error: Throwable): String {
        return when (error) {
            is SecurityException -> "PERMISSION_DENIED"
            is IllegalArgumentException -> "INVALID_PARAMETERS"
            is NotImplementedError -> "NOT_IMPLEMENTED"
            else -> error::class.simpleName ?: "UNKNOWN_ERROR"
        }
    }
}

// ========== Extension Functions ==========

/**
 * Convert Map<String, Any> to Map<String, JsonElement>.
 */
private fun Map<String, Any>.mapToJsonElements(): Map<String, JsonElement> {
    return this.mapValues { (_, value) ->
        when (value) {
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
    }
}
