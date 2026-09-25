package com.chickierunway.game.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Single local source of truth. Stores [GameData] as one serialized JSON string.
 * Reads never crash (I/O errors and bad JSON -> defaults); writes report success as Boolean.
 */
class GameRepository(private val dataStore: DataStore<Preferences>) {

    private val dataKey = stringPreferencesKey(KEY_GAME_DATA)

    val gameData: Flow<GameData> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> GameSerializer.decode(prefs[dataKey]) }
        .distinctUntilChanged()

    val soundEnabled: Flow<Boolean> = gameData
        .map { it.settings.soundEnabled }
        .distinctUntilChanged()

    /** Keeps the higher of the stored and the new score for [categoryId]. */
    suspend fun saveQuizBest(categoryId: String, score: Int): Boolean = update { data ->
        val safeScore = score.coerceIn(0, GameData.MAX_QUIZ_SCORE)
        val old = data.quizBest[categoryId] ?: 0
        if (categoryId.isBlank() || safeScore <= old) data
        else data.copy(quizBest = data.quizBest + (categoryId to safeScore))
    }

    /** Keeps the higher of the stored and the new matched-pairs count for [level]. */
    suspend fun savePairsBest(level: Int, matchedPairs: Int): Boolean = update { data ->
        val key = level.toString()
        val safe = matchedPairs.coerceIn(0, GameData.MAX_PAIRS)
        val old = data.pairsBest[key] ?: 0
        if (level < 1 || safe <= old) data
        else data.copy(pairsBest = data.pairsBest + (key to safe))
    }

    suspend fun setSoundEnabled(enabled: Boolean): Boolean = update { data ->
        data.copy(settings = data.settings.copy(soundEnabled = enabled))
    }

    suspend fun toggleSound(): Boolean = update { data ->
        data.copy(settings = data.settings.copy(soundEnabled = !data.settings.soundEnabled))
    }

    /** Clears ALL progress (quiz bests + pairs bests). The sound preference is kept. */
    suspend fun resetAll(): Boolean = update { data -> GameData(settings = data.settings) }

    private suspend fun update(transform: (GameData) -> GameData): Boolean = try {
        dataStore.edit { prefs ->
            val current = GameSerializer.decode(prefs[dataKey])
            val next = transform(current)
            if (next != current || prefs[dataKey] == null) {
                prefs[dataKey] = GameSerializer.encode(next)
            }
        }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    companion object {
        const val KEY_GAME_DATA = "game_data_json"
    }
}
