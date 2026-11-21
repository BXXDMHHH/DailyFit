# DailyFit 开发状态

> 最后更新：2026-11-21  
> 当前目标：V1.0  
> 当前阶段：阶段 0 — Android 工程基础

## 1. 仓库现状
仓库已从纯文档状态建立为真实 Android Gradle 项目。此前不存在可复用的 Android 源码，因此本次新建基础工程，没有覆盖或删除已有业务代码。

## 2. 阶段 0 已提交内容
- Android 单模块工程：根 Gradle 配置、:app 模块、Manifest 和 Kotlin 源码。
- Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel + StateFlow。
- 首页、计划、历史三个一级页面入口。
- Room 2.8.5 数据库基础类，包含轻量 app_metadata 基础表，schema 导出开启；业务实体将在对应阶段加入。
- 习惯频率纯函数：每日、工作日、自定义星期（ISO 周一至周日）。
- 习惯频率单元测试。
- GitHub Actions：单元测试和 Debug APK 构建工作流。
- README、AGENTS.md、docs/PRD.md。

## 3. 当前实现边界
首页、计划、历史是可导航的页面骨架，不代表习惯、临时任务、训练计划、动作库或训练记录已经完成。Room 目前仅包含基础元数据表，不冒充业务数据库；业务表将在相应阶段按迁移策略逐步加入。没有使用假数据冒充数据库功能。

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

## 5. 测试状态
| 检查项 | 状态 | 说明 |
|---|---|---|
| GitHub 文件写入与回读 | 已验证 | 已提交并回读 |
| Android 源码创建 | 已完成 | 项目结构与 Kotlin 源码已提交 |
| Gradle 依赖解析 | 已通过 | CI 已完成依赖下载与 Android 资源处理 |
| Room KSP | 修复中 | 原空 entities 列表导致 KSP 失败；现已加入 app_metadata 实体 |
| Gradle 编译 | 待 CI | 等待本次修复后的 CI |
| 单元测试 | 待 CI | 已添加频率规则测试 |
| Debug APK | 待 CI | 等待单元测试通过 |
| 设备运行 | 待本地验收 | 尚未在用户 Android Studio / 模拟器运行 |

未执行不视为通过。未通过的检查不会标记为成功。

## 6. 风险与注意事项
1. 首次打开前需在 Android Studio SDK Manager 安装 Android SDK Platform 37。
2. 首次构建需要网络访问 Google/Maven 仓库。
3. 习惯、任务、训练和历史业务表、数据库迁移及历史只读保护尚未实现，需在后续阶段落地并测试。
4. 当前自动化 CI 在 GitHub 托管环境执行；本地设备运行仍需单独验收。

## 7. 下一阶段
**阶段 1：固定习惯。**
- Habit / HabitRecord Room Entity、DAO、Repository。
- DAILY / WEEKDAYS / CUSTOM 频率生成。
- 当天记录事务化、幂等创建；不自动补签。
- 打卡与历史查询。
- 前台恢复时检查本地业务日期变化。
- Repository、日期边界和 Room DAO 测试。

## 8. 阶段跟踪
- [x] 阶段 0：工程基础、主题、导航、Room 基础与 CI（构建验收待通过）
- [ ] 阶段 1：固定习惯
- [ ] 阶段 2：临时任务
- [ ] 阶段 3：训练计划与动作库
- [ ] 阶段 4：训练执行与中断恢复
- [ ] 阶段 5：训练历史永久只读
- [ ] 阶段 6：统计与体验整合
