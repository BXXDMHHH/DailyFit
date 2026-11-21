# DailyFit 开发状态

> 最后更新：2026-11-21  
> 当前目标：V1.0  
> 当前阶段：阶段 2 — 临时任务模块

## 1. 仓库现状
仓库已包含可构建的 Android Gradle 单模块工程。采用 Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel + StateFlow、Room、Coroutines。阶段 0 工程基线与阶段 1 日常习惯模块已完成 CI 验收。

## 2. 已完成阶段

### 阶段 0：工程基础
- Android 单模块工程、Manifest、Gradle Wrapper、主题和三个一级导航入口。
- Room v1 元数据表、Schema 导出配置。
- GitHub Actions 单元测试和 Debug APK 构建流程。
- CI 构建通过。

### 阶段 1：日常习惯模块
- 固定习惯新增、修改、停用。
- DAILY / WEEKDAYS / CUSTOM 三种频率，支持自定义 ISO 星期。
- Room 实体、DAO、Repository、ViewModel + StateFlow。
- 每日任务只按当前本地日期生成，唯一索引保证幂等。
- 每日打卡、取消完成；只能更改当天记录。
- 前台 ON_RESUME 日期同步；前台持续运行时安排本地午夜刷新。
- 历史打卡按日期倒序展示，名称使用生成时快照。
- 今日完成率与累计固定习惯完成率。
- 首页习惯管理 UI，历史页只读展示。
- Room v1→v2 显式 Migration，保留原 app_metadata 数据。
- Room v2 Schema 已纳入版本控制：`app/schemas/com.dailyfit.app.data.local.DailyFitDatabase/2.json`。

## 3. 关键业务规则
- 日期按设备当前时区的 `LocalDate` 计算，数据库保存 ISO 日期字符串。
- 只生成当前日期，不遍历补建昨天或更早日期；昨天未完成仍为未完成。
- 修改名称、频率只更新 Habit 定义，不回写任何已生成的 HabitRecord。
- 每条记录保存 `habitNameSnapshot`；编辑后历史仍显示原名称。
- 停用是软停用，不物理删除 Habit，也不删除 HabitRecord。
- 打卡更新 SQL 限定当前日期；历史日期记录不能补签、不能取消完成。
- 固定习惯完成率只统计 HabitRecord，不混入临时任务或训练数据。
- 如果某天 App 完全没有打开，不会事后补造那天的记录。

## 4. 依赖基线
- Android Gradle Plugin：9.4.0
- Gradle：9.6.0
- Kotlin / Compose compiler plugin：2.4.10
- KSP：2.3.12
- Compose BOM：2026.09.00
- Navigation Compose：2.10.1
- Lifecycle：2.11.0
- Room：2.8.5
- compileSdk / targetSdk：37；minSdk：26
- JDK：17
- 测试：JUnit 4、Robolectric 4.16.1、Room Testing 2.8.5

## 5. 阶段 1 验收结果

<badge>GitHub Actions run 44921749410</badge>

| 检查项 | 结果 | 详情 |
|---|---|---|
| Room Schema 生成 | 通过 | v2 JSON 已导出并纳入版本控制 |
| Kotlin 主源码编译 | 通过 | `compileDebugKotlin` |
| Repository / Room 测试 | 通过 | 8 项，0 失败 |
| 频率规则测试 | 通过 | 3 项，0 失败 |
| 单元测试合计 | 通过 | 11 项，0 失败 |
| Debug APK | 通过 | `assembleDebug` 成功，已上传 CI Artifact |
| 数据库迁移 | 通过 | v1→v2，原 metadata 数据保留 |
| 真机/模拟器验收 | 待用户本地验收 | 当前未连接用户设备 |

构建与测试结果：[GitHub Actions #阶段 1](https://github.com/BXXDMHHH/DailyFit/actions/runs/44921749410)

## 6. 已知限制
1. Habit 目前只支持停用，不提供物理删除或重新启用。
2. 午夜刷新由前台进程内延迟任务完成；进程被系统终止后，重新启动或回到前台时会重新同步。
3. 尚未在用户实际 Android 设备上完成 UI / 生命周期验收。
4. 当前首页的临时任务和训练区域仍是后续阶段占位内容。

## 7. 下一阶段：临时任务模块
- TemporaryTask Entity / DAO / Repository。
- 当日新增、修改、删除、完成、取消完成。
- 日期切换后仅显示当天任务，历史数据保留。
- 临时任务独立统计，不进入固定习惯完成率。
- 首页 UI 与 Room / Repository 测试。

## 8. 阶段跟踪
- [x] 阶段 0：工程基础与 CI
- [x] 阶段 1：日常习惯模块（CI 全部通过）
- [ ] 阶段 2：临时任务
- [ ] 阶段 3：训练计划与动作库
- [ ] 阶段 4：训练执行与中断恢复
- [ ] 阶段 5：训练历史永久只读
- [ ] 阶段 6：统计与体验整合
