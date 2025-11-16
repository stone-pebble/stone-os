package com.stonelauncher.controllers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Controller for app launching and management.
 *
 * TICKET_007: Tool Calling Integration
 *
 * Handles opening apps by name or package identifier.
 * Supports both Stone apps and third-party apps.
 */
class AppController(private val context: Context) {

    companion object {
        private const val TAG = "AppController"

        // Map of Stone app names to placeholder activities
        // TODO: Replace with actual activity names as they're implemented
        private val STONE_APP_NAMES = mapOf(
            "tick" to "com.stonelauncher.ui.TickActivity",
            "pebbles" to "com.stonelauncher.ui.PebblesActivity",
            "set" to "com.stonelauncher.ui.SetActivity",
            "listen" to "com.stonelauncher.ui.ListenActivity",
            "ask" to "com.stonelauncher.ui.AskActivity",
            "look" to "com.stonelauncher.ui.LookActivity",
            "plan" to "com.stonelauncher.ui.PlanActivity",
            "think" to "com.stonelauncher.ui.ThinkActivity",
            "reflect" to "com.stonelauncher.ui.ReflectActivity",
            "connect" to "com.stonelauncher.ui.ConnectActivity",
            "go" to "com.stonelauncher.ui.GoActivity",
            "fund" to "com.stonelauncher.ui.FundActivity"
        )

        // Common third-party app package names
        private val COMMON_APP_PACKAGES = mapOf(
            "spotify" to "com.spotify.music",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "chrome" to "com.android.chrome",
            "camera" to "com.android.camera2"
        )
    }

    /**
     * Open an app by name or package identifier.
     *
     * @param appName App name (e.g., "spotify", "maps") or package name
     * @return Result with app details or error
     */
    fun openApp(appName: String): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Opening app: $appName")

            // Try to resolve to package name
            val packageName = resolvePackageName(appName)

            if (packageName == null) {
                return Result.failure(
                    Exception("Unknown app: $appName")
                )
            }

            // Check if app is installed
            if (!isAppInstalled(packageName)) {
                Log.w(TAG, "App not installed: $packageName")
                // Redirect to Play Store
                openPlayStore(packageName)
                return Result.success(buildJsonObject {
                    put("app_opened", JsonPrimitive(false))
                    put("redirected_to_play_store", JsonPrimitive(true))
                    put("package_name", JsonPrimitive(packageName))
                }.toMap())
            }

            // Get launch intent
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return Result.failure(
                    Exception("Cannot launch app: $packageName")
                )

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            Log.i(TAG, "Successfully opened: $packageName")
            Result.success(buildJsonObject {
                put("app_opened", JsonPrimitive(true))
                put("package_name", JsonPrimitive(packageName))
            }.toMap())

        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $appName", e)
            Result.failure(e)
        }
    }

    /**
     * Check if an app is installed.
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Open Play Store for app installation.
     */
    private fun openPlayStore(packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Play Store", e)
        }
    }

    /**
     * Resolve app name to package name.
     *
     * Supports:
     * - Stone app names (e.g., "tick", "listen")
     * - Common app names (e.g., "spotify", "maps")
     * - Direct package names (e.g., "com.spotify.music")
     */
    private fun resolvePackageName(appName: String): String? {
        val normalized = appName.lowercase().trim()

        // Check if it's a Stone app
        STONE_APP_NAMES[normalized]?.let { return it }

        // Check if it's a common third-party app
        COMMON_APP_PACKAGES[normalized]?.let { return it }

        // Check if it's already a package name (contains dots)
        if (normalized.contains(".")) {
            return normalized
        }

        return null
    }
}

// Helper extension to convert JsonObject to Map
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
