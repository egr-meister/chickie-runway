package com.chickierunway.game

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chickierunway.game.ui.components.LocalSoundManager
import com.chickierunway.game.ui.navigation.AppNavHost
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
                    AppNavHost(app = app)
                }
            }
        }
    }
}
