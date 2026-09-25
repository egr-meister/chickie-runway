package com.chickierunway.game.data

import kotlinx.serialization.json.Json

/**
 * Context-free JSON encode/decode for [GameData]. Decoding NEVER throws:
 * null, empty, blank, corrupted, wrong-shape or unknown-key payloads all fall back to defaults.
 */
object GameSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    fun encode(data: GameData): String = json.encodeToString(GameData.serializer(), data)

    fun decode(raw: String?): GameData {
        if (raw.isNullOrBlank()) return GameData()
        return try {
            json.decodeFromString(GameData.serializer(), raw).sanitized()
        } catch (e: Exception) {
            // SerializationException / IllegalArgumentException / anything else -> safe defaults.
            GameData()
        }
    }
}
