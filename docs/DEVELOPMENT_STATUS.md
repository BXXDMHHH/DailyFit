# DailyFit 开发状态

> 最后更新：2026-11-22  
> 当前目标：V1.0  
> 当前阶段：阶段 3 — 训练计划模块

## 1. 仓库现状
Android Gradle 单模块工程，采用 Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel + StateFlow、Room、Coroutines。阶段 0、阶段 1 和阶段 2 已实现；阶段 2 GitHub Actions #44926740181 全部通过。

## 2. 已完成阶段

### 阶段 0：工程基础
- Android 单模块工程、Manifest、Gradle Wrapper、主题和三个一级导航入口。
- Room v1 元数据表、Schema 导出配置和 GitHub Actions。

### 阶段 1：日常习惯模块
- 固定习惯新增、修改、停用；DAILY / WEEKDAYS / CUSTOM。
- Room 实体、DAO、Repository、ViewModel + StateFlow。
- 每日任务仅生成当前日期记录，唯一索引保证幂等。
- 每日打卡、取消完成；历史日期不能补签。
- ON_RESUME 日期同步及前台午夜刷新。
- 历史记录保存名称快照；习惯修改不覆盖历史。
- 今日和累计固定习惯完成率；首页习惯管理。

### 阶段 2：临时任务模块
- 新增独立 TemporaryTaskEntity、TemporaryTaskDao、TemporaryTaskRepository。
- 任务使用 UUID，保存创建日期、标题、完成状态和时间戳。
- 支持新增、修改、删除、完成、取消完成。
- 编辑、删除和完成 SQL 均限定 businessDate = today；旧日期任务不可修改。
- 首页临时任务卡片展示今日任务、今日完成率和独立累计统计。
- 日期切换只查询新日期任务；旧任务保留在数据库供累计统计，不会延续到第二天。
- 临时任务与 Habit / HabitRecord 使用独立表、DAO、Repository 和统计口径。
- Room v2→v3 显式迁移新增任务表和索引，保留习惯、打卡和 metadata 数据。
- ViewModel 合并首页状态，复用现有日期同步机制。

## 3. 关键业务规则
- 设备当前时区的 LocalDate 是业务日期，数据库保存 ISO 日期字符串。
- 固定习惯每日生成记录；临时任务不自动生成、不自动延续。
- 临时任务创建时固定写入当天日期；只在当天列表展示。
- 临时任务历史行保留用于累计统计；当天以外不能修改、完成或删除。
- 固定习惯完成率只统计 HabitRecord；临时任务完成率只统计 TemporaryTask。
- 训练历史正式保存后永久只读、禁止修改和删除（训练模块尚未实现）。

## 4. 依赖基线
- AGP 9.4.0；Gradle 9.6.0；Kotlin / Compose compiler 2.4.10；KSP 2.3.12。
- Compose BOM 2026.09.00；Navigation Compose 2.10.1；Lifecycle 2.11.0；Room 2.8.5。
- compileSdk / targetSdk 37；minSdk 26；JDK 17。
- JUnit 4、Robolectric 4.16.1、Room Testing 2.8.5。

## 5. 阶段 2 测试范围
新增 TemporaryTaskRepositoryTest 覆盖创建、UUID、创建日期、完成/取消、修改、删除、空标题、跨日隔离、历史保留、独立统计和 v2→v3 迁移。同步更新阶段 1 v1→v3 迁移测试，继续验证习惯模块迁移兼容性。

## 6. 已知限制
- 首页临时任务卡片提供当天管理和累计统计；暂未单独制作临时任务历史页面。
- 临时任务只允许在创建当天编辑和删除；跨日后作为历史统计数据保留。
- 真机/模拟器交互仍需用户本地验收。

## 7. 下一阶段：训练计划模块
- 每周固定训练模板。
- 单日训练安排覆盖，不影响每周模板。
- 训练安排完成、跳过状态。
- 显式 Room Migration、Repository 业务测试和计划页 UI。

## 8. 阶段跟踪
- [x] 阶段 0：工程基础与 CI
- [x] 阶段 1：日常习惯模块
- [x] 阶段 2：临时任务模块（20 项单元测试通过，Debug APK 构建通过）
- [ ] 阶段 3：训练计划
- [ ] 阶段 4：动作库
- [ ] 阶段 5：训练执行与中断恢复
- [ ] 阶段 6：训练历史永久只读
- [ ] 阶段 7：统计与体验整合


## 9. 阶段 2 CI 最终验收

<badge color="success">全部通过</badge>

构建记录：[GitHub Actions #44926740181](https://github.com/BXXDMHHH/DailyFit/actions/runs/44926740181)

| 检查项 | 结果 | 详情 |
|---|---|---|
| Room Schema 生成 | 通过 | v3 Schema 已生成并归档 |
| Kotlin 主源码编译 | 通过 | KSP / Kotlin 编译流程成功 |
| 习惯 Repository / Room 测试 | 通过 | 9 项，0 失败 |
| 习惯频率规则测试 | 通过 | 3 项，0 失败 |
| 临时任务 Repository / Room 测试 | 通过 | 8 项，0 失败 |
| 单元测试合计 | 通过 | 20 项，0 失败 |
| Debug APK | 通过 | assembleDebug 成功，Artifact 已上传 |
| 数据库迁移 | 通过 | v1→v2→v3 和 v2→v3 测试通过 |

真机/模拟器 UI 与生命周期验收仍待用户本地完成。
