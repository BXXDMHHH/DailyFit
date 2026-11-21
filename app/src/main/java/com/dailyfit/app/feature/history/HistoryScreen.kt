package com.dailyfit.app.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.local.HabitRecordEntity
import com.dailyfit.app.data.local.WorkoutHistoryEntity
import com.dailyfit.app.data.local.WorkoutHistoryExerciseEntity
import com.dailyfit.app.data.local.WorkoutHistorySetEntity
import com.dailyfit.app.data.local.WorkoutHistoryStats
import com.dailyfit.app.feature.habits.HabitUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HistoryScreen(
    state: HabitUiState,
    database: DailyFitDatabase,
    modifier: Modifier = Modifier
) {
    val dao = database.workoutSessionDao()
    var rows by remember { mutableStateOf(emptyList<WorkoutHistoryEntity>()) }
    var stats by remember { mutableStateOf(WorkoutHistoryStats(0, 0, 0, 0.0)) }
    var historyLoaded by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<WorkoutHistoryEntity?>(null) }
    var details by remember {
        mutableStateOf(emptyList<Pair<WorkoutHistoryExerciseEntity, List<WorkoutHistorySetEntity>>>())
    }
    var detailsLoading by remember { mutableStateOf(false) }
    var detailsError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(database, reloadKey) {
        historyLoaded = false
        historyError = null
        launch {
            try {
                dao.observeHistory().collect {
                    rows = it
                    historyLoaded = true
                }
            } catch (error: Exception) {
                historyError = error.message ?: "训练历史加载失败"
                historyLoaded = true
            }
        }
        launch {
            try {
                dao.observeStats().collect { stats = it }
            } catch (error: Exception) {
                historyError = error.message ?: "训练统计加载失败"
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("历史与统计", style = MaterialTheme.typography.headlineMedium)
                Text("固定习惯、临时任务和训练分别统计，互不混算。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("固定习惯", style = MaterialTheme.typography.titleMedium)
                    val dashboard = state.dashboard
                    Text("累计完成率：${dashboard?.overallCompletionPercent ?: 0}%（${dashboard?.allCompleted ?: 0}/${dashboard?.allTotal ?: 0}）")
                    Text("连续打卡：${calculateCheckInStreak(state.history, dashboard?.date ?: LocalDate.now())} 天")
                    Text("历史打卡只读展示，不支持补签或修改。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("临时任务（独立统计）", style = MaterialTheme.typography.titleMedium)
                    val tasks = state.temporaryTaskDashboard
                    Text("累计完成：${tasks?.allCompleted ?: 0}/${tasks?.allTotal ?: 0}")
                    Text("今日完成：${tasks?.todayCompleted ?: 0}/${tasks?.todayTotal ?: 0} · 完成率 ${tasks?.todayCompletionPercent ?: 0}%")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("训练统计", style = MaterialTheme.typography.titleMedium)
                    if (!historyLoaded && !state.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text("训练次数：${stats.totalSessions} 次")
                        Text("已完成训练：${stats.completedSessions} 次")
                        Text("完成训练组数：${stats.completedSets} 组")
                        Text("累计训练量：${"%.1f".format(stats.totalVolume)} kg·次")
                    }
                }
            }
        }
        item { Text("习惯打卡记录", style = MaterialTheme.typography.titleLarge) }
        if (state.isLoading) {
            item { CircularProgressIndicator() }
        } else if (state.history.isEmpty()) {
            item { Text("还没有习惯打卡记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.history, key = { it.id }) { record -> HabitHistoryRow(record) }

        item { Text("训练历史", style = MaterialTheme.typography.titleLarge) }
        if (!historyLoaded) {
            item { CircularProgressIndicator() }
        } else if (historyError != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(historyError!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { reloadKey++ }) { Text("重新加载") }
                }
            }
        } else if (rows.isEmpty()) {
            item { Text("还没有正式训练记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(rows, key = { it.id }) { row ->
            Card(Modifier.fillMaxWidth().clickable {
                scope.launch {
                    selected = row
                    detailsLoading = true
                    detailsError = null
                    runCatching {
                        val exercises = dao.observeHistoryExercises(row.id).first()
                        exercises.map { exercise -> exercise to dao.observeHistorySets(exercise.id).first() }
                    }.onSuccess { details = it }
                        .onFailure { detailsError = it.message ?: "训练详情加载失败" }
                    detailsLoading = false
                }
            }) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.titleSnapshot, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${row.businessDate} · ${statusLabel(row.status)} · ${row.durationSeconds / 60} 分钟",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item { Text("正式训练历史永久只读，不能修改或删除。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    selected?.let { row ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(row.titleSnapshot) },
            text = {
                Column(
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${row.businessDate} · ${statusLabel(row.status)}")
                    if (detailsLoading) CircularProgressIndicator()
                    detailsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    details.forEach { (exercise, sets) ->
                        Text(exercise.exerciseNameSnapshot, style = MaterialTheme.typography.titleSmall)
                        sets.forEach { set ->
                            Text(
                                "第${set.setNumber}组：目标 ${set.targetWeight} kg × ${set.targetReps} 次；实际 " +
                                    "${set.actualWeight?.toString() ?: "—"} kg × ${set.actualReps?.toString() ?: "—"} 次" +
                                    if (set.isCompleted) " ✓" else ""
                            )
                        }
                    }
                    Text("正式训练历史永久只读，不能修改或删除。")
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("关闭") } }
        )
    }
}

@Composable
private fun HabitHistoryRow(record: HabitRecordEntity) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.habitNameSnapshot, style = MaterialTheme.typography.titleMedium)
                Text(record.businessDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (record.isCompleted) "已完成" else "未完成",
                color = if (record.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun calculateCheckInStreak(records: List<HabitRecordEntity>, today: LocalDate): Int {
    val completedDates = records.asSequence()
        .filter { it.isCompleted }
        .mapNotNull { runCatching { LocalDate.parse(it.businessDate) }.getOrNull() }
        .toSet()
    var cursor = if (today in completedDates) today else today.minusDays(1)
    var streak = 0
    while (cursor in completedDates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun statusLabel(status: String): String = when (status) {
    "COMPLETED" -> "已完成"
    "INCOMPLETE" -> "未完成"
    "SKIPPED" -> "已跳过"
    else -> status
}
