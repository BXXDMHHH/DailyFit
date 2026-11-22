package com.dailyfit.app.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.local.WorkoutHistoryEntity
import com.dailyfit.app.data.local.WorkoutHistoryExerciseEntity
import com.dailyfit.app.data.local.WorkoutHistorySetEntity
import com.dailyfit.app.data.local.WorkoutHistoryStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun WorkoutHistorySection(database: DailyFitDatabase, modifier: Modifier = Modifier) {
    val dao = database.workoutSessionDao()
    var rows by remember { mutableStateOf(emptyList<WorkoutHistoryEntity>()) }
    var stats by remember { mutableStateOf(WorkoutHistoryStats(0, 0, 0, 0.0)) }
    var selected by remember { mutableStateOf<WorkoutHistoryEntity?>(null) }
    var details by remember {
        mutableStateOf(emptyList<Pair<WorkoutHistoryExerciseEntity, List<WorkoutHistorySetEntity>>>())
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        launch { dao.observeHistory().collect { rows = it } }
        launch { dao.observeStats().collect { stats = it } }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("训练历史", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("训练次数：${stats.totalSessions} 次（已完成 ${stats.completedSessions} 次）")
                    Text("完成训练组数：${stats.completedSets} 组")
                    Text("累计训练量：${"%.1f".format(stats.totalVolume)} kg·次")
                }
            }
        }
        if (rows.isEmpty()) {
            item { Text("还没有正式训练记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(rows, key = { it.id }) { row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            selected = row
                            val exercises = dao.observeHistoryExercises(row.id).first()
                            details = exercises.map { exercise ->
                                exercise to dao.observeHistorySets(exercise.id).first()
                            }
                        }
                    }
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(row.titleSnapshot, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${row.businessDate} · ${statusLabel(row.status)} · ${row.durationSeconds / 60} 分钟",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    selected?.let { row ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(row.titleSnapshot) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${row.businessDate} · ${statusLabel(row.status)}")
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

private fun statusLabel(status: String): String = when (status) {
    "COMPLETED" -> "已完成"
    "INCOMPLETE" -> "未完成"
    "SKIPPED" -> "已跳过"
    else -> status
}
