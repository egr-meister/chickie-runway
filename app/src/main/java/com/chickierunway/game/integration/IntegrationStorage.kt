package com.chickierunway.game.integration

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Integration state on its own SharedPreferences file, so it never collides with game data. */
object IntegrationStorage {

    private const val PREFS_NAME = "integration_prefs"
    private const val KEY_ATTRIBUTION = "attribution"
    private const val KEY_ATTRIBUTION_SETTLED = "attribution_settled"
    private const val KEY_APPSFLYER_ID = "appsflyer_id"
    private const val KEY_CACHED_OFFER_URL = "cached_offer_url"
    private const val KEY_WHITE_LOCKED = "white_locked"
    private const val KEY_APPSFLYER_HOLDBACK = "appsflyer_holdback"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- attribution ---------------------------------------------------------

    /** Replaces the stored conversion data wholesale. */
    fun saveAttribution(data: Map<String, Any?>) {
        writeAttribution(normalize(data))
    }

    /**
     * Adds to the stored conversion data. Later events (onAppOpenAttribution, re-attribution)
     * carry fewer keys than the install event; dropping the ones they omit would throw away
     * the install attribution we already have.
     */
    fun mergeAttribution(data: Map<String, Any?>) {
        val merged = LinkedHashMap(attributionParams)
        merged.putAll(normalize(data))
        writeAttribution(merged)
    }

    fun attributionValue(key: String): String? = attributionParams[key]

    /** Everything stored, in the order AppsFlyer delivered it. */
    val attributionParams: LinkedHashMap<String, String>
        get() {
            val result = LinkedHashMap<String, String>()
            val raw = prefs.getString(KEY_ATTRIBUTION, null) ?: return result
            runCatching {
                val json = JSONObject(raw)
                // Android's JSONObject is backed by a LinkedHashMap, so keys() keeps insertion order.
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    result[key] = json.optString(key, "")
                }
            }
            return result
        }

    val hasAttribution: Boolean get() = attributionParams.isNotEmpty()

    /**
     * True once AppsFlyer answered at all — success or failure. Conversion data only arrives on
     * the install that carried it; without this flag every later launch would wait for nothing.
     */
    var attributionSettled: Boolean
        get() = prefs.getBoolean(KEY_ATTRIBUTION_SETTLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ATTRIBUTION_SETTLED, value).apply()

    var appsFlyerId: String?
        get() = prefs.getString(KEY_APPSFLYER_ID, null)
        set(value) {
            // Once we have a UID, a null/empty read arriving later must never replace it.
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_APPSFLYER_ID, value).apply()
        }

    /**
     * Whether this install is in the AppsFlyer holdback group. Rolled on first access and
     * persisted, so the answer never changes for the life of the install.
     *
     * An install that already talked to AppsFlyer (an update from a build without the
     * holdback) is never moved into it - switching attribution off mid-life would lose a
     * user who is already measured.
     */
    val appsFlyerHoldback: Boolean
        @Synchronized get() {
            if (prefs.contains(KEY_APPSFLYER_HOLDBACK)) {
                return prefs.getBoolean(KEY_APPSFLYER_HOLDBACK, false)
            }
            val alreadyMeasured = attributionSettled || !appsFlyerId.isNullOrBlank() || hasAttribution
            val holdback = !alreadyMeasured && AppsFlyerHoldback.roll()
            // commit, not apply: a process killed right after the roll must not re-roll.
            prefs.edit().putBoolean(KEY_APPSFLYER_HOLDBACK, holdback).commit()
            return holdback
        }

    // --- routing decision ----------------------------------------------------

    var cachedOfferUrl: String?
        get() = prefs.getString(KEY_CACHED_OFFER_URL, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_CACHED_OFFER_URL, value).apply()
        }

    val hasCachedOffer: Boolean get() = !cachedOfferUrl.isNullOrBlank()

    var whiteLocked: Boolean
        get() = prefs.getBoolean(KEY_WHITE_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_WHITE_LOCKED, value).apply()

    /** A branch is already pinned, so startup can skip the network entirely. */
    val hasRouteDecision: Boolean get() = whiteLocked || hasCachedOffer

    // --- internals -----------------------------------------------------------

    private fun writeAttribution(params: Map<String, String>) {
        val json = JSONObject()
        params.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_ATTRIBUTION, json.toString()).apply()
    }

    /**
     * Conversion data is Map<String, Any?> — numbers, booleans, nulls. Everything present is
     * stringified and kept, empty strings included (the tracker tells "sent empty" from "not
     * sent"). A real null is skipped: storing the text "null" would invent a value.
     */
    private fun normalize(data: Map<String, Any?>): LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        data.forEach { (key, value) ->
            if (key.isBlank() || value == null || value == JSONObject.NULL) return@forEach
            result[key] = value.toString()
        }
        return result
    }
}
