package com.mbosse.gymloga.ui

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import java.time.LocalDate

sealed class ExportImportEvent {
    object ExportSuccess : ExportImportEvent()
    data class ExportFailure(val message: String) : ExportImportEvent()
    data class ImportSuccess(val added: Int) : ExportImportEvent()
    data class ImportFailure(val message: String) : ExportImportEvent()
}

enum class GymView { LOG, HISTORY, PRS, SESSION_DETAIL, EXERCISE_HISTORY, ADD_EXERCISE }

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

    var currentView by mutableStateOf(GymView.LOG)
    var editSessionId by mutableStateOf<String?>(null)

    // Log Form State
    var aDate by mutableStateOf(LocalDate.now().toString())
    var aLabel by mutableStateOf("")
    var aNote by mutableStateOf("")
    val aExercises = mutableStateListOf<Exercise>()

    var curName by mutableStateOf("")
    var curSet by mutableStateOf("")
    var curExNote by mutableStateOf("")
    var showNoteInput by mutableStateOf(false)

    val isDateValid: Boolean
        get() = aDate.matches(Regex("""\d{4}-\d{2}-\d{2}""")) &&
                runCatching { java.time.LocalDate.parse(aDate) }.isSuccess

    // Detail States
    var selectedSession by mutableStateOf<Session?>(null)
    var selectedExerciseName by mutableStateOf<String?>(null)

    fun addSet() {
        if (curName.isBlank() || curSet.isBlank()) return
        val sets = DataLogic.parseSets(curSet)
        if (sets.isEmpty()) return

        val existingIndex = aExercises.indexOfFirst { it.name.lowercase() == curName.trim().lowercase() }
        if (existingIndex >= 0) {
            val ex = aExercises[existingIndex]
            aExercises[existingIndex] = ex.copy(sets = ex.sets + sets)
        } else {
            aExercises.add(Exercise(name = curName.trim(), sets = sets))
        }
        curSet = ""
    }

    fun selectExercise(name: String) {
        val trimmed = name.trim()
        curName = trimmed
        curSet = ""
        curExNote = ""
        showNoteInput = false
        // Create a placeholder card so the edit controls appear immediately
        if (aExercises.none { it.name.lowercase() == trimmed.lowercase() }) {
            aExercises.add(Exercise(name = trimmed, sets = emptyList()))
        }
    }

    fun clearCurrentExercise() {
        aExercises.removeAll { it.name.lowercase() == curName.lowercase() && it.sets.isEmpty() }
        curName = ""
        curSet = ""
        curExNote = ""
        showNoteInput = false
    }

    fun addExNote() {
        if (curName.isBlank() || curExNote.isBlank()) return
        val existingIndex = aExercises.indexOfFirst { it.name.lowercase() == curName.trim().lowercase() }
        if (existingIndex >= 0) {
            val oldNote = aExercises[existingIndex].note
            val newNote = if (oldNote.isEmpty()) curExNote.trim() else "$oldNote\n${curExNote.trim()}"
            aExercises[existingIndex] = aExercises[existingIndex].copy(note = newNote)
        }
        curExNote = ""
        showNoteInput = false
    }

    fun removeLastSet(exerciseId: String) {
        val index = aExercises.indexOfFirst { it.id == exerciseId }
        if (index >= 0) {
            val ex = aExercises[index]
            if (ex.sets.size > 1) {
                aExercises[index] = ex.copy(sets = ex.sets.dropLast(1))
            } else {
                aExercises.removeAt(index)
            }
        }
    }

    fun deleteExercise(exerciseId: String) {
        aExercises.removeAll { it.id == exerciseId }
    }

    fun addExerciseDefinition(name: String, category: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = exerciseDefinitions.value
            if (existing.none { it.name.lowercase() == trimmed.lowercase() }) {
                repository.saveExerciseDefinitions(existing + ExerciseDefinition(name = trimmed, category = category.trim()))
            }
            selectExercise(trimmed)
            currentView = GymView.LOG
        }
    }

    fun saveSession() {
        val validExercises = aExercises.filter { it.sets.isNotEmpty() }
        if (validExercises.isEmpty()) return
        val session = Session(
            id = editSessionId ?: java.util.UUID.randomUUID().toString(),
            date = aDate,
            label = aLabel.trim(),
            note = aNote.trim(),
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
        aDate = LocalDate.now().toString()
        aLabel = ""
        aNote = ""
        aExercises.clear()
        curName = ""
        curSet = ""
        curExNote = ""
        editSessionId = null
        showNoteInput = false
    }

    fun editSession(session: Session) {
        aDate = session.date
        aLabel = session.label
        aNote = session.note
        aExercises.clear()
        aExercises.addAll(session.exercises)
        editSessionId = session.id
        currentView = GymView.LOG
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.saveSessions(sessions.value.filter { it.id != sessionId })
            currentView = GymView.HISTORY
        }
    }

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
                val existingIds = sessions.value.map { it.id }.toSet()
                val newSessions = imported.filter { it.id !in existingIds }
                repository.saveSessions(sessions.value + newSessions)
                _exportImportEvents.emit(ExportImportEvent.ImportSuccess(newSessions.size))
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
