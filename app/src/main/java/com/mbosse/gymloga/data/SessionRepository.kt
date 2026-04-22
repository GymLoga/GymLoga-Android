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

    private fun parseData(json: String): GymLogaData {
        val element = Json.parseToJsonElement(json)
        val data = if (element is JsonArray) {
            GymLogaData(sessions = Json.decodeFromJsonElement(element))
        } else {
            Json.decodeFromJsonElement(element)
        }
        // Seed exercise definitions from session history on first migration
        return if (data.exerciseDefinitions.isEmpty() && data.sessions.isNotEmpty()) {
            val seeded = DataLogic.getAllExerciseNames(data.sessions)
                .map { ExerciseDefinition(name = it) }
            data.copy(exerciseDefinitions = seeded)
        } else {
            data
        }
    }

    private val dataFlow: Flow<GymLogaData> = context.dataStore.data.map { preferences ->
        val json = preferences[SESSIONS_KEY] ?: "[]"
        try {
            parseData(json)
        } catch (e: SerializationException) {
            android.util.Log.e("SessionRepository", "Failed to deserialize data", e)
            GymLogaData(sessions = emptyList())
        }
    }

    val sessionsFlow: Flow<List<Session>> = dataFlow.map { it.sessions }

    val exerciseDefinitionsFlow: Flow<List<ExerciseDefinition>> = dataFlow.map { it.exerciseDefinitions }

    suspend fun saveSessions(sessions: List<Session>) {
        context.dataStore.edit { preferences ->
            val current = try {
                preferences[SESSIONS_KEY]?.let { parseData(it) }
                    ?: GymLogaData(sessions = emptyList())
            } catch (e: Exception) {
                GymLogaData(sessions = emptyList())
            }
            val data = current.copy(sessions = sessions)
            preferences[SESSIONS_KEY] = Json.encodeToString(data)
        }
    }

    suspend fun saveExerciseDefinitions(defs: List<ExerciseDefinition>) {
        context.dataStore.edit { preferences ->
            val current = try {
                preferences[SESSIONS_KEY]?.let { parseData(it) }
                    ?: GymLogaData(sessions = emptyList())
            } catch (e: Exception) {
                GymLogaData(sessions = emptyList())
            }
            val data = current.copy(exerciseDefinitions = defs)
            preferences[SESSIONS_KEY] = Json.encodeToString(data)
        }
    }

    suspend fun exportToStream(outputStream: OutputStream) {
        val data = dataFlow.first()
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
