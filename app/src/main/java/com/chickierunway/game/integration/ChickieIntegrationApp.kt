package com.chickierunway.game.integration

import com.chickierunway.game.ChickieApp

/**
 * Extends the game's own Application so its repository and sound setup run first, unchanged.
 * Order matters: storage before AppsFlyer (it reads attributionSettled), the WebView user agent
 * before anything can probe, and OneSignal after AppsFlyer so the external id can be synced.
 */
class ChickieIntegrationApp : ChickieApp() {
    override fun onCreate() {
        super.onCreate()
        IntegrationStorage.init(this)
        DeviceSignals.init(this)
        // The sampling window is a full second, so it has to begin at launch.
        AccelerometerProbe.start(this)
        UserAgentProvider.init(this)
        AppsFlyerManager.init(this)
        OneSignalManager.init(this)
    }
}
