package com.stonelauncher.controllers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Controller for navigation and maps operations.
 *
 * TICKET_007: Tool Calling Integration
 *
 * Handles Google Maps integration for navigation.
 * No special permissions required (uses implicit intents).
 */
class NavigationController(private val context: Context) {

    companion object {
        private const val TAG = "NavigationController"
        private const val MAPS_PACKAGE = "com.google.android.apps.maps"
    }

    /**
     * Navigate to a destination using Google Maps.
     *
     * @param destination Address or place name
     * @param mode Transportation mode: "driving", "walking", "bicycling", "transit"
     * @return Result with navigation details
     */
    fun navigateTo(destination: String, mode: String = "driving"): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Navigating to: $destination (mode: $mode)")

            if (destination.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("Destination cannot be empty")
                )
            }

            // Validate mode
            val modeCode = when (mode.lowercase()) {
                "driving" -> "d"
                "walking" -> "w"
                "bicycling" -> "b"
                "transit" -> "r"
                "two-wheeler" -> "l"
                else -> "d" // Default to driving
            }

            val encodedDestination = Uri.encode(destination)
            val uri = Uri.parse("google.navigation:q=$encodedDestination&mode=$modeCode")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(MAPS_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            Log.i(TAG, "Navigation started to: $destination")
            Result.success(buildJsonObject {
                put("navigation_started", JsonPrimitive(true))
                put("destination", JsonPrimitive(destination))
                put("mode", JsonPrimitive(mode))
            }.toMap())

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Google Maps not installed", e)
            Result.failure(Exception("Google Maps is not installed on this device"))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting navigation", e)
            Result.failure(e)
        }
    }

    /**
     * Show a location on the map (doesn't start navigation).
     *
     * @param query Place name or address
     * @return Result with map details
     */
    fun showLocation(query: String): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Showing location: $query")

            if (query.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("Query cannot be empty")
                )
            }

            val encodedQuery = Uri.encode(query)
            val uri = Uri.parse("geo:0,0?q=$encodedQuery")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(MAPS_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            Log.i(TAG, "Showing location: $query")
            Result.success(buildJsonObject {
                put("location_shown", JsonPrimitive(true))
                put("query", JsonPrimitive(query))
            }.toMap())

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Google Maps not installed", e)
            Result.failure(Exception("Google Maps is not installed on this device"))
        } catch (e: Exception) {
            Log.e(TAG, "Error showing location", e)
            Result.failure(e)
        }
    }

    /**
     * Show location by coordinates.
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @param label Optional label for the location
     * @return Result with map details
     */
    fun showCoordinates(
        latitude: Double,
        longitude: Double,
        label: String? = null
    ): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Showing coordinates: $latitude, $longitude")

            val uri = if (label != null) {
                val query = "$latitude,$longitude($label)"
                val encodedQuery = Uri.encode(query)
                Uri.parse("geo:$latitude,$longitude?q=$encodedQuery&z=16")
            } else {
                Uri.parse("geo:$latitude,$longitude?z=16")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(MAPS_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            Log.i(TAG, "Showing coordinates: $latitude, $longitude")
            Result.success(buildJsonObject {
                put("location_shown", JsonPrimitive(true))
                put("latitude", JsonPrimitive(latitude))
                put("longitude", JsonPrimitive(longitude))
                label?.let { put("label", JsonPrimitive(it)) }
            }.toMap())

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Google Maps not installed", e)
            Result.failure(Exception("Google Maps is not installed on this device"))
        } catch (e: Exception) {
            Log.e(TAG, "Error showing coordinates", e)
            Result.failure(e)
        }
    }
}

// Helper extension
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
