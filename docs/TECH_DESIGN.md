# DailyFit 技术设计

> 版本：V1.0  
> 状态：开发前技术基线  
> 依据：用户确认的 DailyFit V1.0 PRD（当前会话提供的 `fit.md`）。  
> 说明：本仓库当前尚无 Android 工程源码；本文是待实施设计，不代表已有代码已经实现。

## 1. 设计目标与边界

- 使用 Kotlin、Jetpack Compose、Material 3、MVVM、ViewModel + StateFlow、Room、Coroutines、Navigation Compose。
- 本地优先，Room 是业务数据唯一可信来源；V1.0 不引入账号、服务端、云同步。
- 优先采用单 Activity、单 Room 数据库、DAO + Repository，不为每个功能额外引入复杂 UseCase 层。
- 所有业务实体使用稳定 UUID，不使用列表下标作为标识。
- 正式训练历史保存后永久只读、禁止修改和删除；训练草稿在正式保存前可编辑、可放弃。
- 日期业务使用设备当前本地日期，数据库日期字段统一保存 ISO-8601 日期字符串（yyyy-MM-dd）；时间戳保存 epoch millis。

## 2. 当前仓库审计结论

审计分支：`main`。

当前仓库仅包含根目录 `GITHUB_WRITE_TEST.md`。未发现：
- Android Studio / Gradle 工程文件（settings.gradle、build.gradle、gradlew 等）
- `app/src` Kotlin 源码、Compose 页面、Manifest 或资源
- Room Entity、DAO、Database、Repository、ViewModel
- `docs/PRD.md`、`docs/TECH_DESIGN.md`、`docs/DEVELOPMENT_STATUS.md` 或 `AGENTS.md`

因此目前没有可复用的 Android 代码、依赖版本或现有数据库版本可供核验。不能据此声称已有功能、架构或测试通过。当前只能以已确认 PRD 为依据建立技术基线；待 Android 工程上传后，再做逐文件复核并将设计映射到现有代码，避免无关重构。

## 3. 推荐模块与职责

建议按业务职责组织 package，初期保持单 app module：

```text
app/
  data/
    local/
      DailyFitDatabase
      dao/
      entity/
      converter/
    repository/
      *RepositoryImpl
  domain/
    model/                 # 仅放跨层需要的简单模型/枚举
    repository/            # Repository 接口
  feature/
    home/
    habits/
    tasks/
    plans/
    exercises/
    workout/
    history/
  navigation/
  ui/
    theme/
    components/
```

若现有工程已经采用不同但清晰的目录结构，优先沿用，不为匹配目录树而搬迁文件。

### 3.1 分层边界

| 层 | 职责 | 不负责 |
|---|---|---|
| Compose Screen | 展示 StateFlow 状态、收集用户事件、导航 | 直接访问 DAO / 处理数据库事务 |
| ViewModel | 调用 Repository、校验界面输入、组合 UI State、发出一次性提示事件 | 保存业务数据副本、直接写 SQL |
| Repository | 业务操作入口、日期规则、跨 DAO 事务、草稿恢复与正式保存 | UI 排版 |
| DAO | Room 查询、插入、更新、受控删除 | 复杂 UI 业务规则 |
| Room Database | 表、外键、索引、迁移、只读保护触发器 | 页面状态 |

Repository 接口放在 domain 层，Room 实现放在 data 层。ViewModel 通过构造参数接收 Repository。若当前项目没有依赖注入框架，先用简单的 AppContainer/Provider 提供依赖；只有现有工程已采用 Hilt/Koin 时才沿用，不为此单独增加依赖。

### 3.2 页面与 ViewModel

