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

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WorkoutSet(
    val w: Double? = null,
    val r: Int? = null,
    val note: String? = null
)

@Serializable
data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: List<WorkoutSet>,
    val note: String = ""
)

@Serializable
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // ISO date string YYYY-MM-DD
    val label: String = "",
    val note: String = "",
    val exercises: List<Exercise>
)

// Added versioning capability to support converting between schema versions
@Serializable
data class GymLogaData(
    val version: Int = 1,
    val sessions: List<Session>
)

object DataLogic {
    fun parseSets(raw: String): List<WorkoutSet> {
        val t = raw.trim()
        if (t.isEmpty()) return emptyList()

        // Regex: 135x5x3
        val wxrxs = Regex("""^(\d+(?:\.\d+)?)\s*x\s*(\d+)\s*x\s*(\d+)$""", RegexOption.IGNORE_CASE).find(t)
        if (wxrxs != null) {
            val w = wxrxs.groupValues[1].toDoubleOrNull() ?: 0.0
            val r = wxrxs.groupValues[2].toIntOrNull() ?: 0
            val s = wxrxs.groupValues[3].toIntOrNull() ?: 0
            return List(s) { WorkoutSet(w = w, r = r) }
        }

        // Regex: 135x5
        val wxr = Regex("""^(\d+(?:\.\d+)?)\s*x\s*(\d+)$""", RegexOption.IGNORE_CASE).find(t)
        if (wxr != null) {
            val w = wxr.groupValues[1].toDoubleOrNull() ?: 0.0
            val r = wxr.groupValues[2].toIntOrNull() ?: 0
            return listOf(WorkoutSet(w = w, r = r))
        }

        // Freeform with weight/reps
        val freeWxR = Regex("""(\d+(?:\.\d+)?)\s*x\s*(\d+)""", RegexOption.IGNORE_CASE).find(t)
        if (freeWxR != null) {
            val w = freeWxR.groupValues[1].toDoubleOrNull() ?: 0.0
            val r = freeWxR.groupValues[2].toIntOrNull() ?: 0
            return listOf(WorkoutSet(w = w, r = r, note = t))
        }

        return listOf(WorkoutSet(note = t))
    }

    fun getAllExerciseNames(sessions: List<Session>): List<String> {
        return sessions.flatMap { sess -> sess.exercises.map { it.name } }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    data class ExerciseHistoryEntry(
        val date: String,
        val label: String,
        val sets: List<WorkoutSet>,
        val bestW: Double,
        val bestR: Int,
        val note: String
    )

    fun getExerciseHistory(sessions: List<Session>, name: String): List<ExerciseHistoryEntry> {
        val lower = name.lowercase()
        return sessions.flatMap { sess ->
            sess.exercises.filter { it.name.lowercase() == lower }.map { ex ->
                val bestSet = ex.sets.filter { it.w != null }.maxByOrNull { it.w!! }
                ExerciseHistoryEntry(
                    date = sess.date,
                    label = sess.label,
                    sets = ex.sets,
                    bestW = bestSet?.w ?: 0.0,
                    bestR = bestSet?.r ?: 0,
                    note = ex.note
                )
            }
        }.sortedByDescending { it.date }
    }

    data class PRRecord(
        val name: String,
        val bestW: Double,
        val bestWR: Int,
        val bestWDate: String,
        val bestE1rm: Double,
        val bestE1rmW: Double,
        val bestE1rmR: Int,
        val bestE1rmDate: String,
        val totalSets: Int,
        val totalSessions: Int
    )

    fun getAllPRs(sessions: List<Session>): List<PRRecord> {
        val map = mutableMapOf<String, PRRecord>()

        for (sess in sessions) {
            for (ex in sess.exercises) {
                val lower = ex.name.lowercase()
                val current = map[lower] ?: PRRecord(
                    name = ex.name, bestW = 0.0, bestWR = 0, bestWDate = "",
                    bestE1rm = 0.0, bestE1rmW = 0.0, bestE1rmR = 0, bestE1rmDate = "",
                    totalSets = 0, totalSessions = 0
                )

                var newBestW = current.bestW
                var newBestWR = current.bestWR
                var newBestWDate = current.bestWDate
                var newBestE1rm = current.bestE1rm
                var newBestE1rmW = current.bestE1rmW
                var newBestE1rmR = current.bestE1rmR
                var newBestE1rmDate = current.bestE1rmDate
                var sessionSetCount = 0

                for (s in ex.sets) {
                    sessionSetCount++
                    val w = s.w ?: 0.0
                    val r = s.r ?: 0
                    if (w > 0.0 && r > 0) {
                        if (w > newBestW) {
                            newBestW = w
                            newBestWR = r
                            newBestWDate = sess.date
                        }
                        // Epley formula: e1RM = w * (1 + r/30)
                        val e = w * (1.0 + r.toDouble() / 30.0)
                        if (e > newBestE1rm) {
                            newBestE1rm = e
                            newBestE1rmW = w
                            newBestE1rmR = r
                            newBestE1rmDate = sess.date
                        }
                    }
                }

                map[lower] = current.copy(
                    bestW = newBestW, bestWR = newBestWR, bestWDate = newBestWDate,
                    bestE1rm = newBestE1rm, bestE1rmW = newBestE1rmW, bestE1rmR = newBestE1rmR,
                    bestE1rmDate = newBestE1rmDate,
                    totalSets = current.totalSets + sessionSetCount,
                    totalSessions = current.totalSessions + 1
                )
            }
        }

        return map.values.filter { it.bestW > 0.0 }.sortedByDescending { it.bestE1rm }
    }
}
