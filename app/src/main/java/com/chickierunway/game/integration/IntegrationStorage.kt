package com.chickierunway.game.integration

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.UUID

/** Integration state on its own SharedPreferences file, so it never collides with game data. */
object IntegrationStorage {

    private const val PREFS_NAME = "integration_prefs"
    private const val KEY_ATTRIBUTION = "attribution"
    private const val KEY_ADID = "adid"
    private const val KEY_GPS_ADID = "gps_adid"
    private const val KEY_PUSH_TOKEN = "push_token"
    private const val KEY_PUSH_ID = "push_id"
    private const val KEY_CACHED_OFFER_URL = "cached_offer_url"
    private const val KEY_CACHED_OFFER_URL_AT = "cached_offer_url_at"
    private const val KEY_WHITE_LOCKED = "white_locked"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- attribution ---------------------------------------------------------

    /**
     * Adds to the stored attribution. Adjust's listener can fire again on re-attribution with a
     * partial payload; dropping the keys it omits would throw away what the install already had.
     */
    fun mergeAttribution(data: Map<String, Any?>) {
        val merged = LinkedHashMap(attributionParams)
        merged.putAll(normalize(data))
        writeAttribution(merged)
    }

    fun attributionValue(key: String): String? = attributionParams[key]

    /** Everything stored, in the order it was recorded. */
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

    /** Adjust's device id. Empty means "not ready yet", never "none" — must not clear a stored one. */
    var adid: String?
        get() = prefs.getString(KEY_ADID, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_ADID, value).apply()
        }

    /** Google Advertising ID (GAID). Empty is ignored for the same reason as adid. */
    var gpsAdid: String?
        get() = prefs.getString(KEY_GPS_ADID, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_GPS_ADID, value).apply()
        }

    /** FCM registration token. Empty is ignored for the same reason as adid. */
    var pushToken: String?
        get() = prefs.getString(KEY_PUSH_TOKEN, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_PUSH_TOKEN, value).apply()
        }

    /**
     * A stable id for this install on the push side, generated once and kept for the life of the
     * install. It rides with push_token in the click and in pushed links.
     */
    val pushId: String
        @Synchronized get() {
            prefs.getString(KEY_PUSH_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
            val fresh = UUID.randomUUID().toString()
            // commit, not apply: two ids for one install would split it on the push side.
            prefs.edit().putString(KEY_PUSH_ID, fresh).commit()
            return fresh
        }

    // --- routing decision ----------------------------------------------------

    /**
     * The cached offer address if it is still fresh, otherwise null. A write with no timestamp
     * (from a previous app version) reads as expired, so an update heals installs stuck on a
     * stale address.
     */
    fun freshOfferUrl(now: Long = System.currentTimeMillis()): String? {
        val url = prefs.getString(KEY_CACHED_OFFER_URL, null)
        if (url.isNullOrBlank()) return null
        val savedAt = prefs.getLong(KEY_CACHED_OFFER_URL_AT, 0L)
        return if (OfferCache.isFresh(savedAt, now)) url else null
    }

    fun saveOfferUrl(url: String, now: Long = System.currentTimeMillis()) {
        if (url.isBlank()) return
        prefs.edit()
            .putString(KEY_CACHED_OFFER_URL, url)
            .putLong(KEY_CACHED_OFFER_URL_AT, now)
            .apply()
    }

    /** Clears ONLY the cached address. The White verdict is never touched. */
    fun clearOfferUrl() {
        prefs.edit().remove(KEY_CACHED_OFFER_URL).remove(KEY_CACHED_OFFER_URL_AT).apply()
    }

    /**
     * White is pinned forever. There is no time argument and no paired reset on purpose: a device
     * shown White must never later open the offer — that is exactly the cloaking pattern reviews
     * look for. The only way out is Clear data or reinstall, i.e. a new install.
     */
    val isWhiteLocked: Boolean get() = prefs.getBoolean(KEY_WHITE_LOCKED, false)

    fun lockWhite() {
        prefs.edit().putBoolean(KEY_WHITE_LOCKED, true).apply()
    }

    /** A branch is already pinned, so startup can skip the network entirely. */
    fun hasRouteDecision(now: Long = System.currentTimeMillis()): Boolean =
        isWhiteLocked || freshOfferUrl(now) != null

    // --- internals -----------------------------------------------------------

    private fun writeAttribution(params: Map<String, String>) {
        val json = JSONObject()
        params.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_ATTRIBUTION, json.toString()).apply()
    }

    /**
     * Attribution values are Any? — strings, numbers, nulls. Everything present is stringified and
     * kept, empty strings included ("sent empty" differs from "not sent"). A real null is skipped:
     * storing the text "null" would invent a value.
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
