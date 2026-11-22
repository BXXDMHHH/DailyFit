package com.dailyfit.app.domain.model

/**
 * Shared validation for target repetitions and weights entered in plans or workouts.
 * A target may be a single count ("8") or an inclusive range ("8-12").
 */
object WorkoutInputRules {
    private val repetitionPattern = Regex("^(\\d{1,3})(?:\\s*-\\s*(\\d{1,3}))?$")

    fun parseTargetReps(value: String): Int {
        val match = repetitionPattern.matchEntire(value.trim())
            ?: throw IllegalArgumentException("目标次数请填写数字或数字范围，例如 8 或 8-12")
        val minimum = match.groupValues[1].toInt()
        val maximum = match.groupValues[2].ifBlank { match.groupValues[1] }.toInt()
        require(minimum in 1..999 && maximum in minimum..999) {
            "目标次数范围必须在 1 到 999 之间，且上限不能小于下限"
        }
        return minimum
    }

    fun isValidWeight(value: Double): Boolean = value.isFinite() && value >= 0.0
}
