package com.dailyfit.app.feature.workout
import androidx.lifecycle.*
import com.dailyfit.app.data.local.*
import com.dailyfit.app.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
data class WorkoutExerciseUi(val row:WorkoutSessionExerciseEntity,val sets:List<WorkoutSessionSetEntity>)
data class WorkoutUiState(val session:WorkoutSessionEntity?=null,val exercises:List<WorkoutExerciseUi> = emptyList(),val library:List<ExerciseEntity> = emptyList(),val history:List<WorkoutHistoryEntity> = emptyList(),val message:String?=null)
class WorkoutViewModel(private val workouts:WorkoutRepository,private val plans:TrainingPlanRepository,private val database:DailyFitDatabase):ViewModel(){
 private val _state=MutableStateFlow(WorkoutUiState()); val state=_state.asStateFlow()
 init { viewModelScope.launch { workouts.observeActive().collect { active -> _state.update{it.copy(session=active)}; if(active!=null) refresh(active.id) } }; viewModelScope.launch { workouts.observeHistory().collect{rows->_state.update{it.copy(history=rows)}} } }
 fun refresh(id:String){viewModelScope.launch{val dao=database.workoutSessionDao();val ex=dao.getExercises(id);_state.update{it.copy(exercises=ex.map{e->WorkoutExerciseUi(e,dao.getSets(e.id))})}}}
 fun start(plan:DailyWorkoutPlanEntity){viewModelScope.launch{runCatching{if(database.workoutSessionDao().getActive()!=null)error("已有未完成训练，请先恢复");workouts.start(plan,emptyList())}.onSuccess{refresh(it)}.onFailure{message(it)}}}
 fun loadLibrary(){viewModelScope.launch{_state.update{it.copy(library=database.exerciseDao().observeAll().first())}}}
 fun addLibraryExercise(e:ExerciseEntity){val id=_state.value.session?.id?:return;viewModelScope.launch{runCatching{workouts.addExercise(id,e);refresh(id)}.onFailure{message(it)}}}
 fun addTemporary(n:String){val id=_state.value.session?.id?:return;viewModelScope.launch{runCatching{workouts.addExercise(id,null,n);refresh(id)}.onFailure{message(it)}}}
 fun addSet(e:String){val id=_state.value.session?.id?:return;viewModelScope.launch{runCatching{workouts.addSet(id,e);refresh(id)}.onFailure{message(it)}}}
 fun removeSet(e:String,s:String){val id=_state.value.session?.id?:return;viewModelScope.launch{runCatching{workouts.removeSet(id,e,s);refresh(id)}.onFailure{message(it)}}}
 fun removeExercise(e:String){val id=_state.value.session?.id?:return;viewModelScope.launch{runCatching{workouts.removeExercise(id,e);refresh(id)}.onFailure{message(it)}}}
 fun updateSet(s:WorkoutSessionSetEntity,w:String,r:String,d:Boolean){val id=_state.value.session?.id?:return;viewModelScope.launch{runCatching{workouts.updateSet(id,s.id,w.toDoubleOrNull(),r.toIntOrNull(),d);refresh(id)}.onFailure{message(it)}}}
 fun finish(status:String){val id=_state.value.session?.id?:return;viewModelScope.launch{runCatching{workouts.finish(id,status)}.onSuccess{_state.update{it.copy(session=null,exercises=emptyList(),message="训练记录已永久保存")}}.onFailure{message(it)}}}
 fun dismiss(){_state.update{it.copy(message=null)}}
 private fun message(t:Throwable){_state.update{it.copy(message=t.message?:"操作失败")}}
}
class WorkoutViewModelFactory(private val w:WorkoutRepository,private val p:TrainingPlanRepository,private val d:DailyFitDatabase):ViewModelProvider.Factory{
 @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(c:Class<T>):T{if(c.isAssignableFrom(WorkoutViewModel::class.java))return WorkoutViewModel(w,p,d) as T;error("Unknown ViewModel")}
}