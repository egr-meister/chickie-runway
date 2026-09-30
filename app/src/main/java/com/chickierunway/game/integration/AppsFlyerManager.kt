package com.chickierunway.game.integration

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.chickierunway.game.BuildConfig

object AppsFlyerManager {

    private const val TAG = "AppsFlyerManager"

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isAttributionResolved: Boolean = false
        private set

    // Set from the main thread, read from AppsFlyer's worker thread — must be volatile, or the
    // worker may see null and a first install sits out the whole startup timeout.
    @Volatile
    private var onAttributionResolved: (() -> Unit)? = null

    private lateinit var appContext: Context

    fun init(app: Application) {
        appContext = app.applicationContext

        if (IntegrationStorage.attributionSettled) {
            // A previous launch already got its answer; nothing to wait for.
            isAttributionResolved = true
        }

        if (IntegrationStorage.appsFlyerHoldback) {
            Log.i(TAG, "Install is in the AppsFlyer holdback group - SDK not initialised")
            // No SDK means no callback will ever come; resolve now so startup does not sit
            // out the attribution timeout. Persisted, because the group never changes.
            markResolved()
            return
        }

        val devKey = BuildConfig.APPSFLYER_DEV_KEY
        if (devKey.isBlank()) {
            Log.w(TAG, "APPSFLYER_DEV_KEY is empty - AppsFlyer disabled for this build")
            // Unblock the UI, but do not persist attributionSettled: a build with a real key
            // must still wait for its own first callback.
            isAttributionResolved = true
            notifyResolved()
            return
        }

        val appsFlyer = AppsFlyerLib.getInstance()
        if (BuildConfig.DEBUG) appsFlyer.setDebugLog(true)
        appsFlyer.init(devKey, conversionListener, app)
        appsFlyer.start(app, devKey, object : AppsFlyerRequestListener {
            override fun onSuccess() {
                captureAppsFlyerId(app)
            }

            override fun onError(code: Int, message: String) {
                Log.w(TAG, "AppsFlyer start failed: code=$code, message=$message")
            }
        })
    }

    /**
     * The UID is not available synchronously after start(); it appears once the SDK has talked
     * to its backend, which is why several callbacks call this.
     */
    fun captureAppsFlyerId(context: Context) {
        val uid = runCatching { AppsFlyerLib.getInstance().getAppsFlyerUID(context) }
            .onFailure { Log.w(TAG, "getAppsFlyerUID failed", it) }
            .getOrNull()
        // null/empty means "not ready yet", never "no UID" — must not clear a stored one.
        if (uid.isNullOrBlank()) return
        IntegrationStorage.appsFlyerId = uid
        OneSignalManager.syncExternalId()
    }

    fun setOnAttributionResolved(callback: (() -> Unit)?) {
        onAttributionResolved = callback
        if (callback != null && isAttributionResolved) {
            // Resolved before anyone subscribed — deliver now instead of waiting out a timeout.
            mainHandler.post { callback() }
        }
    }

    private val conversionListener = object : AppsFlyerConversionListener {

        override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
            // Stored verbatim, campaign included. Picking "the keys we need" here is how
            // attribution silently goes missing when the network adds a key next month.
            // Merge (not replace): this also fires on re-attribution with partial payloads.
            data?.let { IntegrationStorage.mergeAttribution(it) }
            captureAppsFlyerId(appContext)
            markResolved()
        }

        override fun onConversionDataFail(error: String?) {
            Log.w(TAG, "Conversion data failed: $error")
            captureAppsFlyerId(appContext)
            // A failure is still an answer: no attribution is coming, stop waiting.
            markResolved()
        }

        override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
            // Merge: deep-link events carry few keys and must not wipe install attribution.
            data?.let { IntegrationStorage.mergeAttribution(it) }
        }

        override fun onAttributionFailure(error: String?) {
            Log.w(TAG, "App open attribution failed: $error")
        }
    }

    private fun markResolved() {
        isAttributionResolved = true
        IntegrationStorage.attributionSettled = true
        notifyResolved()
    }

    private fun notifyResolved() {
        val callback = onAttributionResolved ?: return
        // AppsFlyer calls back on a background thread; the waiter touches Compose state.
        mainHandler.post { callback() }
    }
}
