/*
* Copyright (C) 2026 Michael Bosse
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.mbosse.gymloga.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.OutputStream

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gymloga_prefs")

class SessionRepository(private val context: Context) {
    private val SESSIONS_KEY = stringPreferencesKey("gymloga:sessions")

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
                // New format (wrapped in GymLogaData)
                Json.decodeFromJsonElement<GymLogaData>(element).sessions
            }
        } catch (e: SerializationException) {
            android.util.Log.e("SessionRepository", "Failed to deserialize sessions", e)
            emptyList()
        }
    }

    suspend fun saveSessions(sessions: List<Session>) {
        context.dataStore.edit { preferences ->
            val data = GymLogaData(version = 1, sessions = sessions)
            preferences[SESSIONS_KEY] = Json.encodeToString(data)
        }
    }

    suspend fun exportToStream(outputStream: OutputStream) {
        val data = GymLogaData(version = 1, sessions = sessionsFlow.first())
        val prettyJson = kotlinx.serialization.json.Json { prettyPrint = true }
        outputStream.bufferedWriter().use { it.write(prettyJson.encodeToString(data)) }
    }

    fun importFromStream(inputStream: InputStream): List<Session> {
        val text = inputStream.bufferedReader().use { it.readText() }
        val element = Json.parseToJsonElement(text)
        return if (element is JsonArray) {
            Json.decodeFromJsonElement(element)
        } else {
            Json.decodeFromJsonElement<GymLogaData>(element).sessions
        }
    }
}
