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
    val t: Long? = null, // duration in seconds, for timed sets (planks, holds, carries...)
    val note: String? = null
)

@Serializable
data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: List<WorkoutSet>,
    val note: String = "",
    val definitionId: String? = null
)

@Serializable
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // ISO date string YYYY-MM-DD
    val label: String = "",
    val note: String = "",
    val exercises: List<Exercise>
)

@Serializable
data class ExerciseDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "",
    val active: Boolean = true
)

// Added versioning capability to support converting between schema versions
@Serializable
data class GymLogaData(
    val version: Int = 1,
    val sessions: List<Session>,
    val exerciseDefinitions: List<ExerciseDefinition> = emptyList()
)

object DataLogic {

    // Matches pure duration shorthand: 30m, 2h, 20s, 1h20m, 1h20m30s...
    private val DURATION_RE = Regex(
        """^(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""",
        RegexOption.IGNORE_CASE
    )

    /** Parses a pure duration token (e.g. "1h20m") into total seconds, or null if not a duration. */
    fun parseDurationSeconds(token: String): Long? {
        val m = DURATION_RE.find(token) ?: return null
        val hStr = m.groupValues[1]; val mStr = m.groupValues[2]; val sStr = m.groupValues[3]
        if (hStr.isEmpty() && mStr.isEmpty() && sStr.isEmpty()) return null // must have at least one unit
        val h = hStr.toLongOrNull() ?: 0L
        val mi = mStr.toLongOrNull() ?: 0L
        val s = sStr.toLongOrNull() ?: 0L
        return h * 3600 + mi * 60 + s
    }

    /** Formats a duration in seconds back into compact shorthand, e.g. 4820 -> "1h20m20s". */
    fun formatDuration(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val sb = StringBuilder()
        if (h > 0) sb.append("${h}h")
        if (m > 0) sb.append("${m}m")
        if (s > 0 || sb.isEmpty()) sb.append("${s}s")
        return sb.toString()
    }

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

        // Weightless (bodyweight) shorthand: x10x3 -> reps x sets, no weight
        val xrxs = Regex("""^x\s*(\d+)\s*x\s*(\d+)$""", RegexOption.IGNORE_CASE).find(t)
        if (xrxs != null) {
            val r = xrxs.groupValues[1].toIntOrNull() ?: 0
            val s = xrxs.groupValues[2].toIntOrNull() ?: 0
            return List(s) { WorkoutSet(r = r) }
        }

        // Weightless (bodyweight) shorthand: x10 -> one set of reps, no weight
        val xr = Regex("""^x\s*(\d+)$""", RegexOption.IGNORE_CASE).find(t)
        if (xr != null) {
            val r = xr.groupValues[1].toIntOrNull() ?: 0
            return listOf(WorkoutSet(r = r))
        }

        // Timed set, optionally repeated across sets: 30s, 2h, 1h20m, 30sx3
        val durXs = Regex("""^([0-9hms]+)\s*x\s*(\d+)$""", RegexOption.IGNORE_CASE).find(t)
        if (durXs != null) {
            val secs = parseDurationSeconds(durXs.groupValues[1])
            val s = durXs.groupValues[2].toIntOrNull() ?: 0
            if (secs != null && secs > 0) {
                return List(s) { WorkoutSet(t = secs) }
            }
        }
        val durOnly = parseDurationSeconds(t)
        if (durOnly != null && durOnly > 0) {
            return listOf(WorkoutSet(t = durOnly))
        }