- Home：今日固定习惯、临时任务、训练安排、各模块进度；聚合读取，不直接拥有业务数据。
- Plan：固定习惯管理、每周模板、单日计划调整、动作库入口。
- History：习惯历史、正式训练历史和基础统计，只提供查询能力。
- HabitViewModel / TaskViewModel / PlanViewModel / ExerciseViewModel / WorkoutViewModel / HistoryViewModel：按功能划分；若页面状态确实很小，可合并相邻 ViewModel，避免过度拆分。
- 导航使用 Navigation Compose；Screen 不持有跨页面长期业务状态，跨页面数据通过稳定 ID 传递，再由目标 ViewModel 从 Repository 读取。

## 4. Room 数据库设计

建议 V1.0 使用一个 `DailyFitDatabase`，初始数据库版本设为 1。实体字段以实现时的 Kotlin 类型为准；下表定义业务字段和关系。所有主键均为 UUID String，创建/更新时间为 Long（epoch millis），日期为 yyyy-MM-dd String。

### 4.1 表与字段

#### Habit（固定习惯定义）
- `id` TEXT PRIMARY KEY
- `name` TEXT NOT NULL
- `frequencyType` TEXT NOT NULL：DAILY / WEEKDAYS / CUSTOM
- `weekDaysMask` INTEGER NOT NULL：周一至周日 bit mask；仅 CUSTOM 使用
- `isActive` INTEGER NOT NULL
- `createdAt`, `updatedAt` INTEGER NOT NULL

编辑习惯定义只影响未来生成的打卡记录；停用不删除定义和历史记录。V1.0 不提供物理删除习惯的必要性。

#### HabitRecord（每日固定习惯记录）
- `id` TEXT PRIMARY KEY
- `habitId` TEXT NOT NULL，FK -> Habit.id（建议 RESTRICT）
- `businessDate` TEXT NOT NULL
- `habitNameSnapshot` TEXT NOT NULL
- `isCompleted` INTEGER NOT NULL DEFAULT 0
- `completedAt` INTEGER NULL
- `createdAt` INTEGER NOT NULL

每个习惯每天最多一条记录：UNIQUE(`habitId`, `businessDate`)。名称快照保证习惯改名后历史显示不变。只为当天应执行且处于启用状态的习惯生成记录。

#### TemporaryTask（当日临时任务）
- `id` TEXT PRIMARY KEY
- `businessDate` TEXT NOT NULL
- `title` TEXT NOT NULL
- `note` TEXT NOT NULL DEFAULT ''
- `isCompleted` INTEGER NOT NULL DEFAULT 0
- `createdAt`, `updatedAt` INTEGER NOT NULL
- `completedAt` INTEGER NULL

临时任务仅属于创建日期，首页按当天日期查询；不参与固定习惯完成率。按 PRD 支持新增、修改、删除、完成和取消完成。

#### WeeklyWorkoutTemplate（每周模板版本）
- `id` TEXT PRIMARY KEY
- `weekday` INTEGER NOT NULL：1–7（周一至周日）
- `title` TEXT NOT NULL
- `description` TEXT NOT NULL DEFAULT ''
- `effectiveFrom` TEXT NOT NULL
- `effectiveTo` TEXT NULL（左闭右开版本区间的结束日）
- `createdAt` INTEGER NOT NULL

索引：(`weekday`, `effectiveFrom`)、`effectiveTo`。更新长期模板时，在同一事务中关闭旧版本并写入新版本，不能覆盖已生效的旧版本。

#### DailyWorkoutPlan（具体日期的训练安排）
- `id` TEXT PRIMARY KEY
- `businessDate` TEXT NOT NULL
- `planType` TEXT NOT NULL：MAIN / EXTRA
- `sourceTemplateId` TEXT NULL，关联 WeeklyWorkoutTemplate.id（可选 FK，建议 SET NULL）
- `titleSnapshot` TEXT NOT NULL
- `descriptionSnapshot` TEXT NOT NULL DEFAULT ''
- `status` TEXT NOT NULL：PLANNED / COMPLETED / NOT_COMPLETED / SKIPPED
- `isOverride` INTEGER NOT NULL DEFAULT 0
- `createdAt`, `updatedAt` INTEGER NOT NULL

