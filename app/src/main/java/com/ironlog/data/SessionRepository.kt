package com.ironlog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ironlog_prefs")

class SessionRepository(private val context: Context) {
    private val SESSIONS_KEY = stringPreferencesKey("ironlog:sessions")

    val sessionsFlow: Flow<List<Session>> = context.dataStore.data.map { preferences ->
        val sessionsJson = preferences[SESSIONS_KEY] ?: "[]"
        try {
            // implicit version 0 was unversioned, so we upconvert
            val element = Json.parseToJsonElement(sessionsJson)
            if (element is JsonArray) {
                // Migrate old format (direct list)
                Json.decodeFromJsonElement<List<Session>>(element)
            } else {
                // this is version 1, for now the current version
                // New format (wrapped in IronLogData)
                Json.decodeFromJsonElement<IronLogData>(element).sessions
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveSessions(sessions: List<Session>) {
        context.dataStore.edit { preferences ->
            val data = IronLogData(version = 1, sessions = sessions)
            preferences[SESSIONS_KEY] = Json.encodeToString(data)
        }
    }
}
