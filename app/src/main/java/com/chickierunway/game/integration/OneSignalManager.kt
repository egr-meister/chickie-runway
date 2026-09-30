package com.chickierunway.game.integration

import android.app.Application
import android.util.Log
import com.chickierunway.game.BuildConfig
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object OneSignalManager {

    private const val TAG = "OneSignalManager"

    @Volatile
    private var initialized = false

    @Volatile
    private var loggedInAs: String? = null

    /** Must run after AppsFlyerManager.init — the external id is the AppsFlyer UID. */
    fun init(app: Application) {
        val appId = BuildConfig.ONESIGNAL_APP_ID
        if (appId.isBlank()) {
            Log.w(TAG, "ONESIGNAL_APP_ID is empty - push disabled for this build")
            return
        }
        if (BuildConfig.DEBUG) OneSignal.Debug.logLevel = LogLevel.VERBOSE

        OneSignal.initWithContext(app, appId)
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val url = event.notification.additionalData?.optString("url").orEmpty()
                if (url.isNotBlank()) DeepLinkRouter.handle(url)
            }
        })
        initialized = true

        // A returning install already has the UID stored — link it now, not after AF answers.
        syncExternalId()
    }

    /** OneSignal external_id = AppsFlyer UID. Safe to call any time, from any thread. */
    fun syncExternalId() {
        if (!initialized) return
        val appsFlyerId = IntegrationStorage.appsFlyerId
        if (appsFlyerId.isNullOrBlank() || appsFlyerId == loggedInAs) return
        runCatching { OneSignal.login(appsFlyerId) }
            .onSuccess { loggedInAs = appsFlyerId }
            .onFailure { Log.w(TAG, "OneSignal.login failed", it) }
    }

    /** requestPermission is suspend in OneSignal 5.x, so it can only run from a coroutine. */
    fun requestPermission() {
        if (!initialized) return
        CoroutineScope(Dispatchers.Main).launch {
            runCatching { OneSignal.Notifications.requestPermission(true) }
                .onFailure { Log.w(TAG, "Notification permission request failed", it) }
        }
    }
}
