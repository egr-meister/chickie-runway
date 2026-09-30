package com.chickierunway.game

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.chickierunway.game.audio.SoundManager
import com.chickierunway.game.data.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "chickie_runway")

/** Creates the single repository and sound manager (no DI framework). */
open class ChickieApp : Application() {

    lateinit var repository: GameRepository
        private set

    lateinit var soundManager: SoundManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        repository = GameRepository(gameDataStore)
        soundManager = SoundManager(this)
        // Sync the persisted sound toggle into the SoundManager at startup and on every change.
        appScope.launch {
            repository.soundEnabled.collectLatest { soundManager.setEnabled(it) }
        }
    }
}
