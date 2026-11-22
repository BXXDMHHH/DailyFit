package com.dailyfit.app.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    plans: List<DailyWorkoutPlanEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLibrary by remember { mutableStateOf(false) }
    var temporaryName by remember { mutableStateOf("") }
    var showTemporaryDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var incrementExerciseId by remember { mutableStateOf<String?>(null) }
    var incrementInput by remember { mutableStateOf("2.5") }
    var oneRepMaxExerciseId by remember { mutableStateOf<String?>(null) }
    var actualOneRepMaxInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadLibrary() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("训练执行", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("返回") }
            }
        }

        if (state.session == null) {
            item { Text("选择今天的训练安排开始记录") }
            if (plans.isEmpty()) {
                item { Text("今天暂无训练安排，请先到计划页设置训练。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(plans, key = { it.id }) { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(plan.title, style = MaterialTheme.typography.titleMedium)
                        Text(plan.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.start(plan) }, enabled = plan.status == "PLANNED") {
                            Text(when (plan.status) {
                                "COMPLETED" -> "已完成"
                                "SKIPPED" -> "已跳过"
                                else -> "开始训练"
                            })
                        }
                    }
                }
            }
        } else {
            val session = state.session!!
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.title, style = MaterialTheme.typography.titleLarge)
                    Text("训练过程自动保存，重新打开应用会恢复未完成训练。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.exercises.isEmpty()) {
                item { Text("计划没有配置动作，可以从动作库添加，或添加临时动作。") }
            }
            items(state.exercises, key = { it.row.id }) { exercise ->
                val initialWeight = exercise.sets.lastOrNull { !it.isCompleted && it.actualWeight != null }?.actualWeight
                    ?: exercise.sets.firstOrNull { it.actualWeight != null }?.actualWeight
                    ?: exercise.progress.lastWeight
                    ?: exercise.row.targetWeight
                var workingWeight by remember(exercise.row.id, exercise.progress.lastWeight) {
                    mutableStateOf(initialWeight.coerceAtLeast(0.0))
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(exercise.row.exerciseNameSnapshot, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "目标：${exercise.row.targetSets} 组 · ${exercise.row.targetReps} 次 · ${exercise.row.targetWeight} kg",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (exercise.progress.lastWeight != null) {
                            Text("上次完成重量：${exercise.progress.lastWeight} kg", color = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("本次重量", modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = {
                                workingWeight = (workingWeight - exercise.progress.incrementKg).coerceAtLeast(0.0)
                                viewModel.setWorkingWeight(exercise.row.id, workingWeight)
                            }) { Text("−") }
                            Text("${"%.1f".format(workingWeight)} kg", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = {
                                workingWeight += exercise.progress.incrementKg
                                viewModel.setWorkingWeight(exercise.row.id, workingWeight)
                            }) { Text("+") }
                        }
                        TextButton(onClick = {
                            incrementExerciseId = exercise.row.id
                            incrementInput = exercise.progress.incrementKg.toString()
                        }) { Text("调整重量步进（当前 ${exercise.progress.incrementKg} kg）") }

                        exercise.sets.forEach { set ->
                            var reps by remember(set.id, set.actualReps) {
                                mutableStateOf((set.actualReps ?: set.targetReps).toString())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("第${set.setNumber}组", modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = reps,
                                    onValueChange = {
                                        reps = it
                                        if (it.isBlank() || it.toIntOrNull() != null) {
                                            viewModel.updateSet(set, workingWeight.toString(), it, set.isCompleted)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("次数") },
                                    singleLine = true
                                )
                                Checkbox(
                                    checked = set.isCompleted,
                                    onCheckedChange = {
                                        viewModel.updateSet(
                                            set,
                                            workingWeight.toString(),
                                            reps.ifBlank { set.targetReps.toString() },
                                            it
                                        )
                                    }
                                )
                                Text(if (set.isCompleted) "完成" else "完成本组")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.addSet(exercise.row.id) }) { Text("＋ 添加一组") }
                            TextButton(onClick = { viewModel.removeExercise(exercise.row.id) }) { Text("删除动作") }
                        }
                        Text("最佳记录：实际 1RM ${exercise.progress.bestActualOneRepMax?.let { "$it kg" } ?: "—"} · 估算 1RM ${exercise.progress.bestEstimatedOneRepMax?.let { "$it kg" } ?: "—"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            onClick = {
                                oneRepMaxExerciseId = exercise.row.id
                                actualOneRepMaxInput = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("记录本次最大重量 / 1RM") }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showLibrary = true }) { Text("添加动作") }
                    OutlinedButton(onClick = { showTemporaryDialog = true }) { Text("临时动作") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.finish("INCOMPLETE") }) { Text("未完成并保存") }
                        OutlinedButton(onClick = { viewModel.finish("SKIPPED") }) { Text("跳过并保存") }
                        Button(onClick = { viewModel.finish("COMPLETED") }) { Text("完成训练") }
                    }
                    TextButton(onClick = { showDiscardConfirmation = true }) { Text("放弃本次训练（不保存历史）") }
                }
            }
        }
    }

    if (incrementExerciseId != null) {
        AlertDialog(
            onDismissRequest = { incrementExerciseId = null },
            title = { Text("设置重量步进") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("每次点击 + / − 调整的重量，例如杠铃卧推 2.5 kg、侧平举 1 kg。")
                    OutlinedTextField(
                        value = incrementInput,
                        onValueChange = { incrementInput = it },
                        label = { Text("每次增加 / 减少（kg）") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    incrementExerciseId?.let { id -> incrementInput.toDoubleOrNull()?.let { viewModel.setWeightIncrement(id, it) } }
                    incrementExerciseId = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { incrementExerciseId = null }) { Text("取消") } }
        )
    }

    if (oneRepMaxExerciseId != null) {
        AlertDialog(
            onDismissRequest = { oneRepMaxExerciseId = null },
            title = { Text("记录最大重量 / 1RM") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("填写实际完成过的单次最大重量；留空则只记录根据本次已完成组估算的 1RM。两种数据分开保存。")
                    OutlinedTextField(
                        value = actualOneRepMaxInput,
                        onValueChange = { actualOneRepMaxInput = it },
                        label = { Text("实际 1RM（kg，可留空）") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    oneRepMaxExerciseId?.let { viewModel.recordOneRepMax(it, actualOneRepMaxInput) }
                    oneRepMaxExerciseId = null
                }) { Text("记录") }
            },
            dismissButton = { TextButton(onClick = { oneRepMaxExerciseId = null }) { Text("取消") } }
        )
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("放弃本次训练？") },
            text = { Text("将删除这次尚未完成的训练草稿，不会生成正式训练历史。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discard()
                    showDiscardConfirmation = false
                }) { Text("确认放弃") }
            },
            dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }) { Text("继续训练") } }
        )
    }

    if (showLibrary) {
        AlertDialog(
            onDismissRequest = { showLibrary = false },
            title = { Text("从动作库添加") },
            text = {
                LazyColumn {
                    items(state.library, key = { it.id }) { exercise ->
                        TextButton(onClick = {
                            viewModel.addLibraryExercise(exercise)
                            showLibrary = false
                        }) {
                            Column {
                                Text(exercise.name)
                                Text(exercise.category, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLibrary = false }) { Text("关闭") } }
        )
    }

    if (showTemporaryDialog) {
        AlertDialog(
            onDismissRequest = { showTemporaryDialog = false },
            title = { Text("临时训练动作") },
            text = {
                OutlinedTextField(
                    value = temporaryName,
                    onValueChange = { temporaryName = it },
                    label = { Text("动作名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addTemporary(temporaryName)
                        temporaryName = ""
                        showTemporaryDialog = false
                    },
                    enabled = temporaryName.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showTemporaryDialog = false }) { Text("取消") } }
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text("知道了") } }
        )
    }
}
