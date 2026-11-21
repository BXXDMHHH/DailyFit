# DailyFit 开发状态

> 最后更新：2026-11-21  
> 当前目标：V1.0  
> 当前阶段：阶段 1 — 日常习惯模块（已提交，CI 验证中）

## 1. 仓库现状
仓库已包含可构建的 Android Gradle 单模块工程。阶段 0 的 GitHub Actions 构建已通过（commit `0b5d447`）。现有工程采用 Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel + StateFlow、Room、Coroutines。

## 2. 阶段 0 基础
- Android 单模块工程、Manifest、Gradle Wrapper、主题和三个一级导航入口。
- Room v1 元数据表及导出 Schema 配置。
- GitHub Actions 单元测试和 Debug APK 构建流程。
- 阶段 0 CI：成功；设备运行仍需 Android Studio / 模拟器验收。

## 3. 阶段 1 本次交付
- 固定习惯 Room 实体：`HabitEntity`、`HabitRecordEntity`。
- DAO：习惯定义查询/新增/修改/停用；每日记录查询、幂等插入和当天完成状态更新。
- 数据库从 v1 升级至 v2，显式 `MIGRATION_1_2` 保留原 `app_metadata` 数据。
- Repository：按 DAILY / WEEKDAYS / CUSTOM 规则，仅生成当前本地日期的任务；唯一索引保证同习惯同日只生成一条。
- 每日记录保存习惯名称快照；修改习惯定义不改写已有记录；停用不删除历史。
- 仅允许修改当天记录完成状态，历史日期记录只读；取消完成会清空 completedAt。
- ViewModel + StateFlow：新增、编辑、停用、打卡、取消打卡、日期刷新；前台 ON_RESUME 同步，并安排应用运行期间的本地午夜刷新。
- 首页习惯卡片、频率编辑对话框、停用确认和今日完成率。
- 历史页展示每日打卡快照及固定习惯累计完成率。
- 测试覆盖频率规则、幂等生成、编辑快照、停用保留历史、历史不可补签/修改、完成率和 v1→v2 迁移数据保留。

## 4. 业务边界与规则
- 日期按设备当前时区的 `LocalDate` 计算，数据库保存 ISO 日期字符串。
- 只生成当前日期，不遍历补建昨天或更早日期；昨天未完成仍为未完成。
- 修改名称、频率只影响新生成的未来记录；已有记录的名称快照和完成状态保持不变。
- 停用是软停用，不物理删除 Habit，也不删除 HabitRecord。
- 固定习惯完成率仅统计 HabitRecord，不混入临时任务或训练数据。
- 当前阶段不包含临时任务、训练计划、动作库和训练执行功能。

## 5. 依赖基线
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

## 6. 测试状态
| 检查项 | 状态 | 说明 |
|---|---|---|
| 阶段 0 CI | 已通过 | commit `0b5d447` |
| 阶段 1 源码提交 | 已完成 | 等待新一轮 CI |
| Room v1→v2 迁移 | 已实现并添加测试 | 需 CI 实际执行确认 |
| Repository 业务测试 | 已添加 | 等待 CI |
| Kotlin 编译 | 待 CI | 不提前标记通过 |
| 单元测试 | 待 CI | 不提前标记通过 |
| Debug APK | 待 CI | 不提前标记通过 |
| 真机/模拟器验收 | 待本地验收 | 当前未连接用户设备 |

## 7. 已知限制
1. Habit 目前只支持停用，不提供物理删除或重新启用，符合本阶段需求边界。
2. 历史打卡只记录 App 曾经为某个日期生成的任务；若某天完全没有打开 App，不会事后补造那天的记录。
3. 午夜刷新由前台进程内延迟任务完成；进程被系统终止后，重新启动或回到前台时会重新同步。
4. Room schema 由 KSP 在构建时导出；CI 通过后应将生成的 v2 schema 纳入版本控制，以便后续迁移测试。

## 8. 下一阶段
**阶段 2：临时任务模块。**
- TemporaryTask Entity / DAO / Repository。
- 当日新增、修改、删除、完成、取消完成。
- 日期切换后仅显示当天任务，历史数据保留。
- 临时任务独立统计，不进入固定习惯完成率。
- 首页 UI 与 Room / Repository 测试。

## 9. 阶段跟踪
- [x] 阶段 0：工程基础与 CI
- [x] 阶段 1：日常习惯模块（源码已提交，构建验收中）
- [ ] 阶段 2：临时任务
- [ ] 阶段 3：训练计划与动作库
- [ ] 阶段 4：训练执行与中断恢复
- [ ] 阶段 5：训练历史永久只读
- [ ] 阶段 6：统计与体验整合
