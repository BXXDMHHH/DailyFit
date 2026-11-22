# DailyFit 技术设计

## 架构
Kotlin、Jetpack Compose、Material 3、MVVM、ViewModel + StateFlow、Room、Coroutines、Navigation Compose。Room 为本地唯一可信数据源；DAO → Repository → ViewModel → Compose。单 Activity、单模块，不引入不必要依赖。

## 已实现数据
- Habit / HabitRecord：固定习惯及每日快照。
- TemporaryTask：当天独立临时任务。
- WeeklyWorkoutPlanVersion / WeeklyWorkoutPlanDay：按生效日期版本化的每周计划。
- DailyWorkoutPlan：每日日程快照及额外安排。
- Exercise：独立动作库。\n- WorkoutTemplate：可复用的完整单次训练计划，保存名称、分类和说明。

## 阶段 4：训练执行与正式历史

### 进行中训练
WorkoutSession 保存 planId、businessDate、title、status、startedAt、updatedAt。status=IN_PROGRESS 表示可恢复草稿。
WorkoutSessionExercise 保存 exerciseId（可空）、exerciseNameSnapshot、categorySnapshot、equipmentSnapshot、notesSnapshot、排序、目标组数/次数/重量及临时动作标记。
WorkoutSessionSet 保存每组目标次数/重量、实际次数/重量、完成状态、更新时间。
每次有效修改直接写 Room；启动时查询最新 IN_PROGRESS session 并恢复其动作和组数据。进行中数据允许编辑、增加、删除；用户可确认放弃草稿，放弃只删除未完成会话，不生成正式历史。

### 训练重量进阶与 1RM
- 每个动作在训练执行页使用统一工作重量，快捷加减按钮作用于该动作的所有训练组；重量步进按动作配置，默认 2.5 kg。
- 最近一次已完成训练组的实际重量作为下次训练的默认工作重量；不自动增加重量，是否加重由用户根据完成情况决定。
- 训练组默认次数来自目标次数，只有实际次数不同才需要修改；工作重量、次数和完成状态仍实时写入进行态训练表。
- 每个动作的步进、最佳实际/估算 1RM 使用 app_metadata 保存，不修改模板或正式历史表。
- 估算 1RM 使用 Epley 公式：重量 × (1 + 次数 / 30)，仅根据本次已完成且次数为 1–30 的组计算；实际 1RM 由用户手动录入。两种记录分开保存，且只保留各自最佳值。

### 正式历史
WorkoutHistory 保存训练日期、标题快照、状态、起止时间和时长。
WorkoutHistoryExercise 与 WorkoutHistorySet 保存动作与组数据完整快照，不通过实时 JOIN 读取动作库。
完成、未完成、跳过均可生成正式历史状态：COMPLETED / INCOMPLETE / SKIPPED。
结束时在一个 Room transaction 中复制 session、exercise、set 到历史表，再将 session 状态关闭；相同 session 重复结束时返回已存在历史，避免重复生成。
历史 DAO 只提供查询与 Insert；不提供 Update/Delete。SQLite 对三张历史表分别创建 BEFORE UPDATE / BEFORE DELETE 触发器并 RAISE(ABORT)，在数据库层防止篡改与删除。
动作库删除或修改不会触及历史快照。

### 计划动作关联
WorkoutPlanExerciseEntity 通过 ownerType + ownerId 关联周模板日（WEEKLY_DAY）或单日计划（DAILY_PLAN），保存动作 ID、动作信息快照、排序、目标组数、目标次数和目标重量。
每周模板生成新版本时，按星期复制上一版本的动作配置；生成每日主要计划时，将对应星期动作复制为每日独立快照。如果每日计划已先于周模板动作配置生成，用户可在单日动作编辑器中显式选择“从每周模板覆盖当天动作”，不会静默改写已生成快照。单日动作调整不会回写周模板；后续周模板版本也不会覆盖已经生成的每日快照。
开始训练时，Repository 将每日计划动作及目标数据一次性复制到 WorkoutSessionExercise / WorkoutSessionSet。训练执行页再允许临时添加动作、增删动作组、修改实际重量/次数。计划、进行中会话和正式历史各自保存快照，不依赖动作库实时数据。

### 完整训练计划库（Room v7）
WorkoutTemplate 保存完整单次训练计划的元信息；WorkoutPlanExerciseEntity 通过 ownerType=WORKOUT_TEMPLATE、ownerId=模板 ID 保存模板动作快照、排序、目标组数、次数和重量。
- 预置肩部、胸部、背部、腿部、手臂五套完整训练计划。
- 模板元信息和动作列表可独立维护。
- 导入某日时，在事务中校验日期、主要训练状态、模板存在及无进行中训练，然后将模板元信息和全部动作复制到 DAILY_PLAN。
- DAILY_PLAN 副本不依赖模板实时数据。修改或删除模板不会改变已导入的每日训练。
- 每日训练开始时仍按既有流程复制到 WorkoutSession；正式保存时继续复制到不可变历史表。
- 周训练日程只负责长期循环与日期安排；WorkoutTemplate 负责某一训练日的完整动作内容，两者职责分离。

## Room 版本
v7。v4→v5 显式迁移新增训练会话、训练组、正式历史表和历史不可变触发器；v5→v6 新增 workout_plan_exercises 及索引；v6→v7 新增 workout_templates 及分类、名称索引。新安装通过 RoomDatabase.Callback 同样创建正式历史 UPDATE/DELETE 保护触发器。所有迁移均保留既有业务数据，不使用 destructive migration。

## 测试策略
覆盖训练草稿持久化与恢复、计划动作跨周模板/每日快照/会话/历史的传递、单日覆盖独立性、实际数据保存、动作库修改/删除后的历史快照完整性、正式历史三张表 SQL UPDATE/DELETE 被触发器拒绝、未完成/跳过/放弃状态、训练次数/完成组数/训练量统计。习惯历史展示连续有打卡天数。CI 结果未完成前不标记本阶段验收通过。


## 阶段 5 页面与统计整合
- Home 仅负责今日执行入口与进度；固定习惯定义在 Plan 页面管理。
- Plan 页面聚合固定习惯、周计划、单日计划和动作库，所有业务状态仍由既有 ViewModel/Repository/Room 提供。
- History 页面以单个 LazyColumn 统一呈现固定习惯、临时任务和训练统计及历史详情；三个统计口径保持独立。
- 连续打卡按“当天有至少一条已完成习惯记录”计为该日打卡，若今天尚无完成记录则从昨天开始回溯。
- Material 3 采用固定品牌色板，浅色/深色模式共享一致的语义色。
- 仅补齐页面整合、基础统计和状态反馈，不新增成就、图表、云同步或其他未确认能力。
