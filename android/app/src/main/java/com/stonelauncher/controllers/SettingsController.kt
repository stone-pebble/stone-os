package com.stonelauncher.controllers

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Controller for device settings (WiFi, Bluetooth, brightness, volume).
 *
 * TICKET_007: Tool Calling Integration
 *
 * Handles system-level settings control via Android APIs.
 */
class SettingsController(private val context: Context) {

    companion object {
        private const val TAG = "SettingsController"
    }

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Set WiFi enabled state.
     *
     * @param enabled true to enable, false to disable
     * @return Result with new WiFi state
     */
    @Suppress("DEPRECATION")
    fun setWifiEnabled(enabled: Boolean): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Setting WiFi enabled: $enabled")

            // Note: setWifiEnabled() is deprecated in API 29+
            // For newer APIs, should guide user to settings
            @Suppress("DEPRECATION")
            val success = wifiManager.setWifiEnabled(enabled)

            if (!success) {
                return Result.failure(
                    Exception("Failed to change WiFi state. User may need to enable manually.")
                )
            }

            val actualState = wifiManager.isWifiEnabled

            Log.i(TAG, "WiFi state changed: $actualState")
            Result.success(buildJsonObject {
                put("wifi_enabled", JsonPrimitive(actualState))
                put("changed", JsonPrimitive(actualState == enabled))
            }.toMap())

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for WiFi control", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting WiFi state", e)
            Result.failure(e)
        }
    }

    /**
     * Get current WiFi state.
     */
    fun getWifiState(): Result<Map<String, Any>> {
        return try {
            val enabled = wifiManager.isWifiEnabled
            Result.success(buildJsonObject {
                put("wifi_enabled", JsonPrimitive(enabled))
            }.toMap())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi state", e)
            Result.failure(e)
        }
    }

    /**
     * Set screen brightness.
     *
     * @param brightness Brightness level 0-255
     * @return Result with new brightness level
     */
    fun setBrightness(brightness: Int): Result<Map<String, Any>> {
        return try {
            val normalizedBrightness = brightness.coerceIn(0, 255)

            // Check if we have WRITE_SETTINGS permission
            if (!Settings.System.canWrite(context)) {
                return Result.failure(
                    SecurityException("WRITE_SETTINGS permission not granted. User must enable in Settings.")
                )
            }

            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                normalizedBrightness
            )

            Log.i(TAG, "Brightness set to: $normalizedBrightness")
            Result.success(buildJsonObject {
                put("brightness", JsonPrimitive(normalizedBrightness))
            }.toMap())

        } catch (e: Exception) {
            Log.e(TAG, "Error setting brightness", e)
            Result.failure(e)
        }
    }

    /**
     * Set volume for a specific stream.
     *
     * @param streamType "media", "ring", "alarm", "notification"
     * @param level Volume level 0-100 (percentage)
     * @return Result with new volume level
     */
    fun setVolume(streamType: String, level: Int): Result<Map<String, Any>> {
        return try {
            val stream = when (streamType.lowercase()) {
                "media" -> AudioManager.STREAM_MUSIC
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                "notification" -> AudioManager.STREAM_NOTIFICATION
                else -> return Result.failure(
                    IllegalArgumentException("Unknown stream type: $streamType")
                )
            }

            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val targetVolume = ((level.coerceIn(0, 100) / 100.0) * maxVolume).toInt()

            audioManager.setStreamVolume(stream, targetVolume, 0)

            val actualVolume = audioManager.getStreamVolume(stream)
            val actualPercentage = ((actualVolume.toDouble() / maxVolume) * 100).toInt()

            Log.i(TAG, "Volume set for $streamType: $actualPercentage%")
            Result.success(buildJsonObject {
                put("stream_type", JsonPrimitive(streamType))
                put("level_percentage", JsonPrimitive(actualPercentage))
                put("level_absolute", JsonPrimitive(actualVolume))
                put("max_volume", JsonPrimitive(maxVolume))
            }.toMap())

        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
            Result.failure(e)
        }
    }

    /**
     * Get current volume for a specific stream.
     */
    fun getVolume(streamType: String): Result<Map<String, Any>> {
        return try {
            val stream = when (streamType.lowercase()) {
                "media" -> AudioManager.STREAM_MUSIC
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                "notification" -> AudioManager.STREAM_NOTIFICATION
                else -> return Result.failure(
                    IllegalArgumentException("Unknown stream type: $streamType")
                )
            }

            val currentVolume = audioManager.getStreamVolume(stream)
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val percentage = ((currentVolume.toDouble() / maxVolume) * 100).toInt()

            Result.success(buildJsonObject {
                put("stream_type", JsonPrimitive(streamType))
                put("level_percentage", JsonPrimitive(percentage))
                put("level_absolute", JsonPrimitive(currentVolume))
                put("max_volume", JsonPrimitive(maxVolume))
            }.toMap())

        } catch (e: Exception) {
            Log.e(TAG, "Error getting volume", e)
            Result.failure(e)
        }
    }
}

// Helper extension (if not already defined elsewhere)
private fun kotlinx.serialization.json.JsonObject.toMap(): Map<String, Any> {
    return this.entries.associate { (key, value) ->
        key to when (value) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    value.isString -> value.content
                    value.content == "true" || value.content == "false" -> value.content.toBoolean()
                    value.content.toIntOrNull() != null -> value.content.toInt()
                    value.content.toDoubleOrNull() != null -> value.content.toDouble()
                    else -> value.content
                }
            }
            else -> value.toString()
        }
    }
}
