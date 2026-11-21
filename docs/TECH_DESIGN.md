# DailyFit 技术设计

> 版本：V1.0  
> 状态：随阶段实现持续更新  
> 依据：`docs/PRD.md` 与已确认的 DailyFit 产品规则。

## 1. 设计目标与边界
- Kotlin、Jetpack Compose、Material 3、MVVM、ViewModel + StateFlow、Room、Coroutines、Navigation Compose。
- 本地优先，Room 是业务数据唯一可信来源；V1.0 不引入账号、服务端或云同步。
- 单 Activity、单 app module、单 Room 数据库；DAO + Repository，不引入不必要的 UseCase 或 DI 框架。
- 业务主键使用稳定 UUID；日期统一保存 ISO-8601 `yyyy-MM-dd`，时间戳使用 epoch millis。
- 正式训练历史未来实现时必须永久只读、禁止修改和删除。

## 2. 当前仓库审计与可复用代码
当前 `main` 分支已有真实 Android 工程，阶段 0 与阶段 1 CI 已通过。可复用部分：
- Gradle / Android 应用配置、Manifest、Application、MainActivity。
- Material 3 主题、Navigation Compose 三个一级页面入口。
- Room 数据库基础表、显式迁移和导出 Schema。
- 纯函数习惯频率规则 `HabitScheduleRules`。

阶段 1 沿用原有 package 结构，不搬迁工程、不重构导航、不引入 Hilt/Koin。Room 原 v1 数据库包含 `app_metadata`，通过显式迁移升级，不删除原表。

## 3. 分层职责
| 层 | 职责 |
|---|---|
| Compose Screen | 展示 UI State、收集用户事件；不直接访问 DAO |
| ViewModel | 调用 Repository、管理 StateFlow、处理界面事件 |
| Repository | 习惯业务规则、日期生成、跨 DAO 事务、完成率计算 |
| DAO | Room 查询、插入和受限更新 |
| Room Database | 表结构、索引、外键、迁移 |

Repository 直接构造注入到 ViewModel Factory；当前规模不需要额外 DI 框架。

## 4. 已实现：固定习惯数据模型

### 4.1 HabitEntity
- `id`：UUID String 主键
- `name`：习惯名称
- `frequency`：DAILY / WEEKDAYS / CUSTOM
- `customDays`：ISO 星期数字 1–7 的逗号分隔值
- `isActive`：软停用状态
- `createdAt` / `updatedAt`：epoch millis

### 4.2 HabitRecordEntity
- `id`：UUID String 主键
- `habitId`：关联 Habit.id，外键删除策略 RESTRICT
- `businessDate`：yyyy-MM-dd
- `habitNameSnapshot`：生成任务时的名称快照
- `isCompleted` / `completedAt`：完成状态及时间
- `createdAt`：记录创建时间

约束与索引：
- UNIQUE(`habitId`, `businessDate`)：同一习惯同一天最多一条记录。
- `businessDate` 普通索引：支持历史按日期查询。
- 停用习惯不删除定义；不提供物理删除入口，历史记录不级联删除。

## 5. 日期生成与不可补签规则
- Repository 使用设备当前时区的 `Clock.systemDefaultZone()` 获取 `LocalDate`。
- `ensureTodayRecords()` 只计算当前日期，不接受任意历史日期参数，不循环补建过去日期。
- 每次创建/编辑习惯、应用启动和日期同步时，在 Room transaction 内检查启用习惯及频率，并对当前日期执行 insert-ignore。
- 唯一索引保证重复进入、重复触发不会重复生成任务。
- 频率计算使用 ISO 星期：周一=1 至周日=7。
- 昨天未完成记录不会被改写，也不会补签。
- 打卡更新 SQL 同时限定 `businessDate = today`，过去记录无法通过 Repository 完成或取消完成。
- 前台 `ON_RESUME` 时刷新日期；ViewModel 另安排一次延迟任务到本地次日零点，避免 App 持续停留前台时日期不更新。进程重启后会重新同步。

## 6. 修改和停用的历史保护
- 编辑习惯只更新 Habit 定义，不更新任何 HabitRecord。
- 每条 HabitRecord 保存生成时的 `habitNameSnapshot`，改名不会改变历史展示。
- 已存在的当日记录也不被编辑动作覆盖；若当天记录已生成，频率变更不会删除该记录。
- 停用仅将 `isActive` 置为 false；不删除 Habit 或历史记录。
- 历史页只查询 HabitRecord，不提供补签、编辑和删除入口。

## 7. 完成率口径
- 今日完成率 = 今日已完成 HabitRecord 数 / 今日已生成 HabitRecord 数。
- 累计固定习惯完成率 = 所有已生成 HabitRecord 中已完成记录数 / 所有已生成记录数。
- 不将临时任务、训练记录计入固定习惯统计。
- 没有记录时显示 0%，不发生除零。

## 8. 数据库版本和迁移
当前 Room 数据库版本为 2：
- v1：`app_metadata` 基础表。
- v2：保留 `app_metadata`，新增 `habits`、`habit_records` 及对应索引和外键。
- `MIGRATION_1_2` 显式创建新表，不使用 destructive migration。
- Schema 已导出并提交：`app/schemas/com.dailyfit.app.data.local.DailyFitDatabase/2.json`。
- Repository 测试验证 v1→v2 后原 metadata 数据保留。
- 后续数据库结构变更必须递增版本并提供 Migration；禁止破坏性迁移。

## 9. 页面与 ViewModel
- 首页今日习惯区：仅展示当天应执行记录，提供完成/取消完成。
- 首页固定习惯管理区：展示全部启用习惯，支持编辑/停用；即使某个自定义星期习惯当天不执行，也能继续管理。
- 已停用习惯单独展示，不参与每日生成。
- 历史页：按日期倒序显示名称快照、日期、完成状态及累计完成率；只读。
- 计划页：当前仍是后续阶段入口。
- `HabitViewModel` 通过 StateFlow 提供 dashboard 和历史数据；Screen 不直接操作 DAO。
- 依赖由 `DailyFitApplication` 提供的数据库与 Repository 初始化。

## 10. 测试策略与阶段 1 验证
- 频率规则：每天、工作日、自定义星期。
- Room / Repository：当天记录生成、重复生成幂等、编辑不覆盖名称快照、停用保留历史。
- 日期边界：日期切换后生成新的未完成记录；前一日完成状态保留；历史日期不能打卡或取消打卡。
- 管理边界：自定义习惯在非执行日仍可在固定习惯管理列表中编辑和停用。
- 统计：固定习惯完成率只基于 HabitRecord。
- 迁移：v1→v2 后原 metadata 数据保留。
- 当前阶段 CI：12 项单元测试全部通过，`assembleDebug` 成功。

## 11. 后续阶段设计基线
后续仍按单数据库、稳定 UUID、显式迁移原则逐步加入 TemporaryTask、训练计划、Exercise、WorkoutSession、WorkoutExercise 和 WorkoutSet。正式训练保存时复制动作名称和每组数据快照；正式历史只读保护需由 Repository API 和 SQLite 触发器双层实现。
