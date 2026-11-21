# DailyFit 开发状态

> 最后更新：2026-11-22
> 当前目标：V1.0
> 当前阶段：阶段 4 — 训练执行与历史记录

## 已完成阶段
- 阶段 0：Android 工程基础与 CI。
- 阶段 1：固定习惯管理、频率、打卡、历史与统计。
- 阶段 2：独立临时任务及统计。
- 阶段 3：周计划版本、单日快照、额外安排、动作库。
- 阶段 3 CI：29 项单元测试通过，Debug APK 构建成功。

## 阶段 4 本次实现
- 新增 WorkoutSession、WorkoutSessionExercise、WorkoutSessionSet 进行态表。
- 训练过程数据增量写入 Room；进程退出后通过 IN_PROGRESS session 查询恢复。
- 每个训练动作保存名称、分类、器械、备注快照；临时动作不依赖动作库。
- 目标组数/次数/重量与实际重量/次数分开保存；逐组完成状态独立保存。
- 支持训练动作增删、训练组增删、编辑未完成组数据。
- 支持 COMPLETED、INCOMPLETE、SKIPPED 三种正式历史状态。
- 结束时在 Room transaction 内生成 WorkoutHistory、HistoryExercise、HistorySet 快照。
- 历史三张表建立 SQLite BEFORE UPDATE / BEFORE DELETE 触发器，数据库层阻止篡改与删除；历史 DAO 不提供 update/delete API。
- 首页训练入口接入执行页；今日训练安排可启动；进行中训练可恢复。
- 历史页新增训练记录列表和详情只读弹窗。
- Room v4→v5 显式迁移，只新增训练表、索引和不可变触发器。

## 阶段 4 本轮补齐内容
- 新增 WorkoutPlanExerciseEntity，支持周模板日与每日训练计划分别关联动作、目标组数、目标次数、目标重量。
- 周模板创建新版本时按星期复制动作配置；每日主要计划生成时复制对应周模板动作快照。
- 单日动作调整只更新 DAILY_PLAN 关联，不回写周模板；已生成的每日快照不被后续周模板版本覆盖。
- 开始训练时将每日计划动作和目标数据复制到 WorkoutSessionExercise / WorkoutSessionSet，训练页面显示计划动作并记录每组实际数据。
- 修复 WorkoutScreen Compose 作用域语法错误；将训练执行页接入 Navigation Compose，首页入口可启动训练，训练中隐藏底部导航。
- 历史页改为可滚动的训练记录列表与只读详情。
- 正式历史不可变触发器同时覆盖数据库迁移和全新安装；测试数据库也安装相同触发器。
- Room v5→v6 显式迁移新增计划动作关联表和索引。
- 新增计划动作从周模板到每日快照、训练会话、正式历史的 Repository 测试。
- CI 与真机验收结果待本轮最终验证，不提前宣告完成。

## 数据安全
- Room 目标版本 v6；v4→v5、v5→v6 均为显式迁移，不使用 destructive migration。
- 进行中数据可修改；正式历史无修改/删除 API，并由 SQLite 触发器阻止 update/delete。
- 历史动作名称及每组实际数据均复制快照，动作库后续修改/删除不影响历史。

## 下一步
1. 检查本轮 GitHub Actions：Room Schema、单元测试、Debug APK 必须全部通过。
2. 真机验证训练中强制结束 App 后重新启动恢复、组数据保存、正式历史不可修改删除。
3. 真机验收完成后再标记 V1.0 正式验收。
