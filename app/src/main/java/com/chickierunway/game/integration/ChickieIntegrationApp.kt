package com.chickierunway.game.integration

import com.chickierunway.game.ChickieApp

/**
 * Extends the game's own Application so its repository and sound setup run first, unchanged.
 * Order matters: storage first (everything else reads from it), the accelerometer window begins at
 * launch, then Adjust, then the push token — it has to exist before the single click.
 */
class ChickieIntegrationApp : ChickieApp() {
    override fun onCreate() {
        super.onCreate()
        IntegrationStorage.init(this)
        DeviceSignals.init(this)
        // The sampling window is a full second, so it has to begin at launch.
        AccelerometerProbe.start(this)
        // Reading GAID is blocking — start it early, in the background.
        GaidProbe.start(this)
        AdjustManager.init(this)
        PushManager.init(this)
    }
}
