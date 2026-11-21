package com.dailyfit.app.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyfit.app.data.local.HabitRecordEntity
import com.dailyfit.app.feature.habits.HabitUiState
import java.time.LocalDate

@Composable
fun HistoryScreen(state: HabitUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("历史", style = MaterialTheme.typography.headlineMedium)
                val dashboard = state.dashboard
                Text(
                    "固定习惯累计完成率：${dashboard?.overallCompletionPercent ?: 0}%（${dashboard?.allCompleted ?: 0}/${dashboard?.allTotal ?: 0}）",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text("连续有打卡的天数：${currentCheckInStreak(state.history)} 天", style = MaterialTheme.typography.titleMedium)
                Text("历史打卡只读展示，不能补签或修改。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.history.isEmpty()) {
            item { Text("还没有历史打卡记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.history, key = { it.id }) { record -> HabitHistoryRow(record) }
    }
}

@Composable
private fun HabitHistoryRow(record: HabitRecordEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.habitNameSnapshot, style = MaterialTheme.typography.titleMedium)
            Text(record.businessDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (record.isCompleted) "已完成" else "未完成", color = if (record.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


private fun currentCheckInStreak(records: List<HabitRecordEntity>): Int {
    val completedDates = records.asSequence()
        .filter { it.isCompleted }
        .mapNotNull { runCatching { LocalDate.parse(it.businessDate) }.getOrNull() }
        .toSet()
    val today = LocalDate.now()
    var cursor = if (today in completedDates) today else today.minusDays(1)
    var streak = 0
    while (cursor in completedDates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