索引：(`businessDate`, `planType`)；UNIQUE(`businessDate`, `planType`) 保证每天最多一份 MAIN 和一份 EXTRA（如未来确认需要多份额外训练，再通过迁移调整约束）。创建每日安排时复制模板标题和描述快照。单日调整只更新该日记录，不回写周模板。模板变更不得改写已经生成的日期安排。

#### Exercise（独立动作库）
- `id` TEXT PRIMARY KEY
- `name` TEXT NOT NULL
- `description` TEXT NOT NULL DEFAULT ''
- `createdAt`, `updatedAt` INTEGER NOT NULL

索引：`name`。V1.0 先使用 Room LIKE 查询完成本地搜索，不引入 FTS。删除动作时允许物理删除动作定义，但训练中的引用通过 SET NULL 或不强制外键保留快照；不得级联删除训练数据。

#### WorkoutSession（训练会话：进行中草稿或正式历史）
- `id` TEXT PRIMARY KEY
- `businessDate` TEXT NOT NULL
- `startedAt` INTEGER NOT NULL
- `updatedAt` INTEGER NOT NULL
- `status` TEXT NOT NULL：IN_PROGRESS / COMPLETED / ABANDONED
- `completedAt` INTEGER NULL
- `planId` TEXT NULL，关联 DailyWorkoutPlan.id（可选，删除计划时 SET NULL）

索引：(`businessDate`)、`status`、(`status`, `updatedAt`)。V1.0 约束为同一时间最多一条 IN_PROGRESS 会话，由 Repository 在事务中检查并创建/恢复；不要依赖进程内变量。

#### WorkoutExercise（会话内动作及快照）
- `id` TEXT PRIMARY KEY
- `sessionId` TEXT NOT NULL，FK -> WorkoutSession.id（ON DELETE CASCADE，仅草稿可放弃）
- `exerciseId` TEXT NULL，FK -> Exercise.id（ON DELETE SET NULL）
- `exerciseNameSnapshot` TEXT NOT NULL
- `position` INTEGER NOT NULL
- `targetSets` INTEGER NOT NULL
- `targetReps` INTEGER NULL
- `targetWeight` REAL NULL

索引：(`sessionId`, `position`)。动作名在添加到训练时即保存快照；动作库改名或删除不影响本表。

#### WorkoutSet（每组目标与实际数据）
- `id` TEXT PRIMARY KEY
- `workoutExerciseId` TEXT NOT NULL，FK -> WorkoutExercise.id（ON DELETE CASCADE）
- `setNumber` INTEGER NOT NULL
- `targetReps` INTEGER NULL
- `targetWeight` REAL NULL
- `actualReps` INTEGER NULL
- `actualWeight` REAL NULL
- `isCompleted` INTEGER NOT NULL DEFAULT 0
- `completedAt` INTEGER NULL

约束：UNIQUE(`workoutExerciseId`, `setNumber`)；索引 `workoutExerciseId`。目标值与实际值分列存储，禁止以目标值填充实际值。

### 4.2 关系概览

```text
Habit 1 ─── N HabitRecord
WeeklyWorkoutTemplate 1 ─── N DailyWorkoutPlan（通过 sourceTemplateId，可为空）
DailyWorkoutPlan 1 ─── N WorkoutSession（会话可无计划）
WorkoutSession 1 ─── N WorkoutExercise
WorkoutExercise 1 ─── N WorkoutSet
Exercise 1 ─── N WorkoutExercise（可选关联；快照独立存在）
```

训练历史的读取以 WorkoutSession + WorkoutExercise + WorkoutSet 为准；历史展示不得实时依赖 Exercise 表或 WeeklyWorkoutTemplate 表。

### 4.3 索引与约束汇总

