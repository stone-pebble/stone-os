package com.stonelauncher.controllers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Controller for telephony operations (calls, SMS).
 *
 * TICKET_007: Tool Calling Integration
 *
 * Handles phone calls and SMS messaging.
 * Requires runtime permissions: CALL_PHONE, SEND_SMS
 */
class TelephonyController(private val context: Context) {

    companion object {
        private const val TAG = "TelephonyController"
    }

    /**
     * Make a phone call.
     *
     * Requires: CALL_PHONE permission
     *
     * @param phoneNumber Phone number to call
     * @return Result with call details
     */
    fun makeCall(phoneNumber: String): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Attempting to call: $phoneNumber")

            // Check permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
                return Result.failure(
                    SecurityException("CALL_PHONE permission not granted")
                )
            }

            // Validate phone number
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (cleanNumber.length < 10) {
                return Result.failure(
                    IllegalArgumentException("Invalid phone number: $phoneNumber")
                )
            }

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            Log.i(TAG, "Call initiated to: $cleanNumber")
            Result.success(buildJsonObject {
                put("call_initiated", JsonPrimitive(true))
                put("phone_number", JsonPrimitive(cleanNumber))
            }.toMap())

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for phone call", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
            Result.failure(e)
        }
    }

    /**
     * Send an SMS message.
     *
     * Requires: SEND_SMS permission
     *
     * @param phoneNumber Recipient phone number
     * @param message Message text
     * @return Result with send details
     */
    @Suppress("DEPRECATION")
    fun sendSMS(phoneNumber: String, message: String): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Attempting to send SMS to: $phoneNumber")

            // Check permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                return Result.failure(
                    SecurityException("SEND_SMS permission not granted")
                )
            }

            // Validate phone number
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (cleanNumber.length < 10) {
                return Result.failure(
                    IllegalArgumentException("Invalid phone number: $phoneNumber")
                )
            }

            // Validate message
            if (message.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("Message cannot be empty")
                )
            }

            if (message.length > 1600) {
                return Result.failure(
                    IllegalArgumentException("Message too long (max 1600 characters)")
                )
            }

            val smsManager = SmsManager.getDefault()

            // For long messages, divide into parts
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    cleanNumber,
                    null,
                    parts,
                    null,
                    null
                )
                Log.i(TAG, "Sent multipart SMS ($parts parts) to: $cleanNumber")
            } else {
                smsManager.sendTextMessage(
                    cleanNumber,
                    null,
                    message,
                    null,
                    null
                )
                Log.i(TAG, "Sent SMS to: $cleanNumber")
            }

            Result.success(buildJsonObject {
                put("sms_sent", JsonPrimitive(true))
                put("phone_number", JsonPrimitive(cleanNumber))
                put("message_length", JsonPrimitive(message.length))
            }.toMap())

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for SMS", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
            Result.failure(e)
        }
    }

    /**
     * Open dialer with pre-filled number (doesn't auto-call).
     *
     * No permission required.
     *
     * @param phoneNumber Phone number to dial
     * @return Result with dial details
     */
    fun openDialer(phoneNumber: String): Result<Map<String, Any>> {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            Log.i(TAG, "Opened dialer with: $cleanNumber")
            Result.success(buildJsonObject {
                put("dialer_opened", JsonPrimitive(true))
                put("phone_number", JsonPrimitive(cleanNumber))
            }.toMap())

        } catch (e: Exception) {
            Log.e(TAG, "Error opening dialer", e)
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
