package com.mbosse.gymloga.data

import org.junit.Assert.*
import org.junit.Test

class DataLogicTest {

    // ── parseSets ──────────────────────────────────────────────────────────────

    @Test
    fun parseSets_weightXreps() {
        val sets = DataLogic.parseSets("135x5")
        assertEquals(1, sets.size)
        assertEquals(135.0, sets[0].w)
        assertEquals(5, sets[0].r)
        assertNull(sets[0].note)
    }

    @Test
    fun parseSets_weightXrepsXsets() {
        val sets = DataLogic.parseSets("135x5x3")
        assertEquals(3, sets.size)
        sets.forEach {
            assertEquals(135.0, it.w)
            assertEquals(5, it.r)
        }
    }

    @Test
    fun parseSets_bareWeight_countsAsOneRep() {
        val sets = DataLogic.parseSets("135")
        assertEquals(1, sets.size)
        assertEquals(135.0, sets[0].w)
        assertEquals(1, sets[0].r)
    }

    @Test
    fun parseSets_freeform_noWeightOrReps() {
        val sets = DataLogic.parseSets("30s rest")
        assertEquals(1, sets.size)
        assertNull(sets[0].w)
        assertNull(sets[0].r)
        assertEquals("30s rest", sets[0].note)
    }

    @Test
    fun parseSets_empty_returnsEmpty() {
        assertTrue(DataLogic.parseSets("").isEmpty())
        assertTrue(DataLogic.parseSets("   ").isEmpty())
    }

    // ── getSessionVolume ───────────────────────────────────────────────────────

    @Test
    fun getSessionVolume_sumWeightTimesReps() {
        val session = Session(
            date = "2026-01-01",
            exercises = listOf(
                Exercise(name = "Bench Press", sets = listOf(WorkoutSet(w = 135.0, r = 5))),
                Exercise(name = "Squat", sets = listOf(WorkoutSet(w = 100.0, r = 10)))
            )
        )
        assertEquals(1675L, DataLogic.getSessionVolume(session))
    }

    @Test
    fun getSessionVolume_freeformSetsIgnored() {
        val session = Session(
            date = "2026-01-01",
            exercises = listOf(
                Exercise(name = "Plank", sets = listOf(WorkoutSet(note = "60s")))
            )
        )
        assertEquals(0L, DataLogic.getSessionVolume(session))
    }

    // ── applyRename ────────────────────────────────────────────────────────────

    private fun makeSession(exName: String, defId: String? = null) = Session(
        id = "s1",
        date = "2026-01-01",
        exercises = listOf(Exercise(name = exName, sets = listOf(WorkoutSet(w = 100.0, r = 5)), definitionId = defId))
    )

    @Test
    fun applyRename_matchingDefinitionId_updatesName() {
        val defId = "def-1"
        val sessions = listOf(makeSession("Bench Press", defId))
        val result = DataLogic.applyRename(sessions, defId, "Bench Press", "Barbell Bench Press")
        assertEquals("Barbell Bench Press", result[0].exercises[0].name)
    }

    @Test
    fun applyRename_legacyNullDefinitionId_updatesNameByFallback() {
        val sessions = listOf(makeSession("Bench Press", null))
        val result = DataLogic.applyRename(sessions, "def-1", "Bench Press", "Barbell Bench Press")
        assertEquals("Barbell Bench Press", result[0].exercises[0].name)
    }

    @Test
    fun applyRename_legacyFallback_caseInsensitive() {
        val sessions = listOf(makeSession("bench press", null))
        val result = DataLogic.applyRename(sessions, "def-1", "Bench Press", "Barbell Bench Press")
        assertEquals("Barbell Bench Press", result[0].exercises[0].name)
    }

    @Test
    fun applyRename_differentDefinitionId_notUpdated() {
        val sessions = listOf(makeSession("Bench Press", "def-other"))
        val result = DataLogic.applyRename(sessions, "def-1", "Bench Press", "Barbell Bench Press")
        assertEquals("Bench Press", result[0].exercises[0].name)
    }

    @Test
    fun applyRename_differentName_noDefinitionId_notUpdated() {
        val sessions = listOf(makeSession("Squat", null))
        val result = DataLogic.applyRename(sessions, "def-1", "Bench Press", "Barbell Bench Press")
        assertEquals("Squat", result[0].exercises[0].name)
    }

    // ── getExerciseHistory / getAllPRs after rename ────────────────────────────

    @Test
    fun getExerciseHistory_afterRename_findsUpdatedName() {
        val sessions = listOf(
            Session(
                date = "2026-01-01",
                exercises = listOf(Exercise(name = "Barbell Bench Press", sets = listOf(WorkoutSet(w = 135.0, r = 5))))
            )
        )
        val history = DataLogic.getExerciseHistory(sessions, "Barbell Bench Press")
        assertEquals(1, history.size)
        assertEquals(135.0, history[0].bestW, 0.001)
    }

    @Test
    fun getAllPRs_aggregatesAcrossRenamedSessions() {
        val sessions = listOf(
            Session(date = "2026-01-01", exercises = listOf(Exercise(name = "Barbell Bench Press", sets = listOf(WorkoutSet(w = 135.0, r = 5))))),
            Session(date = "2026-01-08", exercises = listOf(Exercise(name = "Barbell Bench Press", sets = listOf(WorkoutSet(w = 140.0, r = 5)))))
        )
        val prs = DataLogic.getAllPRs(sessions)
        assertEquals(1, prs.size)
        assertEquals(140.0, prs[0].bestW, 0.001)
        assertEquals(2, prs[0].totalSessions)
    }
}
