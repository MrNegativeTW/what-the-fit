package com.txwstudio.app.whatthefit.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers which category ids the user last checked on the Generate screen (F14). */
@Singleton
class SelectionPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val key = stringSetPreferencesKey("selected_category_ids")

    /** Null when the user has never made a selection (so callers can apply a default). */
    val selectedIdsOrNull: Flow<Set<Long>?> = dataStore.data.map { prefs ->
        prefs[key]?.mapNotNull { it.toLongOrNull() }?.toSet()
    }

    val selectedIds: Flow<Set<Long>> = selectedIdsOrNull.map { it ?: emptySet() }

    suspend fun setSelectedIds(ids: Set<Long>) {
        dataStore.edit { prefs ->
            prefs[key] = ids.map { it.toString() }.toSet()
        }
    }
}
