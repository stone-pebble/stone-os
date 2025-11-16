package com.stonelauncher.models

/**
 * Data model for a Stone app.
 *
 * Each Stone app has:
 * - id: unique identifier (lowercase)
 * - name: display name (lowercase, for minimalist UI)
 *
 * The 12 Stone apps:
 * tick, pebbles, set, listen, ask, look, plan, think, reflect, connect, go, fund
 */
data class StoneApp(
    val id: String,
    val name: String
)
