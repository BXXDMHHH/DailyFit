package com.dailyfit.app.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity
import com.dailyfit.app.data.local.ExerciseEntity
import com.dailyfit.app.data.local.WorkoutTemplateEntity
import com.dailyfit.app.data.repository.WeeklyPlanInput
import com.dailyfit.app.data.repository.WorkoutPlanStatus
import com.dailyfit.app.feature.habits.HabitViewModel
import com.dailyfit.app.feature.habits.HabitUiState
import androidx.compose.material3.CircularProgressIndicator
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun PlanScreen(viewModel: PlanViewModel, habitViewModel: HabitViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habitState by habitViewModel.uiState.collectAsStateWithLifecycle()
    var showWeeklyEditor by remember { mutableStateOf(false) }
    var showPrimaryEditor by remember { mutableStateOf(false) }
    var showExtraEditor by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showExerciseEditor by remember { mutableStateOf(false) }
    var deletingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var editingExtra by remember { mutableStateOf<DailyWorkoutPlanEntity?>(null) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }
    var deletingTemplate by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }
    var pendingTemplateImport by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf(state.selectedDate.toString()) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(top = 20.dp)) {
                Text("训练计划", style = MaterialTheme.typography.headlineMedium)
                Text("每周模板、单日安排与动作库彼此独立", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { HabitManagementSection(state = habitState, viewModel = habitViewModel) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("完整训练计划库", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("每个计划都包含整套动作和目标组数、次数。选择后一次性导入当天。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Button(onClick = { editingTemplate = null; showTemplateEditor = true }) { Text("新建") }
                    }
                    if (state.workoutTemplates.isEmpty()) {
                        Text("暂无训练计划，点击新建创建一套。", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    state.workoutTemplates.forEach { template ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                                        Text(template.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(onClick = { editingTemplate = template; showTemplateEditor = true }) { Text("信息") }
                                    TextButton(onClick = { viewModel.openActionEditor("WORKOUT_TEMPLATE", template.id) }) { Text("编辑动作") }
                                }
                                if (template.description.isNotBlank()) {
                                    Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val templateActions = state.workoutTemplateExercises.filter { it.ownerId == template.id }.sortedBy { it.sortOrder }
                                templateActions.forEach { action ->
                                    Text(
                                        "${action.sortOrder + 1}. ${action.exerciseNameSnapshot}  ·  ${action.targetSets} 组 × ${action.targetReps} 次",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { pendingTemplateImport = template },
                                        enabled = !state.isLoading && state.dailyPlans.any { it.isPrimary && it.status == "PLANNED" }
                                    ) { Text("导入${state.selectedDate}") }
                                    TextButton(onClick = { deletingTemplate = template }) { Text("删除") }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.isLoading) {
            item {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("每周固定计划", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { showWeeklyEditor = true }) { Text("编辑模板") }
                    }
                    Text("修改将生成新版本，只对生效日期及之后的日期生效。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                    if (!state.isLoading && state.weeklyDays.isEmpty()) {
                        Text("每周训练模板暂不可用，请检查数据后重试。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.weeklyDays.forEach { day ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(names.getOrElse(day.dayOfWeek - 1) { "" }, modifier = Modifier.weight(1f))
                            Text(if (day.isRestDay) "休息" else day.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("单日训练安排", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { value ->
                            dateText = value
                            runCatching { LocalDate.parse(value) }.getOrNull()?.let(viewModel::selectDate)
                        },
                        label = { Text("日期（YYYY-MM-DD）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("只修改所选日期，不会改变每周固定模板。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (!state.isLoading && state.dailyPlans.isEmpty()) {
                        Text("所选日期暂无训练安排。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.dailyPlans.forEach { plan ->
                        DailyPlanRow(
                            plan = plan,
                            onEdit = { if (plan.isPrimary) showPrimaryEditor = true else editingExtra = plan },
                            onStatus = { viewModel.setStatus(plan.id, it) },
                            onDelete = { viewModel.deleteExtra(plan.id) },
                            onActions = { viewModel.openActionEditor("DAILY_PLAN", plan.id) }
                        )
                    }
                    OutlinedButton(onClick = { showExtraEditor = true }) { Text("添加额外训练") }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("独立动作库", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editingExercise = null; showExerciseEditor = true }) { Text("新增动作") }
                    }
                    OutlinedTextField(
                        value = state.exerciseQuery,
                        onValueChange = viewModel::setExerciseQuery,
                        label = { Text("搜索动作名称或类别") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (state.exercises.isEmpty()) Text(if (state.exerciseQuery.isBlank()) "动作库还没有动作，请添加常用训练动作。" else "没有找到匹配的动作。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.exercises.forEach { exercise ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                Text(listOf(exercise.category, exercise.equipment).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { editingExercise = exercise; showExerciseEditor = true }) { Text("编辑") }
                            TextButton(onClick = { deletingExercise = exercise }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    if (showTemplateEditor) {
        WorkoutTemplateEditor(
            initial = editingTemplate,
            onDismiss = { showTemplateEditor = false },
            onSave = { id, name, category, description ->
                viewModel.saveWorkoutTemplate(id, name, category, description)
                showTemplateEditor = false
            }
        )
    }
    deletingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { deletingTemplate = null },
            title = { Text("删除训练计划？") },
            text = { Text("只删除计划库中的模板，不会影响已经导入到某一天的训练副本，也不会影响训练历史。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteWorkoutTemplate(template.id); deletingTemplate = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingTemplate = null }) { Text("取消") } }
        )
    }
    pendingTemplateImport?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingTemplateImport = null },
            title = { Text("导入完整训练计划？") },
            text = {
                Text("将「${template.name}」的全部动作、目标组数和次数复制到 ${state.selectedDate} 的主要训练，并替换当天当前计划。模板本身和已保存的训练历史不会改变。")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importWorkoutTemplateForSelectedDate(template.id)
                    pendingTemplateImport = null
                }) { Text("确认导入") }
            },
            dismissButton = { TextButton(onClick = { pendingTemplateImport = null }) { Text("取消") } }
        )
    }
    if (showWeeklyEditor) {
        WeeklyPlanEditor(
            initial = state.weeklyDays,
            onDismiss = { showWeeklyEditor = false },
            onSave = { effectiveDate, days ->
                viewModel.saveWeeklyPlan(effectiveDate, days)
                showWeeklyEditor = false
            }
        )
    }
    if (showPrimaryEditor) {
        val primary = state.dailyPlans.firstOrNull { it.isPrimary }
        PlanTextEditor(
            title = "调整当天主要训练",
            initialTitle = primary?.title.orEmpty(),
            initialNotes = primary?.notes.orEmpty(),
            onDismiss = { showPrimaryEditor = false },
            onSave = { title, notes -> viewModel.updatePrimary(title, notes); showPrimaryEditor = false }
        )
    }
    if (showExtraEditor) {
        PlanTextEditor("添加额外训练", "", "", { showExtraEditor = false }) { title, notes ->
            viewModel.addExtra(title, notes); showExtraEditor = false
        }
    }
    editingExtra?.let { extra ->
        PlanTextEditor("编辑额外训练", extra.title, extra.notes, { editingExtra = null }) { title, notes ->
            viewModel.updateExtra(extra.id, title, notes); editingExtra = null
        }
    }
    if (showExerciseEditor) {
        ExerciseEditor(
            initial = editingExercise,
            onDismiss = { showExerciseEditor = false },
            onSave = { id, name, category, equipment, notes ->
                viewModel.saveExercise(id, name, category, equipment, notes)
                showExerciseEditor = false
            }
        )
    }
    deletingExercise?.let { exercise ->
        AlertDialog(
            onDismissRequest = { deletingExercise = null },
            title = { Text("删除动作？") },
            text = { Text("删除只会移除动作库条目。后续正式训练历史会保存独立动作名称快照，不依赖动作库。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteExercise(exercise.id); deletingExercise = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingExercise = null }) { Text("取消") } }
        )
    }
    if (state.editingActionOwnerType != null) {
        val ownerLabel = when (state.editingActionOwnerType) { "WEEKLY_DAY" -> "每周计划日动作（兼容旧数据）"; "WORKOUT_TEMPLATE" -> "完整训练计划动作"; else -> "当天计划动作微调" }
        AlertDialog(
            onDismissRequest = viewModel::closeActionEditor,
            title = { Text(ownerLabel) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Text("已配置动作（目标数据可分别调整）", style = MaterialTheme.typography.titleSmall) }
                    itemsIndexed(state.planActions) { index, action ->
                        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(action.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = { viewModel.removeAction(index) }) { Text("移除") }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = action.targetSets,
                                        onValueChange = { viewModel.updateAction(index, targetSets = it) },
                                        label = { Text("组数") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = action.targetReps,
                                        onValueChange = { viewModel.updateAction(index, targetReps = it) },
                                        label = { Text("次数") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = action.targetWeight,
                                        onValueChange = { viewModel.updateAction(index, targetWeight = it) },
                                        label = { Text("kg") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                    item { Text("从动作库添加", style = MaterialTheme.typography.titleSmall) }
                    if (state.exercises.isEmpty()) item { Text("动作库为空，请先添加动作") }
                    items(state.exercises, key = { it.id }) { exercise ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name)
                                Text(exercise.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.addAction(exercise) }) { Text("添加") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::saveActionEditor) { Text("保存动作配置") } },
            dismissButton = { TextButton(onClick = viewModel::closeActionEditor) { Text("取消") } }
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
private fun WorkoutTemplateEditor(
    initial: WorkoutTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建完整训练计划" else "编辑训练计划信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("计划名称") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("训练部位 / 类型") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("说明") })
                if (initial == null) Text("创建后，在计划卡片中点击「编辑动作」添加整套动作。", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(initial?.id, name, category, description) }, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DailyPlanRow(
    plan: DailyWorkoutPlanEntity,
    onEdit: () -> Unit,
    onStatus: (WorkoutPlanStatus) -> Unit,
    onDelete: () -> Unit,
    onActions: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium)
                Text(if (plan.isPrimary) "主要训练" else "额外训练", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onEdit) { Text("调整") }
            TextButton(onClick = onActions) { Text("动作微调") }
            if (!plan.isPrimary) TextButton(onClick = onDelete) { Text("删除") }
        }
        Text("状态：${statusLabel(plan.status)}", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { onStatus(WorkoutPlanStatus.PLANNED) }) { Text("未完成") }
            TextButton(onClick = { onStatus(WorkoutPlanStatus.COMPLETED) }) { Text("完成") }
            TextButton(onClick = { onStatus(WorkoutPlanStatus.SKIPPED) }) { Text("跳过") }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "COMPLETED" -> "已完成"
    "SKIPPED" -> "已跳过"
    else -> "未完成"
}

@Composable
private fun PlanTextEditor(
    title: String,
    initialTitle: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(title, initialTitle) { mutableStateOf(initialTitle) }
    var notes by remember(title, initialNotes) { mutableStateOf(initialNotes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("训练名称") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("备注") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, notes) }, enabled = name.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun WeeklyPlanEditor(
    initial: List<com.dailyfit.app.data.local.WeeklyWorkoutPlanDayEntity>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, List<WeeklyPlanInput>) -> Unit
) {
    val titles = remember(initial) {
        mutableStateListOf<String>().apply {
            (1..7).forEach { day -> add(initial.firstOrNull { it.dayOfWeek == day }?.title ?: "") }
        }
    }
    var effectiveDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑每周模板") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    OutlinedTextField(effectiveDate, { effectiveDate = it }, label = { Text("生效日期 YYYY-MM-DD") }, singleLine = true)
                    Text("历史日期不会被新版本覆盖。", style = MaterialTheme.typography.bodySmall)
                }
                items(7) { index ->
                    OutlinedTextField(
                        value = titles[index],
                        onValueChange = { titles[index] = it },
                        label = { Text(listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[index]) },
                        singleLine = true
                    )
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = try { LocalDate.parse(effectiveDate) } catch (_: DateTimeParseException) { null }
                if (date == null) error = "日期格式无效"
                else {
                    onSave(date, titles.mapIndexed { index, value ->
                        WeeklyPlanInput(index + 1, value.ifBlank { "休息" }, isRestDay = value.isBlank() || value == "休息")
                    })
                }
            }) { Text("保存新版本") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ExerciseEditor(
    initial: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category.orEmpty()) }
    var equipment by remember(initial) { mutableStateOf(initial?.equipment.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增动作" else "编辑动作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("动作名称") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("训练部位 / 类别") }, singleLine = true)
                OutlinedTextField(equipment, { equipment = it }, label = { Text("器械") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("备注") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(initial?.id, name, category, equipment, notes) }, enabled = name.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