        // Regex: 135 (bare weight, assume 1 rep)
        val wOnly = Regex("""^(\d+(?:\.\d+)?)$""").find(t)
        if (wOnly != null) {
            val w = wOnly.groupValues[1].toDoubleOrNull() ?: 0.0
            return listOf(WorkoutSet(w = w, r = 1))
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

    fun getSessionVolume(session: Session): Long =
        session.exercises.sumOf { ex ->
            ex.sets.sumOf { set -> ((set.w ?: 0.0) * (set.r ?: 0)).toLong() }
        }

    fun applyRename(sessions: List<Session>, defId: String, oldName: String, newName: String): List<Session> =
        sessions.map { sess ->
            sess.copy(exercises = sess.exercises.map { ex ->
                val matchById = ex.definitionId == defId
                val matchByName = ex.definitionId == null && ex.name.lowercase() == oldName.lowercase()
                if (matchById || matchByName) ex.copy(name = newName) else ex
            })
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
        val bestT: Long,
        val bestBWR: Int,
        val note: String
    )

    fun getExerciseHistory(sessions: List<Session>, name: String): List<ExerciseHistoryEntry> {
        val lower = name.lowercase()
        return sessions.flatMap { sess ->
            sess.exercises.filter { it.name.lowercase() == lower }.map { ex ->
                val bestSet = ex.sets.filter { it.w != null }.maxByOrNull { it.w!! }
                val bestTimedSet = ex.sets.filter { it.t != null }.maxByOrNull { it.t!! }
                val bestBWSet = ex.sets.filter { it.w == null && it.t == null && it.r != null }.maxByOrNull { it.r!! }
                ExerciseHistoryEntry(
                    date = sess.date,
                    label = sess.label,
                    sets = ex.sets,
                    bestW = bestSet?.w ?: 0.0,
                    bestR = bestSet?.r ?: 0,
                    bestT = bestTimedSet?.t ?: 0L,
                    bestBWR = bestBWSet?.r ?: 0,
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
        val bestT: Long,
        val bestTDate: String,
        val bestBWR: Int,
        val bestBWRDate: String,
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
                    bestT = 0L, bestTDate = "", bestBWR = 0, bestBWRDate = "",
                    totalSets = 0, totalSessions = 0
                )

                var newBestW = current.bestW
                var newBestWR = current.bestWR
                var newBestWDate = current.bestWDate
                var newBestE1rm = current.bestE1rm
                var newBestE1rmW = current.bestE1rmW
                var newBestE1rmR = current.bestE1rmR
                var newBestE1rmDate = current.bestE1rmDate
                var newBestT = current.bestT
                var newBestTDate = current.bestTDate
                var newBestBWR = current.bestBWR
                var newBestBWRDate = current.bestBWRDate
                var sessionSetCount = 0

                for (s in ex.sets) {
                    sessionSetCount++
                    val w = s.w ?: 0.0
                    val r = s.r ?: 0
                    val dur = s.t ?: 0L

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
                    } else if (dur > 0L) {
                        // Timed set (e.g. plank hold, farmer's carry) — PR is the longest duration
                        if (dur > newBestT) {
                            newBestT = dur
                            newBestTDate = sess.date
                        }
                    } else if (w == 0.0 && r > 0 && s.w == null) {
                        // Bodyweight reps, no weight logged — PR is the most reps
                        if (r > newBestBWR) {
                            newBestBWR = r
                            newBestBWRDate = sess.date
                        }
                    }
                }

                map[lower] = current.copy(
                    bestW = newBestW, bestWR = newBestWR, bestWDate = newBestWDate,
                    bestE1rm = newBestE1rm, bestE1rmW = newBestE1rmW, bestE1rmR = newBestE1rmR,
                    bestE1rmDate = newBestE1rmDate,
                    bestT = newBestT, bestTDate = newBestTDate,
                    bestBWR = newBestBWR, bestBWRDate = newBestBWRDate,
                    totalSets = current.totalSets + sessionSetCount,
                    totalSessions = current.totalSessions + 1
                )
            }
        }

        return map.values
            .filter { it.bestW > 0.0 || it.bestT > 0L || it.bestBWR > 0 }
            .sortedByDescending { if (it.bestE1rm > 0.0) it.bestE1rm else if (it.bestT > 0L) it.bestT.toDouble() else it.bestBWR.toDouble() }
    }
}
