package com.chickierunway.game

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chickierunway.game.integration.DeepLinkRouter
import com.chickierunway.game.integration.PushService
import com.chickierunway.game.ui.ChickieRoot
import com.chickierunway.game.ui.components.LocalSoundManager
import com.chickierunway.game.ui.theme.ChickieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // Light sky background -> dark system-bar icons for contrast.
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val app = application as ChickieApp
        setContent {
            ChickieTheme {
                CompositionLocalProvider(LocalSoundManager provides app.soundManager) {
                    ChickieRoot(app = app)
                }
            }
        }
        consumeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop delivers a push tap here instead of creating a second Activity.
        setIntent(intent)
        consumeIntent(intent)
    }

    /**
     * A push tap carries the link in extras. The marker is written on the intent itself (not a
     * field on the Activity) so a recreation does not open the same push twice.
     */
    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        if (!intent.getBooleanExtra(PushService.EXTRA_FROM_PUSH, false)) return
        if (intent.getBooleanExtra(EXTRA_CONSUMED, false)) return
        intent.putExtra(EXTRA_CONSUMED, true)
        // A null/blank link is a push without one: the normal launch routing already shows the
        // offer to a Black install, so there is nothing extra to do.
        DeepLinkRouter.handle(intent.getStringExtra(PushService.EXTRA_PUSH_URL))
    }

    private companion object {
        const val EXTRA_CONSUMED = "chickie_push_consumed"
    }
}
