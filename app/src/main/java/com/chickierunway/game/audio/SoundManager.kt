package com.chickierunway.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.chickierunway.game.R
import java.util.concurrent.ConcurrentHashMap

enum class Sfx(@RawRes val resId: Int) {
    CLICK(R.raw.sfx_click),
    CORRECT(R.raw.sfx_correct),
    WRONG(R.raw.sfx_wrong),
    MATCH(R.raw.sfx_match),
    WIN(R.raw.sfx_win),
    LOSE(R.raw.sfx_lose)
}

/**
 * Short sound effects through [SoundPool]. Created once in the Application.
 * All calls are safe: failures to load or play are ignored (the game stays silent, never crashes).
 */
class SoundManager(context: Context) {

    @Volatile
    var enabled: Boolean = true
        private set

    private val soundIds = ConcurrentHashMap<Sfx, Int>()
    private val loadedIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val soundPool: SoundPool? = try {
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    } catch (e: Exception) {
        null
    }

    init {
        val pool = soundPool
        if (pool != null) {
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) loadedIds.add(sampleId)
            }
            val appContext = context.applicationContext
            Sfx.entries.forEach { sfx ->
                try {
                    soundIds[sfx] = pool.load(appContext, sfx.resId, 1)
                } catch (e: Exception) {
                    // Missing/broken resource -> that effect is simply silent.
                }
            }
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            try {
                soundPool?.autoPause()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val pool = soundPool ?: return
        val id = soundIds[sfx] ?: return
        if (id !in loadedIds) return
        try {
            val volume = if (sfx == Sfx.CLICK) 0.6f else 0.9f
            pool.play(id, volume, volume, 1, 0, 1f)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        try {
            soundPool?.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
