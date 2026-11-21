package com.dailyfit.app.feature.history
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyfit.app.data.local.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
@Composable fun WorkoutHistorySection(database:DailyFitDatabase){
 val dao=database.workoutSessionDao();var rows by remember{mutableStateOf(emptyList<WorkoutHistoryEntity>())};var selected by remember{mutableStateOf<WorkoutHistoryEntity?>(null)}
 var details by remember{mutableStateOf(emptyList<Pair<WorkoutHistoryExerciseEntity,List<WorkoutHistorySetEntity>>>())}
 val scope=rememberCoroutineScope()
 LaunchedEffect(Unit){dao.observeHistory().collect{rows=it}}
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
 Text("训练历史",style=MaterialTheme.typography.titleLarge)
 if(rows.isEmpty())Text("还没有正式训练记录",color=MaterialTheme.colorScheme.onSurfaceVariant)
 rows.forEach{row->Card(Modifier.fillMaxWidth()){TextButton(onClick={scope.launch{selected=row;val ex=dao.observeHistoryExercises(row.id).first();details=ex.map{it to dao.observeHistorySets(it.id).first()}}}){Column{Text(row.titleSnapshot);Text(row.businessDate+" · "+row.status+" · "+row.durationSeconds/60+" 分钟")}}}}
 }
 selected?.let{row->AlertDialog(onDismissRequest={selected=null},title={Text(row.titleSnapshot)},text={Column{Text(row.businessDate+" · "+row.status);details.forEach{(ex,sets)->Text(ex.exerciseNameSnapshot,style=MaterialTheme.typography.titleSmall);sets.forEach{Text("第"+it.setNumber+"组：实际 "+(it.actualWeight?.toString()?: "—")+" kg × "+(it.actualReps?.toString()?: "—")+" 次"+if(it.isCompleted)" ✓" else "")}};Text("历史记录永久只读，不能修改或删除。")}},confirmButton={TextButton(onClick={selected=null}){Text("关闭")}})}
}
