# DailyFit 开发状态

> 最后更新：2026-11-21  
> 当前版本目标：V1.0  
> 当前状态：技术设计已建立；Android 工程尚未导入。

## 1. 仓库与工程现状

仓库：`BXXDMHHH/DailyFit`  
默认分支：`main`

本次检查结果：
- 根目录目前只有 `GITHUB_WRITE_TEST.md`。
- 未发现 Android Studio / Gradle 工程、`app/src` 源码、Manifest、资源文件或测试代码。
- 未发现 `docs/PRD.md`、`docs/TECH_DESIGN.md`、`docs/DEVELOPMENT_STATUS.md`、`AGENTS.md`。
- 因为没有 Android 工程，无法核验现有依赖版本、MVVM 实现、Room schema、页面和既有功能，也无法运行 Gradle 编译或测试。
- 产品需求依据当前会话提供的 DailyFit V1.0 需求文件 `fit.md`；正式训练历史永久只读、禁止修改和删除是已确认规则。

## 2. 本次完成

- 完成 GitHub 默认分支和仓库根目录审计。
- 确认当前没有可复用的 Android 源码；没有进行虚构的代码检查或无关重构。
- 新建 `docs/TECH_DESIGN.md`，定义 Room 实体/关系/索引、MVVM 分层、数据快照、历史只读保护、日期刷新、训练中断恢复、迁移和测试策略。
- 建立后续阶段开发顺序。

## 3. 当前技术决策

- 单 Android app module；Kotlin + Compose + Material 3 + Navigation Compose。
- MVVM：Screen -> ViewModel -> Repository -> DAO -> Room。
- 单一 Room 数据库；稳定 UUID；日期使用 yyyy-MM-dd；时间戳使用 epoch millis。
- 固定习惯打卡记录按日期唯一，按当天日期幂等生成；不自动补签。
- 临时任务绑定创建日期，和固定习惯独立统计。
- 周训练模板按生效日期版本化；每日训练安排保存快照，单日覆盖不反写模板。
- 训练草稿保存在 Room，恢复依赖 IN_PROGRESS 会话而非内存状态。
- 正式训练历史保存动作名称快照；Repository 不暴露历史编辑/删除入口，并由 SQLite 触发器阻止已完成会话及其子记录被修改或删除。
- 禁止 destructive migration；数据库 schema 导出并纳入版本控制。
- 不在没有现有工程的情况下锁定虚构的 Gradle / Kotlin / Compose 版本。

## 4. 测试与构建状态

| 检查项 | 状态 | 说明 |
|---|---|---|
| GitHub 仓库结构审计 | 已完成 | 仅发现写入测试文件 |
| 技术设计文档 | 已完成 | 待随工程实现校准 |
| Gradle 编译 | 未执行 | 仓库没有 Gradle 工程 |
| 单元测试 | 未执行 | 尚无测试源码 |
| Room / Migration 测试 | 未执行 | 尚无数据库实现 |
| Android 集成测试 | 未执行 | 尚无 Android 工程 |

未执行不等于通过；当前没有可报告的构建或测试通过结果。

## 5. 遗留事项与风险

1. 需要将 Android Studio 项目源码推送到仓库，或在仓库内初始化工程。
2. 需要补齐/导入 `docs/PRD.md` 与 `AGENTS.md`；当前会话提供的 `fit.md` 是需求依据，但仓库内尚无正式 PRD 路径。
3. Room 的实际版本、历史数据迁移策略必须在看到真实工程和 schema 后确认。
4. SQLite 触发器需要结合实际 Room entity/table 命名和 Migration 落地，并测试级联删除边界。
5. 当前尚无可运行 APK，不能进行设备验收。

## 6. 下一阶段建议

**下一阶段：阶段 0 — Android 工程基线。**

- 导入或创建完整 Android Studio 工程。
- 保留现有可用代码和版本目录；确认 Gradle Wrapper、AGP、Kotlin、Compose BOM、SDK 与测试依赖。
- 建立 app module、Material 3 主题、Navigation Compose 页面骨架。
- 建立 Room Database 空壳、schema 导出和测试运行基线。
- 完成一次 Gradle 编译与基础测试后，再进入阶段 1（固定习惯）。

## 7. 阶段跟踪

- [x] 技术方案与数据库设计基线
- [ ] 阶段 0：工程基础与页面框架
- [ ] 阶段 1：固定习惯
- [ ] 阶段 2：临时任务
- [ ] 阶段 3：训练计划与动作库
- [ ] 阶段 4：训练执行与中断恢复
- [ ] 阶段 5：训练历史永久只读
- [ ] 阶段 6：统计与体验整合
