package com.ironlog.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironlog.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class IronView { LOG, HISTORY, PRS, SESSION_DETAIL, EXERCISE_HISTORY }

class IronLogViewModel(private val repository: SessionRepository) : ViewModel() {
    val sessions: StateFlow<List<Session>> = repository.sessionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var currentView by mutableStateOf(IronView.LOG)
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

    fun saveSession() {
        if (aExercises.isEmpty()) return
        val session = Session(
            id = editSessionId ?: java.util.UUID.randomUUID().toString(),
            date = aDate,
            label = aLabel.trim(),
            note = aNote.trim(),
            exercises = aExercises.toList()
        )
        
        val updatedSessions = if (editSessionId != null) {
            sessions.value.map { if (it.id == editSessionId) session else it }
        } else {
            listOf(session) + sessions.value
        }
        
        viewModelScope.launch {
            repository.saveSessions(updatedSessions)
            clearLog()
            currentView = IronView.HISTORY
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
        currentView = IronView.LOG
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.saveSessions(sessions.value.filter { it.id != sessionId })
            currentView = IronView.HISTORY
        }
    }
}
