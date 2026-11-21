package com.dailyfit.app.data.repository

import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.local.TemporaryTaskEntity
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class TemporaryTaskDashboard(
    val date: LocalDate,
    val tasks: List<TemporaryTaskEntity>,
    val todayCompleted: Int,
    val todayTotal: Int,
    val allCompleted: Int,
    val allTotal: Int
) {
    val todayCompletionPercent: Int
        get() = if (todayTotal == 0) 0 else todayCompleted * 100 / todayTotal
    val overallCompletionPercent: Int
        get() = if (allTotal == 0) 0 else allCompleted * 100 / allTotal
}

class TemporaryTaskRepository(
    database: DailyFitDatabase,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val dao = database.temporaryTaskDao()

    fun observeDashboard(date: LocalDate): Flow<TemporaryTaskDashboard> =
        combine(dao.observeForDate(date.toString()), dao.observeAll()) { todayTasks, allTasks ->
            TemporaryTaskDashboard(
                date = date,
                tasks = todayTasks,
                todayCompleted = todayTasks.count { it.isCompleted },
                todayTotal = todayTasks.size,
                allCompleted = allTasks.count { it.isCompleted },
                allTotal = allTasks.size
            )
        }

    suspend fun createTask(title: String) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "任务名称不能为空" }
        val now = clock.millis()
        dao.insert(
            TemporaryTaskEntity(
                id = UUID.randomUUID().toString(),
                businessDate = LocalDate.now(clock).toString(),
                title = cleanTitle,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /** Edits are restricted to the task's creation date. */
    suspend fun updateTask(id: String, title: String): Boolean {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "任务名称不能为空" }
        val now = clock.millis()
        return dao.updateToday(id, LocalDate.now(clock).toString(), cleanTitle, now) == 1
    }

    /** Completion can only be changed on the task's creation date. */
    suspend fun setCompletion(id: String, completed: Boolean): Boolean {
        val now = clock.millis()
        return dao.setTodayCompletion(
            id = id,
            today = LocalDate.now(clock).toString(),
            completed = completed,
            completedAt = if (completed) now else null,
            updatedAt = now
        ) == 1
    }

    /** Deletion is restricted to today's tasks; old records remain available for statistics. */
    suspend fun deleteTask(id: String): Boolean =
        dao.deleteToday(id, LocalDate.now(clock).toString()) == 1
}
