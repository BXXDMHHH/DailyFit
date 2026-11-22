package com.dailyfit.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutInputRulesTest {
    @Test
    fun acceptsSingleTargetAndRange() {
        assertEquals(8, WorkoutInputRules.parseTargetReps("8"))
        assertEquals(8, WorkoutInputRules.parseTargetReps("8-12"))
        assertEquals(8, WorkoutInputRules.parseTargetReps(" 8 - 12 "))
    }

    @Test
    fun rejectsInvalidOrReversedTargetRanges() {
        listOf("", "0", "1000", "12-8", "8--12", "8次", "1-1000").forEach { value ->
            assertTrue("Expected invalid target: $value", runCatching {
                WorkoutInputRules.parseTargetReps(value)
            }.isFailure)
        }
    }

    @Test
    fun weightMustBeFiniteAndNonNegative() {
        assertTrue(WorkoutInputRules.isValidWeight(0.0))
        assertTrue(WorkoutInputRules.isValidWeight(120.5))
        assertFalse(WorkoutInputRules.isValidWeight(-0.1))
        assertFalse(WorkoutInputRules.isValidWeight(Double.NaN))
        assertFalse(WorkoutInputRules.isValidWeight(Double.POSITIVE_INFINITY))
    }
}
