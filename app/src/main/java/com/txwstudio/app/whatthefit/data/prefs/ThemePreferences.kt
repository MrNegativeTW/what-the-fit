package com.txwstudio.app.whatthefit.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.txwstudio.app.whatthefit.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val modeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[modeKey] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[modeKey] = mode.name }
    }
}
