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

    // ── suggestSets ────────────────────────────────────────────────────────────

    @Test
    fun suggestSets_barbell_lbs() {
        val pr = DataLogic.PRRecord(
            name = "Bench Press",
            bestW = 225.0, bestWR = 5, bestWDate = "2026-01-01",
            bestE1rm = 262.5, // 225 * (1 + 5/30)
            bestE1rmW = 225.0, bestE1rmR = 5, bestE1rmDate = "2026-01-01",
            totalSets = 10, totalSessions = 2
        )
        // targetReps = 5, targetWeight = 262.5 / (1 + 5/30) = 225.0
        val hint = DataLogic.suggestSets(pr, EquipmentType.BARBELL, 5, WeightUnit.LBS, emptyList())
        assertNotNull(hint)
        assertEquals(225.0, hint!!.workingWeight, 0.001)
        // Warmups according to logic for working > t3 (185lbs): 45x5, 50%x5, 70%x3, 85%x1
        assertEquals(4, hint.warmupSets.size)
        assertEquals(45.0, hint.warmupSets[0].first, 0.001)
        assertEquals(190.0, hint.warmupSets[3].first, 0.001) // 85% of 225 = 191.25 -> 190.0
    }

    @Test
    fun suggestSets_dumbbell_kg() {
        val pr = DataLogic.PRRecord(
            name = "DB Press",
            bestW = 30.0, bestWR = 10, bestWDate = "2026-01-01",
            bestE1rm = 40.0, // 30 * (1 + 10/30)
            bestE1rmW = 30.0, bestE1rmR = 10, bestE1rmDate = "2026-01-01",
            totalSets = 10, totalSessions = 2
        )
        // targetReps = 10, targetWeight = 40.0 / (1 + 10/30) = 30.0
        val hint = DataLogic.suggestSets(pr, EquipmentType.DUMBBELL, 10, WeightUnit.KG, emptyList())
        assertNotNull(hint)
        assertEquals(30.0, hint!!.workingWeight, 0.001)
        // DB warmup: 50% of 30.0 = 15.0 -> rounded to 2.0 increment = 16.0
        assertEquals(1, hint.warmupSets.size)
        assertEquals(16.0, hint.warmupSets[0].first, 0.001)
    }

    @Test
    fun suggestSets_bodyweight_returnsNull() {
        val pr = DataLogic.PRRecord(
            name = "Pushup",
            bestW = 0.0, bestWR = 50, bestWDate = "2026-01-01",
            bestE1rm = 0.0, bestE1rmW = 0.0, bestE1rmR = 50, bestE1rmDate = "2026-01-01",
            totalSets = 10, totalSessions = 2
        )
        val hint = DataLogic.suggestSets(pr, EquipmentType.BODYWEIGHT, 10, WeightUnit.LBS, emptyList())
        assertNull(hint)
    }

    @Test
    fun roundToIncrement_tests() {
        assertEquals(100.0, DataLogic.roundToIncrement(101.0, 2.5), 0.001)
        assertEquals(102.5, DataLogic.roundToIncrement(101.3, 2.5), 0.001)
        assertEquals(105.0, DataLogic.roundToIncrement(104.0, 5.0), 0.001)
        assertEquals(100.0, DataLogic.roundToIncrement(102.4, 5.0), 0.001)
    }
}
