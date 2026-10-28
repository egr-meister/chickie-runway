package com.chickierunway.game.ui

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.chickierunway.game.R

/**
 * Per-app language through AppCompatDelegate, which is the single source of truth: no copy in
 * DataStore, or it drifts once the user changes the language in system app settings (API 33+).
 *
 * "System" is the EMPTY locale list, not a particular language. English is the `values`
 * fallback, so any language outside this list lands in English, not in Russian.
 */
object AppLanguage {

    /** A choice in Settings. [tag] null = follow the system. Each language is named in itself. */
    data class Option(val tag: String?, @StringRes val label: Int)

    val options: List<Option> = listOf(
        Option(null, R.string.language_system),
        Option("en", R.string.language_en),
        Option("ru", R.string.language_ru),
        Option("fr", R.string.language_fr),
    )

    fun apply(tag: String?) {
        val locales =
            if (tag.isNullOrBlank()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** The current choice: null means "system", never "English". Only the language part is used. */
    fun currentTag(): String? =
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .takeIf { it.isNotBlank() }
            ?.substringBefore(',')
            ?.substringBefore('-')

    fun current(): Option = options.firstOrNull { it.tag == currentTag() } ?: options.first()
}
