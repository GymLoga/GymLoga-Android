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
    private val WEIGHT_UNIT_KEY = stringPreferencesKey("gymloga:weight_unit")
    private val TARGET_REPS_KEY = stringPreferencesKey("gymloga:target_reps")

    private val json = Json { ignoreUnknownKeys = true }

    private fun parseData(rawJson: String): GymLogaData {
        val element = json.parseToJsonElement(rawJson)
        val data = if (element is JsonArray) {
            GymLogaData(sessions = json.decodeFromJsonElement(element))
        } else {
            json.decodeFromJsonElement(element)
        }
        return when {
            data.exerciseDefinitions.isNotEmpty() -> data
            data.sessions.isNotEmpty() -> {
                // Migrate: seed definitions from existing session history
                val seeded = DataLogic.getAllExerciseNames(data.sessions).map { ExerciseDefinition(name = it) }
                data.copy(exerciseDefinitions = seeded)
            }
            else -> {
                // Fresh install: provide a starter set with known equipment types
                val defaults = listOf(
                    ExerciseDefinition(name = "Bench Press", equipmentType = EquipmentType.BARBELL),
                    ExerciseDefinition(name = "Overhead Press", equipmentType = EquipmentType.BARBELL),
                    ExerciseDefinition(name = "Power Clean", equipmentType = EquipmentType.BARBELL),
                    ExerciseDefinition(name = "Deadlift", equipmentType = EquipmentType.BARBELL),
                    ExerciseDefinition(name = "Back Squat", equipmentType = EquipmentType.BARBELL)
                )
                data.copy(exerciseDefinitions = defaults)
            }
        }
    }

    private val dataFlow: Flow<GymLogaData> = context.dataStore.data.map { preferences ->
        val rawJson = preferences[SESSIONS_KEY] ?: "[]"
        try {
            parseData(rawJson)
        } catch (e: SerializationException) {
            android.util.Log.e("SessionRepository", "Failed to deserialize data", e)
            GymLogaData(sessions = emptyList())
        }
    }

    val sessionsFlow: Flow<List<Session>> = dataFlow.map { it.sessions }

    val exerciseDefinitionsFlow: Flow<List<ExerciseDefinition>> = dataFlow.map { it.exerciseDefinitions }

    val prRecordsFlow: Flow<List<DataLogic.PRRecord>> = sessionsFlow.map { DataLogic.getAllPRs(it) }

    val weightUnitFlow: Flow<WeightUnit> = context.dataStore.data.map { prefs ->
        when (prefs[WEIGHT_UNIT_KEY]) {
            "KG" -> WeightUnit.KG
            else -> WeightUnit.LBS
        }
    }

    val targetRepsFlow: Flow<Int?> = context.dataStore.data.map { prefs ->
        when (val stored = prefs[TARGET_REPS_KEY]) {
            null -> 5       // first launch default
            "off" -> null   // user explicitly disabled hints
            else -> stored.toIntOrNull()
        }
    }

    suspend fun saveWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { it[WEIGHT_UNIT_KEY] = unit.name }
    }

    suspend fun saveTargetReps(reps: Int?) {
        context.dataStore.edit { it[TARGET_REPS_KEY] = reps?.toString() ?: "off" }
    }

    suspend fun saveSessions(sessions: List<Session>) {
        context.dataStore.edit { preferences ->
            val current = try {
                preferences[SESSIONS_KEY]?.let { parseData(it) }
                    ?: GymLogaData(sessions = emptyList())
            } catch (e: Exception) {
                GymLogaData(sessions = emptyList())
            }
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(sessions = sessions))
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
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = defs))
        }
    }

    suspend fun renameExerciseDefinition(defId: String, oldName: String, newName: String) {
        context.dataStore.edit { preferences ->
            val current = try {
                preferences[SESSIONS_KEY]?.let { parseData(it) } ?: GymLogaData(sessions = emptyList())
            } catch (e: Exception) {
                GymLogaData(sessions = emptyList())
            }
            val updatedDefs = current.exerciseDefinitions.map {
                if (it.id == defId) it.copy(name = newName) else it
            }
            val updatedSessions = DataLogic.applyRename(current.sessions, defId, oldName, newName)
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = updatedDefs, sessions = updatedSessions))
        }
    }

    suspend fun updateExerciseDefinition(
        defId: String,
        newName: String,
        newCategory: String,
        oldName: String,
        equipmentType: EquipmentType?
    ) {
        context.dataStore.edit { preferences ->
            val current = try {
                preferences[SESSIONS_KEY]?.let { parseData(it) } ?: GymLogaData(sessions = emptyList())
            } catch (e: Exception) {
                GymLogaData(sessions = emptyList())
            }
            val nameChanged = newName != oldName
            val updatedDefs = current.exerciseDefinitions.map {
                if (it.id == defId) it.copy(name = newName, category = newCategory, equipmentType = equipmentType) else it
            }
            val updatedSessions = if (nameChanged)
                DataLogic.applyRename(current.sessions, defId, oldName, newName)
            else
                current.sessions
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = updatedDefs, sessions = updatedSessions))
        }
    }

    suspend fun setExerciseDefinitionActive(defId: String, active: Boolean) {
        context.dataStore.edit { preferences ->
            val current = try {
                preferences[SESSIONS_KEY]?.let { parseData(it) } ?: GymLogaData(sessions = emptyList())
            } catch (e: Exception) {
                GymLogaData(sessions = emptyList())
            }
            val updatedDefs = current.exerciseDefinitions.map {
                if (it.id == defId) it.copy(active = active) else it
            }
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = updatedDefs))
        }
    }

    suspend fun exportToStream(outputStream: OutputStream) {
        val data = dataFlow.first()
        val prettyJson = Json { prettyPrint = true }
        outputStream.bufferedWriter().use { it.write(prettyJson.encodeToString(data)) }
    }

    fun importFromStream(inputStream: InputStream): GymLogaData {
        val text = inputStream.bufferedReader().use { it.readText() }
        val element = json.parseToJsonElement(text)
        return if (element is JsonArray) {
            GymLogaData(sessions = json.decodeFromJsonElement(element))
        } else {
            json.decodeFromJsonElement(element)
        }
    }
}
