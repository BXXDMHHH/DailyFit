# DailyFit 开发状态

> 最后更新：2026-11-22  
> 当前目标：V1.0  
> 当前阶段：阶段 4 — 训练执行与中断恢复

## 1. 仓库现状
Android Gradle 单模块工程，采用 Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel + StateFlow、Room、Coroutines。阶段 0 至阶段 3 已实现。阶段 3 GitHub Actions #44929615591 全部通过。

## 2. 已完成阶段

### 阶段 0：工程基础
- Android 单模块工程、Manifest、Gradle Wrapper、主题和三个一级导航入口。
- Room Schema 导出配置和 GitHub Actions。

### 阶段 1：日常习惯模块
- 固定习惯新增、修改、停用；DAILY / WEEKDAYS / CUSTOM。
- Room 实体、DAO、Repository、ViewModel + StateFlow。
- 每日任务仅生成当前日期记录，唯一索引保证幂等。
- 每日打卡、取消完成；历史日期不能补签。
- ON_RESUME 日期同步及前台午夜刷新。
- 历史记录保存名称快照；习惯修改不覆盖历史。
- 今日和累计固定习惯完成率；首页习惯管理。

### 阶段 2：临时任务模块
- 独立 TemporaryTaskEntity、TemporaryTaskDao、TemporaryTaskRepository。
- 新增、修改、删除、完成、取消完成；业务日期隔离。
- 首页临时任务卡片、今日和累计独立统计。
- Room v2→v3 显式迁移，不影响固定习惯数据。

### 阶段 3：训练计划与动作库
- 七天每周模板，默认上肢 A、下肢 A、有氧、上肢 B、下肢 B、有氧、恢复。
- 每次修改固定计划生成新版本，保存 effectiveFromDate；历史版本不覆盖。
- 首次查看某日时，根据当日生效模板生成主要训练安排快照，保存 sourceVersionId。
- 单日调整只更新对应日期的快照，不修改周模板；已经生成的日期安排不会被后续模板版本覆盖。
- 每天默认一份主要安排，支持新增、修改、删除额外训练。
- 训练安排状态支持 PLANNED / COMPLETED / SKIPPED；状态只作用于对应日期安排。
- 独立 Exercise 动作库，支持按名称或类别搜索、新增、修改、删除。
- 动作库与周模板、每日安排不使用级联外键耦合；动作库变化不会删除计划快照。
- 计划页已接入 PlanViewModel、StateFlow、Repository 和 Room。

## 3. 数据库版本与迁移
当前 Room 数据库版本为 4：
- v1：app_metadata。
- v2：habits、habit_records。
- v3：temporary_tasks。
- v4：weekly_workout_plan_versions、weekly_workout_plan_days、daily_workout_plans、exercises。
- 显式 MIGRATION_1_2、MIGRATION_2_3、MIGRATION_3_4；不使用 destructive migration。
- 已提交 v2、v3、v4 Room Schema。

## 4. 关键业务规则
- 设备当前时区 LocalDate 为业务日期，数据库保存 ISO yyyy-MM-dd。
- 周模板通过 effectiveFromDate 选取版本，旧版本永久保留。
- 单日安排是独立数据快照，调整不回写周模板。
- 每日主要安排按需生成；额外训练单独存储。
- 状态仅有 PLANNED、COMPLETED、SKIPPED，不自动影响下一周模板。
- 动作库独立管理。训练历史模块尚未实现；后续正式历史必须复制动作名称和组数据快照，不能依赖动作库实时数据。

## 5. 阶段 3 CI 最终验收
构建记录：[GitHub Actions #44929615591](https://github.com/BXXDMHHH/DailyFit/actions/runs/44929615591)

| 检查项 | 结果 |
|---|---|
| Room Schema 生成 | 通过，生成 v4 Schema |
| Kotlin / KSP 编译 | 通过 |
| 单元测试 | 29 项全部通过，0 失败 |
| v1→v4 / v2→v4 / v3→v4 迁移链 | 测试通过 |
| Debug APK | assembleDebug 成功，Artifact 已上传 |

测试明细：习惯 Repository 9 项、习惯频率规则 3 项、临时任务 Repository 8 项、训练计划 Repository 9 项。

## 6. 已知限制
- 训练计划当前记录训练安排名称、备注和状态；本阶段尚未把动作库动作编排成计划内动作清单。
- 训练执行、组数据、自动保存与进程死亡恢复尚未实现。
- 正式训练历史尚未实现；永久只读和历史动作快照将在后续训练历史阶段落实。
- 真机/模拟器交互仍需用户本地验收。

## 7. 下一阶段：训练执行与中断恢复
- 从首页/计划进入当天训练。
- 训练动作、目标组数、目标重量/次数与每组实际重量/次数分开保存。
- 添加动作、临时动作、添加/删除训练组。
- 训练中自动保存，进程退出后恢复未完成训练。
- 完成后先生成正式只读历史快照（正式历史规则需与 PRD 保持一致）。

## 8. 阶段跟踪
- [x] 阶段 0：工程基础与 CI
- [x] 阶段 1：日常习惯模块
- [x] 阶段 2：临时任务模块
- [x] 阶段 3：训练计划与动作库（29 项测试通过，Debug APK 构建通过）
- [ ] 阶段 4：训练执行与中断恢复
- [ ] 阶段 5：训练历史永久只读
- [ ] 阶段 6：统计与体验整合
