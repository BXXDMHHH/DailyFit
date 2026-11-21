# DailyFit 技术设计

> 版本：V1.0  
> 状态：随阶段实现持续更新  
> 依据：docs/PRD.md 与已确认的 DailyFit 产品规则。

## 1. 设计目标与边界
- Kotlin、Jetpack Compose、Material 3、MVVM、ViewModel + StateFlow、Room、Coroutines、Navigation Compose。
- 本地优先，Room 是业务数据唯一可信来源；V1.0 不引入账号、服务端或云同步。
- 单 Activity、单 app module、单 Room 数据库；DAO + Repository，不引入不必要的 UseCase 或 DI 框架。
- 业务主键使用稳定 UUID；日期统一保存 ISO-8601 yyyy-MM-dd，时间戳使用 epoch millis。
- 正式训练历史必须永久只读、禁止修改和删除；正式保存时必须包含动作名称和训练组数据快照。

## 2. 分层职责
| 层 | 职责 |
|---|---|
| Compose Screen | 展示 UI State、收集用户事件；不直接访问 DAO |
| ViewModel | 调用 Repository、管理 StateFlow、处理界面事件 |
| Repository | 对应模块业务规则、日期限制、统计计算 |
| DAO | Room 查询、插入和受限更新 |
| Room Database | 表结构、索引、外键、显式迁移 |

Repository 直接构造注入到 ViewModel Factory；当前规模不需要额外 DI 框架。

## 3. 已实现：固定习惯
HabitEntity 保存 UUID、名称、DAILY / WEEKDAYS / CUSTOM 频率、自定义 ISO 星期、启停状态和时间戳。HabitRecordEntity 保存 UUID、habitId、businessDate、habitNameSnapshot、完成状态、完成时间和创建时间。UNIQUE(habitId, businessDate)，并为 businessDate 建索引。外键删除策略 RESTRICT；习惯停用不删除历史。修改习惯不回写记录快照；历史打卡不可补签。

## 4. 已实现：临时任务
TemporaryTaskEntity 保存 UUID、businessDate、title、isCompleted、createdAt、updatedAt、completedAt。按 businessDate 和 businessDate + isCompleted 建索引。创建日期固定；仅当天允许编辑、完成/取消和删除。历史任务保留用于独立统计，不参与固定习惯完成率。

## 5. 已实现：每周训练计划与单日安排

### 5.1 WeeklyWorkoutPlanVersionEntity
- id：UUID 主键。
- effectiveFromDate：版本生效日期。
- createdAt：版本创建时间。
- 通过 effectiveFromDate <= 查询日期，按生效日期和创建时间倒序选择适用版本。
- 修改周计划不更新旧版本，而是插入新版本；过去已生成的每日安排不被覆盖。

### 5.2 WeeklyWorkoutPlanDayEntity
- id：UUID 主键。
- versionId：所属周模板版本，外键引用版本表，删除版本时级联删除。
- dayOfWeek：ISO 星期，周一=1 至周日=7。
- title、notes、isRestDay：当天安排内容。
- UNIQUE(versionId, dayOfWeek)，确保每个版本完整七天且每一天唯一。

### 5.3 DailyWorkoutPlanEntity
- id：UUID 主键。
- businessDate：对应日期。
- title、notes：当天快照内容。
- status：PLANNED / COMPLETED / SKIPPED。
- isPrimary：区分主要安排和额外安排。
- sourceVersionId：生成时采用的周模板版本 ID 快照，不建立删除级联。
- createdAt、updatedAt：时间戳。
- 首次查看日期时，Repository 根据当日生效周模板创建主要安排；该记录后续独立保存。
- 编辑主要安排只更新该日期记录，不更新周模板；额外安排独立插入和删除。
- 修改周模板不会重写已经生成的 DailyWorkoutPlan。

### 5.4 ExerciseEntity
- id：UUID 主键。
- name、category、equipment、notes：动作信息。
- createdAt、updatedAt：时间戳。
- 支持全量观察和按名称/类别 LIKE 搜索。
- 动作库不与周计划或每日计划建立级联外键，避免动作库删除导致计划数据丢失。
- 正式训练历史尚未实现；后续历史必须复制 exerciseNameSnapshot 等必要信息，不能依赖当前动作库行。

## 6. 日期与统计规则
- HabitRepository 和 TemporaryTaskRepository 各自独立统计。
- 固定习惯完成率只统计 HabitRecord。
- 临时任务今日完成率 = 今日已完成临时任务数 / 今日临时任务总数；累计统计也只统计临时任务。
- 训练安排状态只作用于对应日期记录；跳过或未完成不修改周模板，不影响下一周。
- 设备当前时区 LocalDate 为业务日期，数据库保存 ISO 日期字符串。

## 7. 首页、计划页与 ViewModel
- HomeScreen 继续显示习惯、临时任务和训练入口占位。
- PlanScreen 提供每周模板、单日安排、额外训练和动作库管理/搜索。
- PlanViewModel 通过 StateFlow 暴露所选日期、周模板、每日安排、动作库和搜索词。
- TrainingPlanRepository 负责周版本生成、每日主要安排快照、额外训练 CRUD、状态变更和动作库 CRUD。
- DailyFitApplication 提供 TrainingPlanRepository；MainActivity 注入 PlanViewModel。
- Compose 页面不直接访问 DAO。

## 8. 数据库版本和迁移
当前 Room 数据库版本为 4：
- v1：app_metadata。
- v2：habits、habit_records。
- v3：temporary_tasks。
- v4：weekly_workout_plan_versions、weekly_workout_plan_days、daily_workout_plans、exercises。
- 使用显式 MIGRATION_1_2、MIGRATION_2_3、MIGRATION_3_4；不使用 destructive migration。
- v3→v4 只新增表和索引，不改写既有习惯、打卡和临时任务数据。
- Room Schema 2、3、4 随项目提交。

## 9. 测试策略
阶段 1、2 的既有测试继续运行。阶段 3 新增 TrainingPlanRepositoryTest 覆盖七天初始化、有效日期版本切换、单日覆盖隔离、历史快照稳定、额外训练与状态、非法模板校验、动作库 CRUD/搜索、v3→v4 迁移。阶段 3 GitHub Actions #44929615591：29 项单元测试全部通过，Debug APK 构建成功。

## 10. 后续阶段设计基线
训练执行阶段加入 WorkoutSession、WorkoutSessionExercise、WorkoutSet 等实体。训练过程中的未完成数据需要增量保存和恢复；正式完成时生成只读历史快照。历史层不能暴露 update/delete DAO 或 Repository 方法，并使用 SQLite 触发器保护正式历史不可变。
