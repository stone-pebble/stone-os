package com.stonelauncher.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for handling Intent API calls to Stone Launcher.
 *
 * This is the foundation of the "Headless" interface in Stone's "Head & Headless" architecture.
 * AI agents and external applications can control Stone Launcher functionality by sending
 * broadcast Intents with specific actions. This receiver routes those Intents to the appropriate
 * handlers and returns results via broadcast.
 *
 * Architecture Pattern:
 * - Touch UI (React Native) → Native Module → Controller (business logic)
 * - AI Agent → Intent API → StoneApiReceiver → Controller (same business logic)
 *
 * Both interfaces use the same underlying Controller classes, ensuring consistency.
 *
 * Usage:
 * Send Intent via adb:
 *   adb shell am broadcast -a com.stone.launcher.action.GET_WIFI_STATE
 *
 * Listen for result:
 *   Action: com.stone.launcher.result.GET_WIFI_STATE
 *   Extras: success=true, wifi_enabled=true/false
 */
class StoneApiReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "StoneApiReceiver"
        const val CATEGORY = "com.stone.launcher.category.API"

        /**
         * Converts an action string to its corresponding result action.
         * Example: "com.stone.launcher.action.SET_WIFI" -> "com.stone.launcher.result.SET_WIFI"
         */
        fun getResultAction(action: String): String {
            return action.replace(".action.", ".result.")
        }
    }

    /**
     * Receives and processes broadcast Intents.
     *
     * Routes each Intent to the appropriate handler based on action,
     * then sends a result broadcast back to the caller.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ""
        Log.d(TAG, "Received intent: $action")

        try {
            val result = when (action) {
                "com.stone.launcher.action.SET_WIFI" -> handleSetWifi(context, intent)
                "com.stone.launcher.action.GET_WIFI_STATE" -> handleGetWifiState(context, intent)
                // Additional handlers will be added as features are implemented in future tickets
                else -> createErrorResult(
                    message = "Unknown action: $action",
                    code = "UNKNOWN_ACTION"
                )
            }

            sendResult(context, action, result)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling intent: $action", e)
            sendResult(
                context,
                action,
                createErrorResult(
                    message = "Internal error: ${e.message}",
                    code = "INTERNAL_ERROR"
                )
            )
        }
    }

    /**
     * Placeholder handler for WiFi control.
     * Will be fully implemented in TICKET_002: WiFi Controller.
     *
     * Expected extras:
     * - enabled: Boolean (true to enable WiFi, false to disable)
     *
     * Returns:
     * - success: Boolean
     * - wifi_enabled: Boolean (new WiFi state)
     */
    private fun handleSetWifi(context: Context, intent: Intent): IntentResult {
        Log.d(TAG, "handleSetWifi called (placeholder - will be implemented in TICKET_002)")

        val enabled = intent.getBooleanExtra("enabled", false)

        // TODO: Implement in TICKET_002
        // val controller = WifiController(context)
        // val result = controller.setWifiEnabled(enabled)
        // return result.fold(
        //     onSuccess = { IntentResult.success(mapOf("wifi_enabled" to it)) },
        //     onFailure = { IntentResult.error(it.message ?: "Failed to set WiFi state") }
        // )

        return IntentResult.success(mapOf(
            "wifi_enabled" to enabled,
            "placeholder" to true
        ))
    }

    /**
     * Placeholder handler for getting WiFi state.
     * Will be fully implemented in TICKET_002: WiFi Controller.
     *
     * Returns:
     * - success: Boolean
     * - wifi_enabled: Boolean (current WiFi state)
     */
    private fun handleGetWifiState(context: Context, intent: Intent): IntentResult {
        Log.d(TAG, "handleGetWifiState called (placeholder - will be implemented in TICKET_002)")

        // TODO: Implement in TICKET_002
        // val controller = WifiController(context)
        // val isEnabled = controller.isWifiEnabled()
        // return IntentResult.success(mapOf("wifi_enabled" to isEnabled))

        return IntentResult.success(mapOf(
            "wifi_enabled" to false,
            "placeholder" to true
        ))
    }

    /**
     * Sends a result broadcast back to the caller.
     *
     * Result Intents follow this pattern:
     * - Action: com.stone.launcher.result.{ORIGINAL_ACTION}
     * - Category: com.stone.launcher.category.API
     * - Extras: success + (data OR error_message/error_code)
     *
     * @param context Android context
     * @param originalAction The action that was received
     * @param result The result to send
     */
    private fun sendResult(context: Context, originalAction: String, result: IntentResult) {
        val resultIntent = Intent(getResultAction(originalAction)).apply {
            addCategory(CATEGORY)
            putExtra("success", result.success)

            if (result.success) {
                // Add all success data to the Intent
                result.data.forEach { (key, value) ->
                    when (value) {
                        is String -> putExtra(key, value)
                        is Int -> putExtra(key, value)
                        is Boolean -> putExtra(key, value)
                        is Long -> putExtra(key, value)
                        is Double -> putExtra(key, value)
                        is Float -> putExtra(key, value)
                        else -> Log.w(TAG, "Unsupported data type for key $key: ${value::class.java}")
                    }
                }
            } else {
                // Add error information
                putExtra("error_message", result.errorMessage ?: "Unknown error")
                result.errorCode?.let { putExtra("error_code", it) }
            }
        }

        context.sendBroadcast(resultIntent)
        Log.d(TAG, "Sent result for $originalAction: success=${result.success}")
    }

    /**
     * Helper to create error results consistently.
     */
    private fun createErrorResult(message: String, code: String? = null): IntentResult {
        return IntentResult.error(message, code)
    }
}
