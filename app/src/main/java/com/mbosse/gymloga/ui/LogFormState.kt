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
package com.mbosse.gymloga.ui

import androidx.compose.runtime.*
import com.mbosse.gymloga.data.Exercise
import com.mbosse.gymloga.data.Session
import java.time.LocalDate

class LogFormState {
    var aDate by mutableStateOf(LocalDate.now().toString())
    var aLabel by mutableStateOf("")
    var aNote by mutableStateOf("")
    val aExercises = mutableStateListOf<Exercise>()
    var curName by mutableStateOf("")
    var curDefinitionId by mutableStateOf<String?>(null)
    var curSet by mutableStateOf("")
    var curExNote by mutableStateOf("")
    var showNoteInput by mutableStateOf(false)

    val isDateValid: Boolean
        get() = aDate.matches(Regex("""\d{4}-\d{2}-\d{2}""")) &&
                runCatching { LocalDate.parse(aDate) }.isSuccess

    fun clear() {
        aDate = LocalDate.now().toString()
        aLabel = ""
        aNote = ""
        aExercises.clear()
        curName = ""
        curDefinitionId = null
        curSet = ""
        curExNote = ""
        showNoteInput = false
    }

    fun load(session: Session) {
        aDate = session.date
        aLabel = session.label
        aNote = session.note
        aExercises.clear()
        aExercises.addAll(session.exercises)
    }
}
