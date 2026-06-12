package com.kmhmubin.kothagolp.data.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "update_settings")

class UpdatePreferences(private val context: Context) {

    companion object {
        private val CHECK_UPDATES_ON_STARTUP = booleanPreferencesKey("check_updates_on_startup")
        private val HAS_SHOWN_STARTUP_UPDATE = booleanPreferencesKey("has_shown_startup_update")
        private val DISMISSED_VERSION = booleanPreferencesKey("dismissed_version")
    }

    val checkUpdatesOnStartup: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CHECK_UPDATES_ON_STARTUP] ?: true // Default enabled
    }

    suspend fun getCheckUpdatesOnStartup(): Boolean {
        return context.dataStore.data.first()[CHECK_UPDATES_ON_STARTUP] ?: true
    }

    suspend fun setCheckUpdatesOnStartup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CHECK_UPDATES_ON_STARTUP] = enabled
        }
    }
}