- HabitRecord：UNIQUE(habitId, businessDate)，另建 businessDate 索引便于历史查询。
- TemporaryTask：businessDate 索引。
- WeeklyWorkoutTemplate：(weekday, effectiveFrom) 与 effectiveTo 索引。
- DailyWorkoutPlan：(businessDate, planType) 复合索引及唯一约束。
- Exercise：name 索引。
- WorkoutSession：businessDate、status、(status, updatedAt) 索引。
- WorkoutExercise：(sessionId, position) 索引。
- WorkoutSet：UNIQUE(workoutExerciseId, setNumber)。

避免为了少量本地数据添加冗余索引；如 EXPLAIN QUERY PLAN 证明有需要，再增加索引。

## 5. 正式训练历史的永久只读保护

只靠 UI 隐藏编辑/删除按钮不够。采用两层保护：

1. **Repository/API 约束**：HistoryRepository 只暴露 Flow 查询；不提供 update/delete 历史方法。训练写入入口只允许修改 IN_PROGRESS 会话。正式保存使用单个 Room transaction，将会话状态改为 COMPLETED 并写入 completedAt。
2. **SQLite 触发器保护**：在 RoomDatabase.Callback.onOpen 中创建幂等触发器，阻止 COMPLETED 会话及其 WorkoutExercise / WorkoutSet 子记录的 UPDATE、DELETE；也阻止向已完成会话插入新子记录。触发器抛出 ABORT，防止未来误加 DAO 写入口造成静默破坏。

完成保存的事务顺序：校验会话仍为 IN_PROGRESS -> 校验动作/组数据 -> 写入最终状态和完成时间 -> 同事务提交。事务提交后，历史只能查询。草稿放弃将状态改为 ABANDONED 或删除草稿及子项；禁止将 COMPLETED 会话转回其他状态。数据库触发器需要覆盖会话状态从 COMPLETED 改回去的 UPDATE 场景。

不要对正式历史执行级联删除；外键删除动作库记录只能 SET NULL，历史动作名和训练数据快照始终保留。Room 迁移和触发器创建必须纳入测试。

## 6. 关键业务规则落地

### 6.1 每日日期刷新与固定习惯

- 日期由注入的 Clock/DateProvider 根据设备当前时区计算，不在 ViewModel 创建时永久缓存。
- Home 进入前台（ON_RESUME）时重新获取 LocalDate；日期变化后重新触发当天数据加载。
- 不需要常驻后台服务或每分钟轮询；应用不在前台时无需刷新 UI。
- Repository 的 `ensureTodayHabitRecords(date)` 使用 Room transaction：查询当天应执行且启用的 Habit，按 DAILY / WEEKDAYS / CUSTOM 计算频率，插入缺失记录；依赖 UNIQUE(habitId, businessDate) 保证幂等。
- 只补建“当前日期”的应执行记录，不回填昨天或更早日期；昨天未完成记录保留原状态，不自动补签。
- 修改习惯定义只影响未来日期；HabitRecord 保存名称快照。停用习惯不删除过去记录。
- 自定义星期使用明确的 ISO 星期编号 1–7，避免 Calendar 周起始日差异。

### 6.2 临时任务

- 新增时写入当前 businessDate；列表始终按日期过滤。
- 日期变化后旧任务自然不出现在今日列表，但仍保留在历史数据中供统计/回顾。
- 临时任务独立统计，不进入 HabitRecord 查询或固定习惯完成率分母。

### 6.3 周模板与单日覆盖

- 生成某日计划时，根据 weekday + businessDate 选择有效模板版本，复制标题/描述到 DailyWorkoutPlan。
- 生成操作幂等；若当天计划已存在，不重新覆盖用户单日调整。
- 单日覆盖只更新 DailyWorkoutPlan 快照和 isOverride，不修改模板版本。
- 修改模板时采用版本生效日期；历史日期的 DailyWorkoutPlan 快照不回写。
- NOT_COMPLETED / SKIPPED 只作用于当天记录，不改变下一周模板。

### 6.4 训练中断恢复

