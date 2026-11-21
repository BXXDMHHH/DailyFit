package com.dailyfit.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyfit.app.data.local.HabitEntity
import com.dailyfit.app.data.repository.HabitItem
import com.dailyfit.app.feature.habits.HabitUiState
import com.dailyfit.app.feature.habits.HabitViewModel
import com.dailyfit.app.domain.model.HabitFrequency
import java.time.DayOfWeek

@Composable
fun HomeScreen(
    state: HabitUiState,
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val dashboard = state.dashboard
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deactivatingHabit by remember { mutableStateOf<HabitEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(top = 22.dp)) {
                Text("DailyFit", style = MaterialTheme.typography.headlineMedium)
                Text("把日常和训练，稳稳记录下来", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("今日习惯", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { showCreateDialog = true }) { Text("新增习惯") }
                    }
                    if (state.isLoading || dashboard == null) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            "${dashboard.date}  ·  已完成 ${dashboard.todayCompleted}/${dashboard.todayTotal}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("今日完成率 ${dashboard.todayCompletionPercent}%", style = MaterialTheme.typography.titleMedium)
                        if (dashboard.todayHabits.isEmpty()) {
                            Text("今天没有需要完成的固定习惯", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        dashboard.todayHabits.forEach { item ->
                            HabitRow(
                                item = item,
                                onChecked = { viewModel.setCompleted(item.record.id, it) },
                                onEdit = { editingHabit = item.habit },
                                onDeactivate = { deactivatingHabit = item.habit }
                            )
                        }
                    }
                }
            }
        }
        if (dashboard != null && dashboard.inactiveHabits.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("已停用习惯", style = MaterialTheme.typography.titleMedium)
                        dashboard.inactiveHabits.forEach { Text(it.name, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("今日任务", style = MaterialTheme.typography.titleMedium)
                    Text("临时任务将在下一阶段接入", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("今日训练", style = MaterialTheme.typography.titleMedium)
                    Text("训练计划与训练记录将在后续阶段接入", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showCreateDialog) {
        HabitEditorDialog(
            title = "新增习惯",
            initialName = "",
            initialFrequency = HabitFrequency.DAILY,
            initialDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            onDismiss = { showCreateDialog = false },
            onSave = { name, frequency, days ->
                viewModel.addHabit(name, frequency, days)
                showCreateDialog = false
            }
        )
    }
    editingHabit?.let { habit ->
        HabitEditorDialog(
            title = "编辑习惯",
            initialName = habit.name,
            initialFrequency = runCatching { HabitFrequency.valueOf(habit.frequency) }.getOrDefault(HabitFrequency.DAILY),
            initialDays = habit.customDays.split(",").mapNotNull { it.toIntOrNull()?.let(DayOfWeek::of) }.toSet(),
            onDismiss = { editingHabit = null },
            onSave = { name, frequency, days ->
                viewModel.editHabit(habit.id, name, frequency, days)
                editingHabit = null
            }
        )
    }
    deactivatingHabit?.let { habit ->
        AlertDialog(
            onDismissRequest = { deactivatingHabit = null },
            title = { Text("停用习惯？") },
            text = { Text("停用后不会再生成新的每日任务，已有打卡历史会永久保留。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deactivateHabit(habit.id)
                    deactivatingHabit = null
                }) { Text("确认停用") }
            },
            dismissButton = { TextButton(onClick = { deactivatingHabit = null }) { Text("取消") } }
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
private fun HabitRow(
    item: HabitItem,
    onChecked: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = item.record.isCompleted, onCheckedChange = onChecked)
            Column(Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.bodyLarge)
                Text(frequencyLabel(item.habit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onEdit) { Text("编辑") }
            TextButton(onClick = onDeactivate) { Text("停用") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
    var validationError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("习惯名称") }, singleLine = true)
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
                    DayOfWeek.values().forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = day in days, onCheckedChange = { checked ->
                                days = if (checked) days + day else days - day
                            })
                            Text(dayLabel(day))
                        }
                    }
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> validationError = "请输入习惯名称"
                    frequency == HabitFrequency.CUSTOM && days.isEmpty() -> validationError = "至少选择一个星期"
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
    else -> habit.customDays.split(",").mapNotNull { it.toIntOrNull()?.let { value -> dayLabel(DayOfWeek.of(value)) } }.joinToString("、")
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
