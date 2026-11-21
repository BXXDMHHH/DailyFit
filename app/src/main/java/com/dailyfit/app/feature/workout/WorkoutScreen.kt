package com.dailyfit.app.feature.workout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyfit.app.data.local.*
@Composable fun WorkoutScreen(vm:WorkoutViewModel,plans:List<DailyWorkoutPlanEntity>,onBack:()->Unit,modifier:Modifier=Modifier){
 val s by vm.state.collectAsStateWithLifecycle();var showLibrary by remember{mutableStateOf(false)};var temp by remember{mutableStateOf("")};var showTemp by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){vm.loadLibrary()}
 LazyColumn(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 item{Row{Text("训练执行",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.weight(1f));TextButton(onClick=onBack){Text("返回")}}}
 if(s.session==null){item{Text("选择今天的训练安排开始记录")};items(plans,key={it.id}){p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(p.title,style=MaterialTheme.typography.titleMedium);Text(p.notes);Button(onClick={vm.start(p)}){Text("开始训练")}}}}}
 else{
 item{Text(s.session!!.title,style=MaterialTheme.typography.titleLarge);Text("训练自动保存；重新打开应用会恢复未完成训练。")}
 items(s.exercises,key={it.row.id}){ex->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
 Text(ex.row.exerciseNameSnapshot,style=MaterialTheme.typography.titleMedium)
 Text("目标："+ex.row.targetSets+"组 · "+ex.row.targetReps+"次 · "+ex.row.targetWeight+"kg")
 ex.sets.forEach{set->var w by remember(set.id,set.actualWeight){mutableStateOf(set.actualWeight?.toString()?: "")};var r by remember(set.id,set.actualReps){mutableStateOf(set.actualReps?.toString()?: "")}
 Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("第"+set.setNumber+"组",Modifier.width(55.dp));OutlinedTextField(w,{w=it;vm.updateSet(set,it,r,set.isCompleted)},Modifier.weight(1f),label={Text("kg")},singleLine=true);OutlinedTextField(r,{r=it;vm.updateSet(set,w,it,set.isCompleted)},Modifier.weight(1f),label={Text("次数")},singleLine=true);Checkbox(set.isCompleted,{vm.updateSet(set,w,r,it)});TextButton(onClick={vm.removeSet(ex.row.id,set.id)}){Text("删")}}
 }
 TextButton(onClick={vm.addSet(ex.row.id)}){Text("＋ 添加一组")};TextButton(onClick={vm.removeExercise(ex.row.id)}){Text("删除动作")}
 }}}
 item{Row{Button(onClick={showLibrary=true}){Text("添加动作")};TextButton(onClick={showTemp=true}){Text("临时动作")}}}
 item{Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){OutlinedButton(onClick={vm.finish("INCOMPLETE")}){Text("未完成")};OutlinedButton(onClick={vm.finish("SKIPPED")}){Text("跳过")};Button(onClick={vm.finish("COMPLETED")}){Text("完成训练")}}}
 }}
 }
 if(showLibrary)AlertDialog(onDismissRequest={showLibrary=false},title={Text("选择动作")},text={LazyColumn{items(s.library,key={it.id}){e->TextButton(onClick={vm.addLibraryExercise(e);showLibrary=false}){Text(e.name)}}}},confirmButton={TextButton(onClick={showLibrary=false}){Text("关闭")}})
 if(showTemp)AlertDialog(onDismissRequest={showTemp=false},title={Text("临时训练动作")},text={OutlinedTextField(temp,{temp=it},label={Text("动作名称")})},confirmButton={TextButton(onClick={vm.addTemporary(temp);temp="";showTemp=false}){Text("添加")}})
 s.message?.let{AlertDialog(onDismissRequest=vm::dismiss,title={Text("提示")},text={Text(it)},confirmButton={TextButton(onClick=vm::dismiss){Text("知道了")}})}
}