- 每次动作、组数、重量、次数或完成状态变更后，及时通过 Room upsert 保存；避免只保存在 Compose remember 或 ViewModel 内存。
- App 启动/回到前台时查询 IN_PROGRESS 会话；找到后提示继续训练并加载其动作和组数据。
- 新建训练时先在数据库创建会话，再允许录入；所有编辑以 sessionId / workoutExerciseId / setId 稳定关联。
- 放弃训练只处理 IN_PROGRESS 会话，状态改为 ABANDONED 或删除草稿；不得影响 COMPLETED 历史。
- 完成训练在单一事务中固化历史；事务失败时仍保持 IN_PROGRESS，可重试，不产生半条正式历史。
- V1.0 默认单个活动训练会话，避免复杂的多会话并发恢复逻辑。

## 7. 数据库迁移与数据安全

- Room 开发时开启 `exportSchema = true`，将 schema JSON 纳入版本控制。
- 首次落地数据库使用 version 1；每次结构变更递增版本并提供明确 Migration。
- 禁止使用 destructive migration 作为正式回退策略，禁止为通过测试删除已有数据。
- 对历史只读触发器、唯一约束、外键行为和事务保存编写 Room/Robolectric 或 instrumentation 测试。
- 如果当前工程已有数据库和用户数据，必须先读取真实版本与 schema，再编写迁移；本设计不能替代针对现存数据的迁移评估。

## 8. 测试策略

优先覆盖业务边界，不以 UI 截图测试代替数据正确性。

- 频率：每日、工作日、自定义星期；跨日后新记录生成；昨天未完成不补签；重复进入不重复插入。
- 历史：改名/停用习惯不改历史快照；动作改名/删除不改变已保存训练历史。
- 临时任务：只在创建日期显示，统计与固定习惯隔离。
- 训练计划：模板版本生效日期、单日覆盖不反写模板、跳过/未完成不影响下周。
- 训练执行：目标/实际值独立；自动保存；进程重建后恢复；放弃草稿。
- 永久只读：完成后 update/delete 会话失败；子动作/组 insert/update/delete 失败；History 层无写入口；保存事务失败时不生成半成品。
- 数据库：唯一约束、外键行为、迁移前后数据完整性。

## 9. 依赖策略

目标技术栈只要求 Kotlin、Compose、Material 3、Lifecycle ViewModel、StateFlow、Coroutines、Room、Navigation Compose。实际版本应以 Android 工程创建时的稳定兼容版本和现有 Gradle Version Catalog 为准。当前仓库没有 Gradle 文件，故不虚构具体版本号；不为了本设计额外引入网络层、云同步、DI 框架、WorkManager、FTS 或多模块架构。

## 10. 实施顺序

按依赖关系分阶段实现，每阶段均可编译和验收：

1. **阶段 0：工程基线** — 建立/导入 Android 工程，确认 AGP、Kotlin、Compose、SDK、Gradle Wrapper；建立主题、Navigation Compose、Room 空数据库、测试基线与 CI/本地构建说明。
2. **阶段 1：固定习惯** — Habit/HabitRecord、频率计算、当天记录幂等生成、打卡、历史查询、跨日刷新与测试。
3. **阶段 2：临时任务** — TemporaryTask CRUD、当天过滤、独立统计与测试。
4. **阶段 3：训练计划与动作库** — 周模板版本、每日快照/覆盖、状态标记、Exercise 搜索与 CRUD。
5. **阶段 4：训练执行与恢复** — WorkoutSession 草稿、动作/组编辑、实际数据、自动保存、进程重建恢复、放弃训练。
6. **阶段 5：正式历史与只读保护** — 原子完成保存、动作快照、SQLite 触发器、只读 History 页面和完整性测试。
7. **阶段 6：统计与体验整合** — Home/Plan/History 整合、统计口径、鼓励弹窗、全量回归、性能与可用性验收。

> 原 PRD 的八个业务实施项在此合并为 6 个交付阶段（阶段 0–6，阶段 0 为工程准备），不改变功能范围；动作库和训练计划放在训练执行之前，确保训练过程可引用稳定动作数据。
