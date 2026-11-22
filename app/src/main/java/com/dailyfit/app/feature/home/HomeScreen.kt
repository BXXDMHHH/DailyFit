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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity
import com.dailyfit.app.data.local.HabitEntity
import com.dailyfit.app.data.local.TemporaryTaskEntity
import com.dailyfit.app.data.repository.HabitItem
import com.dailyfit.app.feature.habits.HabitUiState
import com.dailyfit.app.feature.habits.HabitViewModel
import java.time.DayOfWeek

@Composable
fun HomeScreen(
    state: HabitUiState,
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onStartWorkout: () -> Unit = {},
    onOpenTrainingPlan: () -> Unit = {},
    todayWorkoutPlans: List<DailyWorkoutPlanEntity> = emptyList(),
    isWorkoutPlansLoading: Boolean = false
) {
    val dashboard = state.dashboard
    val taskDashboard = state.temporaryTaskDashboard
    val pendingHabits = dashboard?.todayHabits?.filterNot { it.record.isCompleted }.orEmpty()
    val completedHabits = dashboard?.todayHabits?.filter { it.record.isCompleted }.orEmpty()
    var showCompletedHabits by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TemporaryTaskEntity?>(null) }
    var deletingTask by remember { mutableStateOf<TemporaryTaskEntity?>(null) }
    var showTemporaryTasks by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(top = 22.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("早上好，今天也向前一点", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("DailyFit", style = MaterialTheme.typography.headlineLarge)
                Text("把日常和训练，稳稳记录下来", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 固定习惯是首页的核心内容，放在首位并给予最大的视觉权重。
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("每日习惯", style = MaterialTheme.typography.titleLarge)
                            Text("每天坚持一点点，慢慢成为想成为的人", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (dashboard != null && !state.isLoading) {
                            Text(
                                "${dashboard.todayCompleted}/${dashboard.todayTotal}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (state.isLoading || dashboard == null) {
                        CircularProgressIndicator()
                    } else {
                        Text("${dashboard.date}  ·  今日完成率 ${dashboard.todayCompletionPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { dashboard.todayCompletionPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        if (dashboard.todayHabits.isEmpty()) {
                            Text("今天没有安排固定习惯，去计划页添加吧。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("待完成 · ${pendingHabits.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            if (pendingHabits.isEmpty()) {
                                Text("今天的习惯都完成啦。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                pendingHabits.forEach { item ->
                                    HabitRow(item = item, onChecked = { viewModel.setCompleted(item.record.id, it) })
                                }
                            }
                            if (completedHabits.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("已完成 · ${completedHabits.size}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { showCompletedHabits = !showCompletedHabits }) {
                                        Text(if (showCompletedHabits) "收起" else "查看")
                                    }
                                }
                                if (showCompletedHabits) {
                                    completedHabits.forEach { item ->
                                        HabitRow(item = item, onChecked = { viewModel.setCompleted(item.record.id, it) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 训练入口保留在首页，但层级低于固定习惯、高于临时任务。
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("今日训练", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text("TRAINING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    if (isWorkoutPlansLoading) {
                        CircularProgressIndicator()
                    } else if (todayWorkoutPlans.isEmpty()) {
                        Text("先设置每周训练动作，再一键导入到今天。", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    } else {
                        todayWorkoutPlans.forEach { plan ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(plan.title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(
                                    when (plan.status) {
                                        "COMPLETED" -> "已完成"
                                        "SKIPPED" -> "已跳过"
                                        else -> "未完成"
                                    },
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onStartWorkout) { Text("开始今日训练") }
                        TextButton(onClick = onOpenTrainingPlan) { Text("设置动作 / 导入") }
                    }
                }
            }
        }

        // 临时任务降为次级工具区，默认收起，不与每日习惯争夺首屏注意力。
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("临时任务", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (taskDashboard == null) "只记录今天额外要做的事" else "今日完成 ${taskDashboard.todayCompleted}/${taskDashboard.todayTotal} · 不计入习惯完成率",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showCreateTaskDialog = true }) { Text("＋ 添加") }
                        TextButton(onClick = { showTemporaryTasks = !showTemporaryTasks }) {
                            Text(if (showTemporaryTasks) "收起" else "展开")
                        }
                    }
                    if (showTemporaryTasks) {
                        if (state.isLoading || taskDashboard == null) {
                            CircularProgressIndicator()
                        } else if (taskDashboard.tasks.isEmpty()) {
                            Text("今天还没有临时任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            taskDashboard.tasks.forEach { task ->
                                TemporaryTaskRow(
                                    task = task,
                                    onChecked = { viewModel.setTemporaryTaskCompleted(task.id, it) },
                                    onEdit = { editingTask = task },
                                    onDelete = { deletingTask = task }
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showCreateTaskDialog) {
        TemporaryTaskEditorDialog(
            title = "新增临时任务",
            initialTitle = "",
            onDismiss = { showCreateTaskDialog = false },
            onSave = { title ->
                viewModel.addTemporaryTask(title)
                showCreateTaskDialog = false
            }
        )
    }
    editingTask?.let { task ->
        TemporaryTaskEditorDialog(
            title = "编辑临时任务",
            initialTitle = task.title,
            onDismiss = { editingTask = null },
            onSave = { title ->
                viewModel.editTemporaryTask(task.id, title)
                editingTask = null
            }
        )
    }
    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            title = { Text("删除临时任务？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemporaryTask(task.id)
                    deletingTask = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingTask = null }) { Text("取消") } }
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
private fun TemporaryTaskRow(
    task: TemporaryTaskEntity,
    onChecked: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = task.isCompleted, onCheckedChange = onChecked)
            Text(
                task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onEdit) { Text("编辑") }
            TextButton(onClick = onDelete) { Text("删除") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun TemporaryTaskEditorDialog(
    title: String,
    initialTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(title, initialTitle) { mutableStateOf(initialTitle) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("任务内容") },
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (value.isBlank()) error = "请输入任务内容" else onSave(value.trim())
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun HabitRow(
    item: HabitItem,
    onChecked: (Boolean) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = item.record.isCompleted, onCheckedChange = onChecked)
            Column(Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.bodyLarge)
                Text(frequencyLabel(item.habit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
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
