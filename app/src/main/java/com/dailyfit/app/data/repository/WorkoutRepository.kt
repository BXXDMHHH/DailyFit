package com.dailyfit.app.data.repository

import androidx.room.withTransaction
import com.dailyfit.app.data.local.*
import java.time.Clock
import java.util.UUID
import com.dailyfit.app.domain.model.WorkoutInputRules

class WorkoutRepository(private val db:DailyFitDatabase, private val clock:Clock=Clock.systemDefaultZone()) {
 private val dao=db.workoutSessionDao()
 private fun id()=UUID.randomUUID().toString()
 fun observeActive()=dao.observeActive()
 fun observeHistory()=dao.observeHistory()
 fun observeHistoryDetail(id:String)=dao.observeHistoryById(id)
 fun observeHistoryExercises(id:String)=dao.observeHistoryExercises(id)
 fun observeHistorySets(id:String)=dao.observeHistorySets(id)
 fun observeSessionExercises(id:String)=dao.observeExercises(id)
 fun observeSessionSets(id:String)=dao.observeSets(id)
 suspend fun start(plan:DailyWorkoutPlanEntity, actions:List<WorkoutPlanExerciseEntity> = emptyList()):String = db.withTransaction {
  check(plan.status=="PLANNED") { "这份训练安排已完成或跳过，请添加额外训练" }
  check(dao.getActive()==null) { "已有未完成训练，请先恢复或结束" }
  val sessionId=id()
  val now=clock.millis()
  dao.insertSession(WorkoutSessionEntity(sessionId,plan.id,plan.businessDate,plan.title,"IN_PROGRESS",now,now))
  actions.sortedBy { it.sortOrder }.forEachIndexed { index, action ->
   require(action.targetSets in 1..30 && WorkoutInputRules.isValidWeight(action.targetWeight))
   val targetReps = WorkoutInputRules.parseTargetReps(action.targetReps)
   val exerciseId=id()
   dao.insertExercise(WorkoutSessionExerciseEntity(
    exerciseId,sessionId,action.exerciseId,action.exerciseNameSnapshot,action.categorySnapshot,
    action.equipmentSnapshot,action.notesSnapshot,index,action.targetSets,action.targetReps,
    action.targetWeight,false
   ))
   val reps=targetReps
   dao.insertSets((1..action.targetSets).map { setNumber ->
    WorkoutSessionSetEntity(id(),exerciseId,setNumber,reps,action.targetWeight,null,null,false,now)
   })
  }
  sessionId
 }
 suspend fun addExercise(sessionId:String, exercise:ExerciseEntity?, temporaryName:String?=null,
                         targetSets:Int=3,targetReps:String="8-12",targetWeight:Double=0.0) = db.withTransaction {
  require(targetSets in 1..30)
  require(WorkoutInputRules.isValidWeight(targetWeight)) { "目标重量必须是非负有限数值" }
  val targetRepCount = WorkoutInputRules.parseTargetReps(targetReps)
  val session=dao.get(sessionId) ?: error("训练不存在")
  check(session.status=="IN_PROGRESS") { "训练已结束，不能修改" }
  val name=exercise?.name ?: temporaryName?.trim().orEmpty()
  require(name.isNotBlank()) { "动作名称不能为空" }
  val exId=id()
  val order=dao.getExercises(sessionId).size
  dao.insertExercise(WorkoutSessionExerciseEntity(exId,sessionId,exercise?.id,name,
   exercise?.category.orEmpty(),exercise?.equipment.orEmpty(),exercise?.notes.orEmpty(),
   order,targetSets,targetReps,targetWeight,exercise==null))
  dao.insertSets((1..targetSets).map { n -> WorkoutSessionSetEntity(id(),exId,n,
   targetRepCount,targetWeight,null,null,false,clock.millis()) })
 }
 suspend fun removeExercise(sessionId:String,exerciseId:String) = db.withTransaction {
  val session=dao.get(sessionId) ?: error("训练不存在"); check(session.status=="IN_PROGRESS")
  dao.deleteSetsForExercise(exerciseId,sessionId); dao.deleteExercise(exerciseId,sessionId)
 }
 suspend fun addSet(sessionId:String,exerciseId:String) = db.withTransaction {
  val session=dao.get(sessionId) ?: error("训练不存在"); check(session.status=="IN_PROGRESS")
  check(dao.getExerciseForSession(exerciseId,sessionId) != null) { "动作不属于当前训练" }
  val sets=dao.getSets(exerciseId); val last=sets.lastOrNull()
  dao.insertSets(listOf(WorkoutSessionSetEntity(id(),exerciseId,((sets.maxOfOrNull { it.setNumber } ?: 0)+1),
   last?.targetReps ?: 0,last?.targetWeight ?: 0.0,null,null,false,clock.millis())))
 }
 suspend fun removeSet(sessionId:String,exerciseId:String,setId:String) = db.withTransaction {
  val session=dao.get(sessionId) ?: error("训练不存在"); check(session.status=="IN_PROGRESS")
  check(dao.getExerciseForSession(exerciseId,sessionId) != null) { "动作不属于当前训练" }
  val sets=dao.getSets(exerciseId); require(sets.size>1) { "每个动作至少保留一组" }
  check(dao.deleteSet(setId,exerciseId,sessionId)==1) { "训练组不存在或已发生变化" }
 }
 suspend fun updateSet(sessionId:String,setId:String,weight:Double?,reps:Int?,done:Boolean) = db.withTransaction {
  require(weight==null || (weight.isFinite() && weight>=0)) { "实际重量必须是非负有限数值" }
  require(reps==null || reps in 0..999) { "实际次数必须在 0 到 999 之间" }
  val now=clock.millis()
  check(dao.updateSet(setId,sessionId,weight,reps,done,now)>0) { "训练已结束或数据不存在" }
  check(dao.touch(sessionId,now)>0) { "训练已结束或数据不存在" }
 }
 suspend fun rename(sessionId:String,title:String) {
  require(title.isNotBlank()); check(dao.rename(sessionId,title.trim(),clock.millis())>0)
 }
 suspend fun finish(sessionId:String,status:String="COMPLETED"):String = db.withTransaction {
  require(status in setOf("COMPLETED","INCOMPLETE","SKIPPED"))
  val session=dao.get(sessionId) ?: error("训练不存在")
  dao.historyForSession(sessionId)?.let { return@withTransaction it.id }
  check(session.status=="IN_PROGRESS") { "训练已结束" }
  val now=clock.millis(); val historyId=id()
  val exercises=dao.getExercises(sessionId)
  val history=WorkoutHistoryEntity(historyId,sessionId,session.planId,session.businessDate,
   session.title,status,session.startedAt,now,((now-session.startedAt)/1000).coerceAtLeast(0))
  dao.insertHistory(history)
  val exerciseRows=exercises.map { ex -> WorkoutHistoryExerciseEntity(id(),historyId,ex.exerciseId,
   ex.exerciseNameSnapshot,ex.categorySnapshot,ex.equipmentSnapshot,ex.notesSnapshot,
   ex.sortOrder,ex.targetSets,ex.targetReps,ex.targetWeight,ex.isTemporary) }
  dao.insertHistoryExercises(exerciseRows)
  exerciseRows.zip(exercises).forEach { (dest,src) ->
   val sets=dao.getSets(src.id).map { s -> WorkoutHistorySetEntity(id(),dest.id,s.setNumber,
    s.targetReps,s.targetWeight,s.actualReps,s.actualWeight,s.isCompleted) }
   if(sets.isNotEmpty()) dao.insertHistorySets(sets)
  }
  dao.setStatus(sessionId,status,now)
  session.planId?.let { db.dailyWorkoutPlanDao().updateStatus(it,session.businessDate,if(status=="COMPLETED") "COMPLETED" else if(status=="SKIPPED") "SKIPPED" else "PLANNED",now) }
  historyId
 }
 suspend fun discard(sessionId:String) = db.withTransaction {
  val session=dao.get(sessionId) ?: return@withTransaction
  check(session.status=="IN_PROGRESS") { "训练已结束，不能放弃" }
  dao.deleteSetsForSession(sessionId)
  dao.deleteExercisesForSession(sessionId)
  check(dao.deleteActiveSession(sessionId)==1) { "训练状态已变化，请重试" }
 }
 suspend fun recoverableSession():WorkoutSessionEntity? = db.workoutSessionDao().getActive()
}
