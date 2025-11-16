package com.stonelauncher.api

/**
 * Data class for Intent API results.
 *
 * Represents the result of processing an Intent API call, containing
 * either success data or error information.
 *
 * @property success Whether the operation succeeded
 * @property data Result data as key-value pairs (only present on success)
 * @property errorMessage Human-readable error message (only present on failure)
 * @property errorCode Machine-readable error code (optional, only present on failure)
 */
data class IntentResult(
    val success: Boolean,
    val data: Map<String, Any> = emptyMap(),
    val errorMessage: String? = null,
    val errorCode: String? = null
) {
    companion object {
        /**
         * Creates a successful result with optional data.
         *
         * @param data Result data as key-value pairs
         * @return IntentResult with success=true
         */
        fun success(data: Map<String, Any> = emptyMap()) = IntentResult(
            success = true,
            data = data
        )

        /**
         * Creates an error result with required message and optional error code.
         *
         * @param message Human-readable error message
         * @param code Optional machine-readable error code
         * @return IntentResult with success=false
         */
        fun error(message: String, code: String? = null) = IntentResult(
            success = false,
            errorMessage = message,
            errorCode = code
        )
    }
}
