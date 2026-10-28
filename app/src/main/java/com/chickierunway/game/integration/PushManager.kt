package com.chickierunway.game.integration

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Firebase Cloud Messaging, direct. The push service identifies a device by the `push_token` that
 * arrives in the tracker with the click, and an install has exactly one click (see
 * adjust-integration, routing). A token that misses it is never seen again, so it is requested in
 * Application.onCreate and awaited, bounded, while the offer URL is built.
 */
object PushManager {

    private const val TAG = "PushManager"
    private const val AWAIT_TIMEOUT_SECONDS = 3L
    private val ready = CountDownLatch(1)

    fun init(app: Application) {
        // A stored token is the answer immediately: later launches wait for nothing.
        if (!IntegrationStorage.pushToken.isNullOrBlank()) {
            ready.countDown()
            return
        }
        // getInstance throws if Firebase is not configured (a stub google-services.json). That is
        // a build without push, not a crash: open the latch and move on.
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) IntegrationStorage.pushToken = task.result
                else Log.w(TAG, "FCM token request failed", task.exception)
                // Open the latch on failure too: "no token right now" is also an answer.
                ready.countDown()
            }
        }.onFailure {
            Log.w(TAG, "FirebaseMessaging unavailable - push disabled for this build", it)
            ready.countDown()
        }
    }

    /** Background thread only: blocks up to a few seconds. */
    fun awaitToken(): String {
        runCatching { ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        return IntegrationStorage.pushToken.orEmpty()
    }
}
