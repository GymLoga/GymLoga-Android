package com.mbosse.gymloga.ui

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbosse.gymloga.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

sealed class ExportImportEvent {
    object ExportSuccess : ExportImportEvent()
    data class ExportFailure(val message: String) : ExportImportEvent()
    data class ImportSuccess(val addedSessions: Int, val addedDefs: Int) : ExportImportEvent()
    data class ImportFailure(val message: String) : ExportImportEvent()
}

enum class GymView { LOG, HISTORY, PRS, SESSION_DETAIL, EXERCISE_HISTORY, ADD_EXERCISE, MANAGE_EXERCISES }

class GymLogaViewModel(private val repository: SessionRepository) : ViewModel() {
    private val _exportImportEvents = MutableSharedFlow<ExportImportEvent>()
    val exportImportEvents: SharedFlow<ExportImportEvent> = _exportImportEvents.asSharedFlow()

    val sessions: StateFlow<List<Session>> = repository.sessionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val exerciseDefinitions: StateFlow<List<ExerciseDefinition>> = repository.exerciseDefinitionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val prRecords: StateFlow<List<DataLogic.PRRecord>> = repository.prRecordsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val weightUnit: StateFlow<WeightUnit> = repository.weightUnitFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeightUnit.LBS
    )

    val targetReps: StateFlow<Int?> = repository.targetRepsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    // Log form state — all mutable form fields live here
    val logForm = LogFormState()

    // Navigation state
    var currentView by mutableStateOf(GymView.LOG)
    var editSessionId by mutableStateOf<String?>(null)
    var editDefinitionId by mutableStateOf<String?>(null)
    var selectedSession by mutableStateOf<Session?>(null)
    var selectedExerciseName by mutableStateOf<String?>(null)
    var exerciseHistorySource by mutableStateOf(GymView.HISTORY)

    // Computed hint for the currently active exercise
    var currentHint by mutableStateOf<DataLogic.SetHint?>(null)
        private set

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { repository.saveWeightUnit(unit) }
        currentHint = computeHint(logForm.curName, targetReps.value, unit)
    }

    fun setTargetReps(reps: Int?) {
        viewModelScope.launch { repository.saveTargetReps(reps) }
        currentHint = computeHint(logForm.curName, reps, weightUnit.value)
    }

    private fun computeHint(name: String, reps: Int?, unit: WeightUnit): DataLogic.SetHint? {
        if (reps == null || name.isBlank()) return null
        val pr = prRecords.value.find { it.name.lowercase() == name.lowercase() }
        val def = exerciseDefinitions.value.find { it.name.lowercase() == name.lowercase() }
        val lastSets = DataLogic.getExerciseHistory(sessions.value, name).firstOrNull()?.sets ?: emptyList()
        return DataLogic.suggestSets(pr, def?.equipmentType, reps, unit, lastSets)
    }

    private fun refreshHint() {
        currentHint = computeHint(logForm.curName, targetReps.value, weightUnit.value)
    }

    // Log form actions

    fun addSet() {
        val name = logForm.curName
        val raw = logForm.curSet
        if (name.isBlank() || raw.isBlank()) return
        val sets = DataLogic.parseSets(raw)
        if (sets.isEmpty()) return

        val existingIndex = logForm.aExercises.indexOfFirst { it.name.lowercase() == name.trim().lowercase() }
        if (existingIndex >= 0) {
            val ex = logForm.aExercises[existingIndex]
            logForm.aExercises[existingIndex] = ex.copy(sets = ex.sets + sets)
        } else {
            logForm.aExercises.add(Exercise(name = name.trim(), sets = sets, definitionId = logForm.curDefinitionId))
        }
        logForm.curSet = ""
    }

    fun selectExercise(name: String, definitionId: String? = null) {
        val trimmed = name.trim()
        logForm.curName = trimmed
        logForm.curDefinitionId = definitionId
        logForm.curSet = ""
        logForm.curExNote = ""
        logForm.showNoteInput = false
        if (logForm.aExercises.none { it.name.lowercase() == trimmed.lowercase() }) {
            logForm.aExercises.add(Exercise(name = trimmed, sets = emptyList(), definitionId = definitionId))
        }
        refreshHint()
    }

    fun clearCurrentExercise() {
        logForm.aExercises.removeAll { it.name.lowercase() == logForm.curName.lowercase() && it.sets.isEmpty() }
        logForm.curName = ""
        logForm.curDefinitionId = null
        logForm.curSet = ""
        logForm.curExNote = ""
        logForm.showNoteInput = false
        currentHint = null
    }

    fun addExNote() {
        val name = logForm.curName
        val note = logForm.curExNote
        if (name.isBlank() || note.isBlank()) return
        val existingIndex = logForm.aExercises.indexOfFirst { it.name.lowercase() == name.trim().lowercase() }
        if (existingIndex >= 0) {
            val oldNote = logForm.aExercises[existingIndex].note
            val newNote = if (oldNote.isEmpty()) note.trim() else "$oldNote\n${note.trim()}"
            logForm.aExercises[existingIndex] = logForm.aExercises[existingIndex].copy(note = newNote)
        }
        logForm.curExNote = ""
        logForm.showNoteInput = false
    }

    fun removeLastSet(exerciseId: String) {
        val index = logForm.aExercises.indexOfFirst { it.id == exerciseId }
        if (index >= 0) {
            val ex = logForm.aExercises[index]
            if (ex.sets.size > 1) {
                logForm.aExercises[index] = ex.copy(sets = ex.sets.dropLast(1))
            } else {
                logForm.aExercises.removeAt(index)
            }
        }
    }

    fun deleteExercise(exerciseId: String) {
        logForm.aExercises.removeAll { it.id == exerciseId }
    }

    // Exercise definition actions

    fun addExerciseDefinition(name: String, category: String, equipmentType: EquipmentType?) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = exerciseDefinitions.value
            if (existing.none { it.name.lowercase() == trimmed.lowercase() }) {
                repository.saveExerciseDefinitions(
                    existing + ExerciseDefinition(name = trimmed, category = category.trim(), equipmentType = equipmentType)
                )
            }
            selectExercise(trimmed)
            currentView = GymView.LOG
        }
    }

    fun renameExercise(defId: String, oldName: String, newName: String) {
        viewModelScope.launch { repository.renameExerciseDefinition(defId, oldName, newName) }
    }

    fun setExerciseActive(defId: String, active: Boolean) {
        viewModelScope.launch { repository.setExerciseDefinitionActive(defId, active) }
    }

    fun updateExerciseDefinition(defId: String, newName: String, newCategory: String, equipmentType: EquipmentType?) {
        val existing = exerciseDefinitions.value.find { it.id == defId } ?: return
        viewModelScope.launch {
            repository.updateExerciseDefinition(defId, newName.trim(), newCategory.trim(), existing.name, equipmentType)
        }
    }

    // Session actions

    fun saveSession() {
        val validExercises = logForm.aExercises.filter { it.sets.isNotEmpty() }
        if (validExercises.isEmpty()) return
        val session = Session(
            id = editSessionId ?: java.util.UUID.randomUUID().toString(),
            date = logForm.aDate,
            label = logForm.aLabel.trim(),
            note = logForm.aNote.trim(),
            exercises = validExercises
        )

        val updatedSessions = if (editSessionId != null) {
            sessions.value.map { if (it.id == editSessionId) session else it }
        } else {
            listOf(session) + sessions.value
        }

        viewModelScope.launch {
            repository.saveSessions(updatedSessions)
            clearLog()
            currentView = GymView.HISTORY
        }
    }

    fun clearLog() {
        logForm.clear()
        editSessionId = null
        currentHint = null
    }

    fun editSession(session: Session) {
        logForm.load(session)
        editSessionId = session.id
        currentView = GymView.LOG
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.saveSessions(sessions.value.filter { it.id != sessionId })
            currentView = GymView.HISTORY
        }
    }

    // Import / Export

    fun exportToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri)?.use { repository.exportToStream(it) }
                _exportImportEvents.emit(ExportImportEvent.ExportSuccess)
            } catch (e: Exception) {
                Log.e("GymLogaViewModel", "Export failed", e)
                _exportImportEvents.emit(ExportImportEvent.ExportFailure(e.message ?: "Unknown error"))
            }
        }
    }

    fun importFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imported = contentResolver.openInputStream(uri)?.use { repository.importFromStream(it) }
                    ?: run {
                        _exportImportEvents.emit(ExportImportEvent.ImportFailure("Could not open file"))
                        return@launch
                    }

                val existingSessionIds = sessions.value.map { it.id }.toSet()
                val newSessions = imported.sessions.filter { it.id !in existingSessionIds }

                // Merge definitions: add imported ones not already present by ID,
                // preserving local definitions (so local equipmentType edits survive)
                val existingDefIds = exerciseDefinitions.value.map { it.id }.toSet()
                val newDefs = imported.exerciseDefinitions.filter { it.id !in existingDefIds }

                repository.saveSessions(sessions.value + newSessions)
                if (newDefs.isNotEmpty()) {
                    repository.saveExerciseDefinitions(exerciseDefinitions.value + newDefs)
                }

                _exportImportEvents.emit(ExportImportEvent.ImportSuccess(newSessions.size, newDefs.size))
            } catch (e: SerializationException) {
                Log.e("GymLogaViewModel", "Import failed", e)
                _exportImportEvents.emit(ExportImportEvent.ImportFailure("Invalid file format"))
            } catch (e: Exception) {
                Log.e("GymLogaViewModel", "Import failed", e)
                _exportImportEvents.emit(ExportImportEvent.ImportFailure(e.message ?: "Unknown error"))
            }
        }
    }
}
