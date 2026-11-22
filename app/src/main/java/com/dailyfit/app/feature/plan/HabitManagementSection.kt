package com.dailyfit.app.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyfit.app.data.local.HabitEntity
import com.dailyfit.app.domain.model.HabitFrequency
import com.dailyfit.app.feature.habits.HabitUiState
import com.dailyfit.app.feature.habits.HabitViewModel
import java.time.DayOfWeek

@Composable
fun HabitManagementSection(state: HabitUiState, viewModel: HabitViewModel) {
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HabitEntity?>(null) }
    var deactivating by remember { mutableStateOf<HabitEntity?>(null) }
    val dashboard = state.dashboard

    Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("固定习惯管理", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Button(onClick = { showCreate = true }) { Text("新增习惯") }
            }
            if (state.isLoading || dashboard == null) {
                Text("正在加载习惯…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                if (dashboard.activeHabits.isEmpty()) {
                    Text("还没有固定习惯，添加后会按频率生成每日打卡。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                dashboard.activeHabits.forEach { habit ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.bodyLarge)
                            Text(frequencyLabel(habit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { editing = habit }) { Text("编辑") }
                        TextButton(onClick = { deactivating = habit }) { Text("停用") }
                    }
                }
                if (dashboard.inactiveHabits.isNotEmpty()) {
                    Text("已停用", style = MaterialTheme.typography.titleSmall)
                    dashboard.inactiveHabits.forEach { Text(it.name, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    if (showCreate) {
        HabitEditorDialog(
            title = "新增固定习惯",
            initialName = "",
            initialFrequency = HabitFrequency.DAILY,
            initialDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            onDismiss = { showCreate = false },
            onSave = { name, frequency, days ->
                viewModel.addHabit(name, frequency, days)
                showCreate = false
            }
        )
    }
    editing?.let { habit ->
        HabitEditorDialog(
            title = "编辑固定习惯",
            initialName = habit.name,
            initialFrequency = runCatching { HabitFrequency.valueOf(habit.frequency) }.getOrDefault(HabitFrequency.DAILY),
            initialDays = habit.customDays.split(",").mapNotNull { it.toIntOrNull()?.let { day -> runCatching { DayOfWeek.of(day) }.getOrNull() } }.toSet(),
            onDismiss = { editing = null },
            onSave = { name, frequency, days ->
                viewModel.editHabit(habit.id, name, frequency, days)
                editing = null
            }
        )
    }
    deactivating?.let { habit ->
        AlertDialog(
            onDismissRequest = { deactivating = null },
            title = { Text("停用习惯？") },
            text = { Text("停用后不会再生成新的每日任务，已有打卡历史会永久保留。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deactivateHabit(habit.id); deactivating = null }) { Text("确认停用") }
            },
            dismissButton = { TextButton(onClick = { deactivating = null }) { Text("取消") } }
        )
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::consumeMessage,
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::consumeMessage) { Text("知道了") } }
        )
    }
}

@Composable
private fun HabitEditorDialog(
    title: String,
    initialName: String,
    initialFrequency: HabitFrequency,
    initialDays: Set<DayOfWeek>,
    onDismiss: () -> Unit,
    onSave: (String, HabitFrequency, Set<DayOfWeek>) -> Unit
) {
    var name by remember(title, initialName) { mutableStateOf(initialName) }
    var frequency by remember(title, initialFrequency) { mutableStateOf(initialFrequency) }
    var days by remember(title, initialDays) { mutableStateOf(initialDays) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("习惯名称") }, singleLine = true)
                Text("执行频率", style = MaterialTheme.typography.titleSmall)
                listOf(
                    HabitFrequency.DAILY to "每天",
                    HabitFrequency.WEEKDAYS to "工作日（周一至周五）",
                    HabitFrequency.CUSTOM to "自定义星期"
                ).forEach { (value, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = frequency == value, onClick = { frequency = value })
                        Text(label)
                    }
                }
                if (frequency == HabitFrequency.CUSTOM) {
                    DayOfWeek.entries.forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = day in days, onCheckedChange = { checked ->
                                days = if (checked) days + day else days - day
                            })
                            Text(dayLabel(day))
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> error = "请输入习惯名称"
                    frequency == HabitFrequency.CUSTOM && days.isEmpty() -> error = "至少选择一个星期"
                    else -> onSave(name.trim(), frequency, days)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun frequencyLabel(habit: HabitEntity): String = when (habit.frequency) {
    "DAILY" -> "每天"
    "WEEKDAYS" -> "工作日"
    else -> habit.customDays.split(",").mapNotNull { it.toIntOrNull()?.let { value -> runCatching { DayOfWeek.of(value) }.getOrNull()?.let(::dayLabel) } }.joinToString("、")
}

private fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}
