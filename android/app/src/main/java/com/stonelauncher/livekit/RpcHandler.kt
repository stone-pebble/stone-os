package com.stonelauncher.livekit

import android.util.Log
import io.livekit.android.room.participant.RpcInvocationData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handler for RPC methods that match the React prototype patterns.
 *
 * TICKET_004: LiveKit Android SDK Integration
 *
 * Implements RPC methods from the React prototype:
 * - routeToAgent: Route requests to different agents
 * - getAvailableAgents: List available agents
 * - healthCheck: Health check endpoint
 * - sendBusySignal: Signal operation status (handled via data channel)
 */
object RpcHandler {

    private const val TAG = "RpcHandler"

    /**
     * Register all RPC methods with LiveKitManager.
     */
    fun registerAllMethods() {
        LiveKitManager.registerRpcMethod("routeToAgent", ::handleRouteToAgent)
        LiveKitManager.registerRpcMethod("getAvailableAgents", ::handleGetAvailableAgents)
        LiveKitManager.registerRpcMethod("healthCheck", ::handleHealthCheck)
        Log.d(TAG, "All RPC methods registered")
    }

    /**
     * Route to a specific agent type.
     *
     * Expected payload format:
     * {
     *   "agentType": "think|go|router|etc",
     *   "message": "user message"
     * }
     */
    private suspend fun handleRouteToAgent(data: RpcInvocationData): String {
        return try {
            val params = JSONObject(data.payload)
            val agentType = params.getString("agentType")
            val message = params.optString("message", "")

            Log.i(TAG, "Routing to $agentType with message: $message")

            // Return success response
            JSONObject().apply {
                put("success", true)
                put("routed", true)
                put("targetAgent", agentType)
                put("message", "Routing request acknowledged for $agentType")
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in routeToAgent", e)
            JSONObject().apply {
                put("success", false)
                put("error", e.message ?: "Unknown error")
            }.toString()
        }
    }

    /**
     * Get list of available agents.
     *
     * Returns the 12 Stone App agents.
     */
    private suspend fun handleGetAvailableAgents(data: RpcInvocationData): String {
        return try {
            val agents = JSONArray().apply {
                // Core Stone agents
                put(createAgentInfo("router", "Main routing agent"))
                put(createAgentInfo("tick", "Time management - alarms, timers, stopwatch"))
                put(createAgentInfo("pebbles", "Task management and app launcher"))
                put(createAgentInfo("set", "System settings - WiFi, Bluetooth, brightness"))
                put(createAgentInfo("listen", "Music control - Spotify integration"))
                put(createAgentInfo("ask", "Knowledge and search - Perplexity integration"))
                put(createAgentInfo("look", "Digital library - Project Gutenberg books"))
                put(createAgentInfo("plan", "Calendar and goal tracking"))
                put(createAgentInfo("think", "Note-taking and file management"))
                put(createAgentInfo("reflect", "Activity logging and daily summaries"))
                put(createAgentInfo("connect", "Communications - calls, SMS, email"))
                put(createAgentInfo("go", "Navigation and location services"))
                put(createAgentInfo("fund", "Payments and banking"))
            }

            JSONObject().apply {
                put("success", true)
                put("agents", agents)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAvailableAgents", e)
            JSONObject().apply {
                put("success", false)
                put("error", e.message ?: "Unknown error")
            }.toString()
        }
    }

    /**
     * Health check endpoint.
     */
    private suspend fun handleHealthCheck(data: RpcInvocationData): String {
        return try {
            JSONObject().apply {
                put("success", true)
                put("status", "healthy")
                put("timestamp", System.currentTimeMillis())
                put("platform", "android")
                put("version", "0.1.0-alpha")
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in healthCheck", e)
            JSONObject().apply {
                put("success", false)
                put("error", e.message ?: "Unknown error")
            }.toString()
        }
    }

    /**
     * Create agent info JSON object.
     */
    private fun createAgentInfo(name: String, description: String): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", description)
            put("available", true)
        }
    }

    /**
     * Send an operation status message via data channel.
     *
     * This matches the React prototype's sendBusySignal pattern.
     * Uses data channel instead of RPC for status updates.
     *
     * @param agent Agent name (e.g., "stone", "think")
     * @param operation Operation description (e.g., "processing", "searching")
     * @param busy Whether agent is busy
     * @param details Additional details
     * @param toolName Optional tool name being executed
     * @param stepDescription Optional step description
     * @param operationId Optional unique operation ID
     * @param progressPercentage Optional progress percentage (0-100)
     */
    suspend fun sendOperationStatus(
        agent: String,
        operation: String,
        busy: Boolean,
        details: String = "",
        toolName: String? = null,
        stepDescription: String? = null,
        operationId: String? = null,
        progressPercentage: Int? = null
    ) {
        try {
            val statusData = JSONObject().apply {
                put("type", "operation_status")
                put("agent", agent)
                put("busy", busy)
                put("operation", operation)
                put("details", details)
                toolName?.let { put("toolName", it) }
                stepDescription?.let { put("stepDescription", it) }
                operationId?.let { put("operationId", it) }
                progressPercentage?.let { put("progressPercentage", it) }
                put("timestamp", System.currentTimeMillis())
            }.toString()

            LiveKitManager.publishData(
                data = statusData.toByteArray(Charsets.UTF_8),
                topic = "agent_operation_status"
            )

            Log.d(TAG, "Sent operation status: $agent - $operation")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send operation status", e)
        }
    }
}
