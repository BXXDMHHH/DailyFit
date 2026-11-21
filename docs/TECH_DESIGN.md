# DailyFit 技术设计

> 版本：V1.0  
> 状态：随阶段实现持续更新  
> 依据：docs/PRD.md 与已确认的 DailyFit 产品规则。

## 1. 设计目标与边界
- Kotlin、Jetpack Compose、Material 3、MVVM、ViewModel + StateFlow、Room、Coroutines、Navigation Compose。
- 本地优先，Room 是业务数据唯一可信来源；V1.0 不引入账号、服务端或云同步。
- 单 Activity、单 app module、单 Room 数据库；DAO + Repository，不引入不必要的 UseCase 或 DI 框架。
- 业务主键使用稳定 UUID；日期统一保存 ISO-8601 yyyy-MM-dd，时间戳使用 epoch millis。
- 正式训练历史未来实现时必须永久只读、禁止修改和删除。

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

### 4.1 TemporaryTaskEntity
- id：UUID String 主键。
- businessDate：创建时设备本地日期，ISO yyyy-MM-dd。
- title：任务名称。
- isCompleted：完成状态。
- createdAt、updatedAt：epoch millis。
- completedAt：完成时间，可空。

索引：businessDate 用于按日期查询；businessDate + isCompleted 用于按日期和状态聚合。临时任务不关联 Habit，保持模块独立。

### 4.2 TemporaryTaskDao
- observeForDate(date)：只查询指定日期。
- observeAll()：历史统计使用。
- insert()：创建当天任务。
- updateToday()、setTodayCompletion()、deleteToday()：所有变更 SQL 均带业务日期条件，避免旧任务被改写。

### 4.3 TemporaryTaskRepository
- 创建任务时从注入 Clock 获取当前本地日期，写入 businessDate。
- 只允许修改、完成/取消完成、删除当前日期的任务。
- 旧日期任务保留在表中，不会自动复制到新日期。
- Dashboard 提供今日任务、今日完成率和临时任务累计完成率。
- 所有统计仅基于 temporary_tasks，不读取 habit_records。

## 5. 日期与统计规则
- HabitRepository 和 TemporaryTaskRepository 各自独立统计。
- 固定习惯完成率只统计 HabitRecord。
- 临时任务今日完成率 = 今日已完成临时任务数 / 今日临时任务总数。
- 临时任务累计完成率 = 所有日期已完成临时任务数 / 所有临时任务总数。
- 分母为 0 时显示 0%。
- 日期变化由现有 HabitViewModel 的 ON_RESUME 和午夜刷新机制驱动，两个模块使用同一 LocalDate。
- 临时任务不执行日期生成；新日期只显示新日期创建的任务。

## 6. 首页与 ViewModel
- HomeScreen 包含今日习惯、固定习惯管理、今日临时任务、今日训练占位卡片。
- 临时任务卡片支持新增、勾选/取消、编辑、删除。
- HabitUiState 增加独立 TemporaryTaskDashboard。
- HabitViewModel 复用现有日期 StateFlow，将 HabitDashboard、习惯历史、TemporaryTaskDashboard 合并为首页状态。
- DailyFitApplication 提供同一 Room 数据库的两个独立 Repository。
- MainActivity 的 ViewModel Factory 同时注入两个 Repository。

## 7. 数据库版本和迁移
当前 Room 数据库版本为 3：v1 为 app_metadata；v2 保留 app_metadata 并新增 habits、habit_records；v3 新增 temporary_tasks，以及 businessDate、businessDate+isCompleted 索引。使用显式 MIGRATION_1_2 和 MIGRATION_2_3；不使用 destructive migration。v1→v3 迁移链保留旧 metadata、习惯和打卡数据。

## 8. 测试策略
阶段 1 习惯测试继续运行，验证原习惯逻辑不回归。阶段 2新增 Room / Repository 测试覆盖创建、编辑、删除、完成/取消、空标题、跨日隔离、历史保留、统计隔离和 v2→v3 迁移。CI 执行 Room Schema 生成、全部 JVM 单元测试和 Debug APK 构建。

## 9. 后续阶段设计基线
后续按单数据库、稳定 UUID、显式迁移原则加入 WeeklyWorkoutPlan、DailyWorkoutPlan、Exercise、WorkoutSession、WorkoutExercise 和 WorkoutSet。正式训练保存时复制动作名称和每组数据快照；正式历史只读保护需由 Repository API 和 SQLite 触发器双层实现。
