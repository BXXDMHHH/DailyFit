# DailyFit 技术设计

## 架构
Kotlin、Jetpack Compose、Material 3、MVVM、ViewModel + StateFlow、Room、Coroutines、Navigation Compose。Room 为本地唯一可信数据源；DAO → Repository → ViewModel → Compose。单 Activity、单模块，不引入不必要依赖。

## 已实现数据
- Habit / HabitRecord：固定习惯及每日快照。
- TemporaryTask：当天独立临时任务。
- WeeklyWorkoutPlanVersion / WeeklyWorkoutPlanDay：按生效日期版本化的每周计划。
- DailyWorkoutPlan：每日日程快照及额外安排。
- Exercise：独立动作库。

## 阶段 4：训练执行与正式历史

### 进行中训练
WorkoutSession 保存 planId、businessDate、title、status、startedAt、updatedAt。status=IN_PROGRESS 表示可恢复草稿。
WorkoutSessionExercise 保存 exerciseId（可空）、exerciseNameSnapshot、categorySnapshot、equipmentSnapshot、notesSnapshot、排序、目标组数/次数/重量及临时动作标记。
WorkoutSessionSet 保存每组目标次数/重量、实际次数/重量、完成状态、更新时间。
每次修改直接写 Room；启动时查询最新 IN_PROGRESS session 并恢复其动作和组数据。进行中数据允许编辑、增加、删除。

### 正式历史
WorkoutHistory 保存训练日期、标题快照、状态、起止时间和时长。
WorkoutHistoryExercise 与 WorkoutHistorySet 保存动作与组数据完整快照，不通过实时 JOIN 读取动作库。
完成、未完成、跳过均可生成正式历史状态：COMPLETED / INCOMPLETE / SKIPPED。
结束时在一个 Room transaction 中复制 session、exercise、set 到历史表，再将 session 状态关闭；相同 session 重复结束时返回已存在历史，避免重复生成。
历史 DAO 只提供查询与 Insert；不提供 Update/Delete。SQLite 对三张历史表分别创建 BEFORE UPDATE / BEFORE DELETE 触发器并 RAISE(ABORT)，在数据库层防止篡改与删除。
动作库删除或修改不会触及历史快照。

### 计划动作关联限制
当前 DailyWorkoutPlan 与 WeeklyWorkoutPlanDay 只保存训练名称和备注，不保存动作清单。阶段 4 执行界面允许从独立动作库添加动作和临时动作，但从计划自动加载动作/目标组数必须在后续增加计划动作关联数据后实现，不能从计划标题猜测动作。

## Room 版本
v5。v4→v5 显式迁移新增 workout_sessions、workout_session_exercises、workout_session_sets、workout_history、workout_history_exercises、workout_history_sets，以及索引和正式历史不可变触发器。不删除或重写 v4 既有数据，不启用 destructive migration。

## 测试策略
覆盖训练草稿持久化与恢复、实际数据保存、动作库修改/删除后的历史快照完整性、正式历史 SQL UPDATE/DELETE 被触发器拒绝、未完成与跳过状态。CI 结果未完成前不标记本阶段验收通过。
