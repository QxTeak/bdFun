# AGENTS.md — bdFun 开发与重构指南

> **这份文件是给谁看的**：任何在本仓库工作的 AI 编码助手（Codex / Claude / OpenCode / Cursor 等），以及新加入的人类协作者。
> **它解决什么问题**：AI 每次都是"失忆上岗"——不知道项目现状、不知道哪里不能碰、不知道团队约定的写法。这份文件就是它的**入职手册**。
> **怎么维护**：架构或约定变了，先改这份文件，再改代码。文件末尾有变更记录。

---

## 0. 三条铁律（如果只读一段，读这里）

1. **不许破坏现有体验。** 任何改动必须满足：能编译、能装机、能回滚。不确定就问，不要"顺手优化"。
2. **新代码必须分层；旧代码按 `docs/MIGRATION_TODO.md` 逐步搬，不在旧代码上继续加深耦合。**
3. **B 站接口的字段含义不许猜。** 拿不准就看真实返回、看 `docs/` 里的对齐文档，或问人。猜错字段名的代价是线上白屏。

---

## 1. 项目是什么

- **bdFun**：第三方哔哩哔哩 Android 客户端（非官方，仅学习研究）。
- **包名**：`com.aritxonly.bdfun`（debug 构建会自动加 `.debug` 后缀，两者可共存）。
- **技术栈**：Kotlin 2.4.21 + Jetpack Compose 1.12.2 + Miuix（HyperOS 风格组件库）+ OkHttp + Media3/ExoPlayer + Coil。
- **系统要求**：`minSdk 33` / `compileSdk 37` / JDK 21。
- **界面语言**：简体中文，文案直接写在 Kotlin 里（**历史现状，暂不引入 `strings.xml`**，见 §5）。

### 构建命令

```bash
# Windows
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
gradlew.bat :danmaku-engine:test

# Linux / macOS
./gradlew :app:assembleDebug
```

**构建前必须知道的三件事**：

1. 本项目用**本地 Miuix 源码**参与 composite build。源码放在 `../miuix-src/miuix-45645854cd46c32275c5666e3aafa9980d8f104e`，或用环境变量 `MIUIX_SOURCE` 指定。也可运行 `python3 tools/setup-miuix.py` 准备 `.local/` 下的固定快照；找不到时回退到 Maven 依赖，但**版本必须对齐**（AGP 9.5.1 不能和别的 AGP 混用）。
2. 每次构建会把时间戳写进 `BuildConfig.BUILD_STAMP` 和 `versionName`，所以**每次构建产物的版本号都不同**，这是故意的，别当 bug 修。
3. 可以用 `-Ppkg=com.example.app` 覆盖包名做实验，不加时 debug 包名是 `com.aritxonly.bdfun.debug`。

---

## 2. 目录地图（当前）

| 位置 | 内容 | 状态 |
|---|---|---|
| `app/src/main/kotlin/com/aritxonly/bdfun/` | 20 个 Activity | 生命周期、旋转、画中画、媒体键、下载服务 |
| `.../bdfun/ui/` | 76 个 Compose 文件 | 界面 + 大量业务逻辑（待拆） |
| `.../bdfun/data/` | 20 个文件 | `BiliApi`(3758 行)、`SettingsStore`(888 行)、模型、缓存 |
| `.../bdfun/AppGraph.kt` | 进程级单例 + 页面间传参 | 待替换为 `AppContainer` |
| `.../bdfun/App.kt` | `BdFunApp` 主壳 + 底栏 | 523 行，待拆 |
| `.../bdfun/ui/PlayerUiState.kt` | `VideoPageBus` / `PlayerUiState` / `ReplyTarget` | 全局 Bus，迁移期保留 |
| `.../top/yukonga/miuix/kmp/effect/bg/` | 背景特效（随项目内联的 miuix 片段） | 纯 UI，与业务无关 |
| `TeakUI/` | 设计系统模块（玻璃材质、页面骨架、导航组件） | **已分层，勿动** |
| `danmaku-engine/` | 弹幕解析与渲染引擎 | **已分层、19 个测试，勿动** |
| `core/model/` | P0-01 纯 Kotlin 业务模型（迁移期与旧模型并存） | 已建骨架 |
| `core/common/`、`core/domain/` | 格式化、结果类型与只读仓储接口 | P0 骨架 |
| `core/application/`、`core/navigation/` | SessionStore 与 Route 契约 | P0 空壳，未替换运行时 |
| `app/.../di/AppContainer.kt` | 进程依赖创建与初始化 | AppGraph 委托 |
| `infra/bili/` | HTTP/签名/业务分片/mapper/仓储实现 | 已从 app 迁入，原入口转发 |
| `infra/storage/` | 旧设置文件适配、搜索历史 JSON 编码 | 原存储格式兼容 |
| `app/.../feature/settings/` | 五组纯 StateFlow 控制器与逐字段 UI 投影 | 81 字段测试、升级比对通过 |
| `tools/` | 源码/模块依赖检查、Kotlin PSI尺寸审计、固定 Miuix 快照准备脚本 | Python 3 |
| `docs/` | 架构审查、对齐文档、来源说明 | |

### 现状规模（2026-11 实测，别当永久事实）

- 271 个 Kotlin 文件 / 约 63,700 行；`:app` 占 44,390 行。
- `ui/` 里直接调用 `api.xxx()` **115 处**，直接读全局设置 **63 处**。
- ViewModel / StateFlow / Repository / UseCase：**0 处**。
- 全局 `object`（含 5 个 `*Bus`）：**23 个**。
- 测试：`:app` 7 个、`TeakUI` 2 个、`danmaku-engine` 19 个。

---

## 3. 目标架构（重构完成后长这样）

完整论证见 **`docs/ARCHITECTURE_REVIEW.md`**；这里只给执行时需要的部分。

```
:app                   组装壳：路由表 + AppContainer(依赖注入) + 启动逻辑
feature:*              按业务切：home / video / dynamic / message / search /
                       space / library / live / settings / onboarding
                      每个内部四层：presentation → application → (domain)
core:model             实体与值对象（纯 Kotlin）
core:domain            仓储接口 + 纯业务策略（纯 Kotlin，无 Android/网络/JSON）
core:application       跨屏状态：Session、PlaybackController、DownloadQueue
core:navigation        路由契约（feature 之间不互相依赖）
core:common            结果类型、错误、日志、格式化
core:ui                = 现有 `:TeakUI` 模块的**目标称呼**；不重命名、不新建
infra:bili             HTTP、签名、Cookie、各业务 Api 分片、dto、mapper
infra:storage          设置(DataStore)、播放进度、下载缓存
infra:playback         ExoPlayer 适配、后台播放 Service、媒体通知、画中画
```

### 依赖规则（违反即打回）

| 规则 | 说明 |
|---|---|
| `presentation` **只能**依赖 `application` 与 `core:ui` | 不许出现 `api.xxx()`、不许 `import okhttp/org.json` |
| `application` **只能**依赖 `domain` 与 `core:model` | 不许出现 Compose 的 `@Composable` |
| `domain` **零依赖** | 不许出现 `import android.*` / `okhttp3` / `org.json` / `coil` |
| `infra` 实现 `domain` 的接口 | 只被 `:app` 在启动时装配，`feature` 看不见实现 |
| `feature` 之间禁止互相 `import` | 跨 feature 走 `core:navigation` 契约或 `core:application` 状态 |
| 完成标准 | 单文件 ≤ 400 行，单函数 ≤ 80 行，单函数参数 ≤ 12 个 |

> **模块名对照**：上图是**目标**模块名。当前仓库只有 `:app`、`:TeakUI`、`:danmaku-engine` 三个模块。
> `:core:ui` 指的就是现在的 `:TeakUI`（**不要新建第四个 UI 模块**）；其余 `core:*` / `infra:*` / `feature:*`
> 按 `docs/MIGRATION_TODO.md` 的 P0、P1、P5 逐步创建，不要一步到位。

### 迁移期的"双轨制"

新旧代码会并存一段时间，按下面办：

1. **新功能一律写在新位置**（feature/core/infra），不写进 `BiliApi` 和巨型 Composable。
2. **搬旧代码只做"搬迁"**：不改行为、不改逻辑、不顺手重命名。要改行为，另开一个任务。
3. **搬完就删旧文件**，不留 `XxxOld.kt` / 注释掉的副本 / 双写。
4. **一个 PR 只做一件事**，PR 描述里写任务编号（如 `P1-03`）。

---

## 4. 代码约定（从现有代码提炼，不是凭空规定）

| 项 | 约定 |
|---|---|
| 缩进 / 风格 | 4 空格；`kotlin.code.style=official`；项目**没有** ktlint/detekt 配置，别引入 |
| 注释 | **中文**，解释"为什么这么做"，不解释"这行在干嘛" |
| 可见性 | 模块内部优先 `internal`，只有跨模块契约才 `public` |
| Composable 命名 | 大写驼峰（`VideoGridCard`）；`@Composable` 只出现在 presentation 层 |
| 状态 | 新代码用**不可变 `UiState` + `StateFlow`**；`mutableStateOf` 只允许出现在 StateHolder 内部 |
| 错误处理 | 旧代码大量 `runCatching{}.getOrNull()`（历史写法，允许保留）；**新代码用显式 `Result<T, AppError>` 或领域错误类型** |
| 日志 | 用 `data/LogCollector.kt`（仅 debug 生效，内存 3000 行、文件 2MB 上限）；**禁止 `println`** |
| 依赖 | 版本锁在 `gradle/libs.versions.toml`；**新增依赖必须在 PR 里写明理由与替代方案评估** |
| 文案 | 中文硬编码（现状）。不要"顺手"迁移到 `strings.xml`，那是独立任务 |
| 网络 | 不要在主线程；`BiliApi` 内部已 `withContext(Dispatchers.IO)`，新代码沿用这个边界 |
| 线程 | 不要自己 `Thread{}`（少数历史代码除外）；用 coroutines |

---

## 5. 常见任务手册

### 5.1 新增一个 B 站接口

1. 先用真实请求确认路径、参数、返回字段（别猜）。
2. 落点：`infra:bili` 下对应分片（`VideoApi` / `CommentApi` / …）。
3. 写 `dto`（只描述 JSON 结构）+ `mapper`（转成 `core:model` 的实体）。
4. 在 `core:domain` 的仓储接口上加方法；实现放 `infra:bili`。
5. 补 mapper 单测：拿一段**真实返回的 JSON 字符串**当固定样本。
6. 迁移期如果 UI 还要走旧的 `BiliApi`，就加一个薄转发，**不要复制逻辑**。

### 5.2 新增一个页面

1. 建 `feature:<名字>`，内部 `presentation/` + `application/`。
2. 路由写在 `core:navigation` 的契约里，`Activity`/导航壳只负责传参。
3. 页面入参**必须显式**，不许写进 `AppGraph.pendingXxx`（迁移期旧页面除外）。
4. 首屏状态要有三种：加载中 / 空 / 出错（现有页面的通病是只有"加载中"和"空白"）。

### 5.3 新增一个设置项

1. 落点：`core:domain` 的 `SettingsRepository` 分组接口（外观 / 播放 / 弹幕 / 消息 / 通用）。
2. 存储实现放 `infra:storage`；**不要**往 888 行的 `SettingsStore` 里再加字段。
3. 默认值必须写在一个地方（不要在 UI 里散落 `?: 80`）。
4. 涉及播放器/弹幕的，要在设置页和实际生效处都验证一遍。

### 5.4 改播放器行为

1. 先读 `ui/VideoCollapse.kt` 里的纯函数（那是全项目少有的、已抽出来的决策逻辑，照着写）。
2. 播放器的三个敏感点：**旋转、画中画、后台播放**。改任意一个，另两个必须回归验证。
3. 后台播放的入口在 `ui/BackgroundPlayback.kt` + `BackgroundPlaybackService.kt` + `MediaPlayback.kt`，三处状态要一致。
4. 自动暂停/恢复的逻辑有单测（`app/src/test/.../VideoExitOwnershipTest.kt` 等），别绕过。

### 5.5 改弹幕渲染

`danmaku-engine` 里已有清晰分层（`parser` / `engine` / `render` / `control`），**按它的接口扩展，不要在 `app` 里写渲染逻辑**。改动必须补测试——它是全项目测试最全的模块，别拉低水位。

---

## 6. 已知坑（真实踩过 / 代码里写死的约束）

| 坑 | 说明 |
|---|---|
| Miuix 版本锁 | composite build 不能混 AGP 版本，升级 Miuix 要同时对齐 AGP / Kotlin / Compose |
| 主界面限帧 | `MainActivity` 把窗口钉在 60Hz（底栏玻璃每帧要录整屏）。**删了会掉帧**，别当冗余代码清理 |
| `BUILD_STAMP` | 每次构建都变，导致 `versionName` 变化。是特性，不是 bug |
| debug 包名 | 自动加 `.debug` 后缀；`-Ppkg=` 会覆盖并**取消**该后缀 |
| `minSdk 33` | 不要加降级兼容代码；也不要用 34+ 才有的 API 而不做版本判断 |
| `FreezeWatchdog` | 只在 debug 启动（`AppGraph.init`）。别在 release 打开 |
| `LogCollector` | release 不初始化、不写文件；只有 debug 有崩溃记录 |
| 大量 `remember` 状态 | 屏幕旋转/进程回收会丢；现有 `*Memory` 单例是补丁。新代码不要再加这种补丁 |
| `AppGraph.pendingXxx` | 隐式传参，少写一行就打开空白页。新代码禁止使用 |
| R8 fullMode | `android.enableR8.fullMode=true`；反射/序列化相关改动要在 release 构建下验证 |
| 测试稀疏 | `:app` 只有 7 个测试。**新逻辑没测试就等于没有保障**，这是当前最大风险 |

---

## 7. 测试与验证

```bash
gradlew.bat :danmaku-engine:test          # 快，先跑这个
gradlew.bat :app:testDebugUnitTest        # app 逻辑测试
gradlew.bat :app:assembleDebug            # 最终必须过
```

**硬要求**：

- `core:domain` 及任何纯逻辑的新增/修改，**必须**有纯 JVM 测试（不启模拟器、不联网）。
- 覆盖不到的 UI 改动，在 PR 里贴**手动验证清单**。视频相关至少验证：进入播放 → 切清晰度 → 切分P → 全屏旋转 → 画中画 → 后台播放 → 退出 → 重新进入恢复进度。
- 不许为了让测试过而改测试的断言（除非原断言本身就是错的，且 PR 里说明）。

---

## 8. 文档索引

| 文件 | 内容 |
|---|---|
| `docs/ARCHITECTURE_REVIEW.md` | 现状审查、新旧架构对比图、文件去向表、验收标准 |
| `docs/KOTLIN_SIZE_AUDIT.md` | Kotlin PSI完整尺寸基线、实际超标清单与严格任务 |
| `docs/MIGRATION_TODO.md` | 可领取的迁移任务清单（编号、前置、验收） |
| `docs/DEADLINER_GLASS_SYNC.md` | 玻璃材质与 Deadliner 的同步约定 |
| `docs/HYPERPILI_DYNAMIC_ALIGNMENT.md` | 动态页与上游实现的对齐说明 |
| `docs/HYPERPILI_MESSAGE_ALIGNMENT.md` | 消息页与上游实现的对齐说明 |
| `TeakUI/docs/PAGE_CHROME_SPEC.md` | 页面骨架规范 |
| `TeakUI/docs/PROVENANCE.md` | 组件来源与许可 |
| `README.md` | 面向用户的介绍与构建说明 |

---

## 9. 给 AI 助手的工作流程（建议照做）

1. **先读**：本文件 → `docs/ARCHITECTURE_REVIEW.md` 的"六个病灶" → 当前任务编号在 `docs/MIGRATION_TODO.md` 里的条目。
2. **再定位**：用搜索找真实调用点，不要凭文件名猜。
3. **小步改**：一次只做任务卡上写的事；扩大的部分记下来，另开任务。
4. **自检**：对着 §3 的依赖规则表和 §3 的完成标准（行数/参数）检查一遍。
5. **报告**：说明改了什么、为什么、怎么验证的、有什么没做。

---

## 10. 变更记录

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-11-11 | 初版：现状盘点、目标分层、依赖规则、约定、已知坑 | 架构审查 |

### 2026-11-11 状态迁移检查点

首页、动态、消息、媒体库、空间的纯状态容器位于 `app/.../feature/*/application`；旧 `ui` 渲染入口仍在迁移，后续业务逻辑应进入状态容器和 core/domain 仓储接口。登录态由 core/application/SessionStateHolder 管理，AppGraph.session 仅为兼容 Compose 投影。分页必须传播协程取消并在 finally 释放 busy/loading；账号切换使旧请求失效。弹幕测试现需 kotlin-test-junit 与 Robolectric 4.17（仅测试运行时），不允许用 Android stub 默认值掩盖测试异常。

### 2026-11-11 视频、评论与显式路由检查点

视频只读/互动/播放选择/元数据状态位于 feature/video/application，评论分页与写操作位于 feature/comment/application。视频/动态/空间入口使用 core/navigation/Route 与 Intent 快照，禁止恢复 AppGraph.pendingXxx。VideoPageBus 等全局页面状态已改为 core/application/VideoPageStateHolder + 页面 CompositionLocal 投影；局部旧名称只是页面实例别名。跨播放器暂停使用 AppContainer 的 PlaybackPauseRegistry，不能改成只暂停当前最后注册实例。CommentLikeStore 按账号清理、容量 300，Compose 投影按 rpid 更新。ExoPlayer/BackgroundPlayback 尚未统一，剩余大文件与其他 Bus 仍需迁移；不要将本检查点误报为全量完成。

### 2026-11-11 播放控制与页面壳检查点

:infra:playback 已建立，Media3 构建/播放源准备、系统媒体会话与前台通知实现位于该模块。core/application 的 PlaybackController/PlaybackSessionStateHolder 是前后台的共享传输与状态契约，后台兼容 getter 指向 AppContainer 实例；禁止重新在 UI 维护独立真实进度或新建 ExoPlayer 后台副本。结束事件确认必须跨轮询保持，长生命周期协程读取播放状态要用可观察 delegate 或控制器最新快照，不能捕获一次性的 val 副本。三个其他页面 Bus 已改为 PageChromeStateHolder/页面壳投影。播放器与详情主函数、剩余缓存/状态、模块化及完整装机回归尚未完成。

### 2026-11-11 队列、刷新率与表情缓存检查点

播放队列现由 AppContainer 的 PlaybackQueueStateHolder 持有，旧 VideoPlayQueue 只是 getter。SmartRefresh 已改每窗口 StateHolder，规则在 core/domain/RefreshRatePolicy，勿恢复全局窗口引用；fullscreen 提示按播放 owner 隔离。共享表情使用 core/application/EmoteStore 与 core/domain/EmoteRepository，取消必须解锁，账号变化拒绝旧响应。主评论文件已拆至 400 行以内，但主函数/楼中楼及视频主函数仍需继续拆，不可将文件达标当作全量完成。

### 2026-11-11 播放跟踪与进度检查点

播放器历史与空降请求走 PlaybackTrackingStateHolder 及领域仓储；VideoPlayer 不再直接请求 AppGraph.api。旧 PlaybackProgressStore 只委托 AppContainer，实际 Android 适配在 infra/storage/AndroidPlaybackProgressRepository，必须保持 playback_progress/p_ 与 CSV 兼容；校验与淘汰在 PlaybackProgressPolicy。VideoDetailPager 与 VideoDetailEffects 已拆出，瞬时开关在 VideoDetailUiStateHolder；VideoScreen 仍超长，不能据此结束迁移。

### 2026-11-12 播放器拆分检查点

详情初始化/页面元信息/动作适配/弹层/遮罩位于 ui/video；播放器组合期状态集中在 ui/player/VideoPlayerUiState，生命周期、系统媒体、控件显隐、错误回退、播放源与进度绑定分别在 Player*Effects/PlayerMediaBridge。搬迁时保留 effect key 和动态状态委托，尤其长期协程不可捕获过期速度、bvid 或媒体会话。完整迁移与最终装机验收仍未完成。

### 2026-11-12 播放器组合契约检查点

VideoPlayer 为短组装入口；PlayerObservedStateHolder 为共享只读投影，事件闭包应持有 State 委托，不捕获一次组合的 playing/position/duration 值。半屏控件使用 InlineChromeState/Actions，更多面板使用 HalfMoreState/QuickActions/SettingsActions；保持状态与动作分组，避免重新展开长入参。SponsorSegmentPolicy 位于 core/domain，经 PlaybackTrackingStateHolder 使用，当前 bvid 的已关闭提示仍随视频作用域重置。完整迁移及最终验收尚未结束。

### 2026-11-12 详情与 Activity 壳检查点

VideoScreen 为短组装入口（VideoScreenRequest/Presentation/Events），状态创建与 effect 绑定在 ui/video/VideoScreenStateHolders、VideoScreenEffects，窗口几何与播放器动作在 VideoPlayerViewport、VideoDetailPlayerWindow/Actions。BV/AV 入口由 feature/video/application/VideoEntryStateHolder 和 core/domain/VideoEntryRepository 处理，infra/bili 复用原读取，快照优先与失败关闭保持。VideoActivity 只保留原生窗口/生命周期，通过 VideoActivityHostActions 接入 Compose 页面壳；tabs 仍为原 TeakPagerTabs。不要把推荐入场进度挪入懒列表或全屏分支。全部迁移及最终 main/播放/release 验收仍未完成。

### 2026-11-12 详情展示与主壳检查点

详情元信息、简介测量/绘制、互动按压、合集/条目与推荐尾部分别位于 ui/video 的小组件和 StateHolder；合集展开按 title/条目数重建，滚动和坐标仍是页面组合范围，不能挪入 AnimatedVisibility。主界面消息角标走 MessageBadgeStateHolder，按账号创建并由原 STARTED 生命周期轮询。主题、底栏与主 tab 组装在 presentation/shell，底栏/唱片动画在可见性分支外创建。App.kt 主函数与其余页面尚需拆分，全部验收仍未完成。

### 2026-11-12 主壳短入口与搜索检查点

BdFunApp 只组装 MainShellTheme/State、TeakTheme 和 MainScaffold，显式分组对象在 presentation/shell；主壳文本与显隐是原页面生命周期的 StateHolder。搜索网络与结果状态由 feature/search/application/SearchStateHolder 管理，经 core/domain/SearchRepository 与 infra/bili 接入；SearchFilterState 位于 core/domain，UI 在 ui/search。保持原防抖、用户先于视频、loadedOnce 与原错误原文；appliedFilters 原先未被菜单写回的行为本轮保留，不能借迁移修复。其余页面与最终全量验收尚未完成。

### 2026-11-12 空间展示检查点

SpaceActivity 只保留原生壳，SpaceScreen 与展示/滚动/动作/effect 在 presentation/space。main 671d264 实际为四个主 tab，投稿含子分类。compact/wide 四份列表与头部/选择状态保持原作用域，不要挪入加载或宽屏分支；chrome 进度必须在组合期读取后投影。分页使用 SpaceStateHolder.nextPage 在滚动事件时读当前 StateFlow，不能捕获首次页码/忙碌值；原第四页 bangumi 分支本轮保持。最终全量验收尚未完成。

### 2026-11-12 私信迁移检查点

私信业务状态在 feature/message/application/PrivateChatStateHolder，经 core/domain/PrivateChatRepository 与 infra/bili 接口实现接入。页面/账号各有控制器与子任务，退出或切换使旧请求失效；保留图片先发、文字失败重试不重复图片、草稿相等才清空等原规则。presentation/message/chat 只渲染与派发，Route.PrivateChat 显式传参并兼容旧 Intent 字段。游客运行检查不代表登录后真实收发通过，最终全量验收仍未完成。

### 2026-11-12 消息中心与首页展示检查点

消息中心和通知页展示在 presentation/message（保留旧包名），账号变化重建控制器并取消对应子任务；离开用 close/revision 拒收旧结果。页脚在 application 读取当前游标与重试状态，消息打开优先级在 MessageOpenTarget。首页展示与复用卡片在 presentation/home，HomePullSession 保留原阻尼/阈值/缓动与 remember(state) 几何捕获，不要在拆分中改动布局或合并两种卡片。旧 ui/MessageScreen.kt、ui/HomeScreen.kt 已删除。最终全量验收尚未完成。

### 2026-11-12 动态展示与详情检查点

动态展示在 presentation/dynamic，旧 ui/DynamicScreen.kt 已删除；DynamicCard 使用 actions/presentation 契约，主卡与转发卡的大图状态必须独立。大图变换决策在 feature/dynamic/application，保留原 pager 手势交接阈值与关闭时序。动态详情经 DynamicDetailRepository 和 DynamicDetailStateHolder 接入，保留与列表不同的原点赞并发/回退规则。账号替换时容器关闭旧动态控制器，页面动作按 state 取消；InlineVideoPlayer/InlineVideoCache、评论回复条及最终全量验收尚未完成。

### 2026-11-12 内嵌预览检查点

内嵌预览经 InlineVideoRepository / BiliInlineVideoRepository 和 feature/dynamic/application/InlineVideoStateHolder 接入；原静态 InlineVideoCache 已删除，仓储保留 BV 单键、各 12 项上限、原淘汰与晚采样设置规则。原生实现为 infra/playback/Media3InlinePlaybackController，经 app 装配 TextureView 绑定；不要复用详情的 Builder 改掉默认缓冲/音频焦点，也不要改变 preferred→64→最后一项的原选择顺序。动态账号 slot 的所有清理走 releaseDynamicSlot，先 close 再取消投影。临时预览测试页已删除，最终登录动态/全量 main 对照及发布仍未完成。

### 2026-11-12 评论回复条与评论页入口检查点

评论回复条状态在 feature/comment/application/CommentComposerStateHolder；CommentImageRepository 的图片字符串是原上传编码后的不透明载荷，展示层不解析。失败上传保留旧载荷、发送执行时读取图片、成功清当前草稿等均为 main 的既有时序，不可顺手替换为私信规则。原 ui/CommentReplyBar.kt、ui/CommentScreen.kt 已删除，展示分别位于 presentation/comment/composer 和 page；入口使用 request/layout 契约。分页触发必须读取当前 StateFlow，避免长期 effect 捕获初次 loading。页面/账号生命周期仍需全量审计；全部逐页 main 对照和 release 交付尚未完成。

### 2026-11-12 评论作用域与楼中楼检查点

评论列表/动作/楼中楼控制器均有 close 与迟到结果守卫；展示在账号、CommentLikeStore.generationState、页面及原查询/根楼 key 下持有控制器和子任务。替换时先 close 再 cancel。共享点赞关闭回退必须只执行一次，不能让晚到的取消回退覆盖新页面快照，也不能跨账号代数。楼中楼展示在 presentation/comment/thread，旧 ui/CommentThreadSheet.kt 和 ui/comment/CommentThreadDialog.kt 已删除；原窗口位置、布局节点、渐变和动画不改。CommentRow/折叠回复等剩余长函数及最终全量验收仍需继续。

### 2026-11-12 评论行与图片展示检查点

评论行/前两条折叠回复在 presentation/comment/row；CommentRow 使用 item/disliked/actions 三个参数，行级点赞仍按 rpid 订阅，长按 bounds 在原行范围的展示 StateHolder 中。图片缩略图与原大图手势在 presentation/comment/images；每个 pager 页独立记忆 scale/pan，不改 1.01、12px 和 1..4 阈值。楼中楼回复输入已迁到 presentation/comment/thread。旧 ui/comment/CommentRow.kt、CommentReplyPreview.kt、CommentImages.kt、CommentThreadComposer.kt 已删除。评论展示局部源码尺寸已达标，完整行为/像素验收和全量迁移仍未完成。

### 2026-11-12 引导页检查点

引导状态在 feature/onboarding/application/OnboardingStateHolder，只依赖现有 AppearanceSettings/GeneralSettings 与设置变更信号；保存恢复只传 step 整数，勾选/条款/方向为本次会话状态。TermsActivity 的 manifest 自行处理旋转，所以普通旋转保留全部状态，Activity 重建才重置临时状态；不要把两者混为一谈。打开条款立即标记已查看，继续仅由 accepted 控制，不新增完整阅读或滚动门槛。六项设置沿用原仓储约束，色彩/界面风格仍限制 0..1；主题全局投影在展示适配器按原顺序更新。展示位于 presentation/onboarding，旧 ui/OnboardingScreen.kt 已删除；呼吸动画保持在 graphicsLayer 内读取 State，避免逐帧整页重组。全量迁移/main 像素验收与 release 交付仍未完成。

### 2026-11-12 媒体库与个人页检查点

媒体库与个人页展示位于 presentation/library、presentation/mine，业务状态位于 feature/library/application、feature/mine/application。页面不直接调用网络；媒体库操作每个请求前后核对账号归属，取消/关闭不得继续批量循环。个人页单账号资料缓存由 AppContainer 持有，五分钟从请求开始计时，失败字段回退到初始资料，账号变更清理。本机发送记录实现已迁入 infra:storage，旧 data 包仅留类型别名，禁止另写存储副本。上述为迁移检查点，登录后 UI/main 逐页对照与最终 release 验收仍未完成。

### 2026-11-12 设置展示依赖检查点

SettingsStore 只负责 Android 装配，展示层使用 feature/settings/presentation/SettingsBindings 的原逐字段投影，禁止在控件里新建 SettingsStore。Activity/TeakBareTheme 提供 LocalSettingsBindings；弹幕面板的草稿和展开状态在 DanmakuPanelStateHolder，18 字段重置策略复用于旧兼容入口。面板内容在 presentation/settings/danmaku；其离线布局对照通过不代表实际播放器的全屏旋转/画中画/后台行为已验收。

### 2026-11-12 设置页拆分检查点

设置页区块位于 presentation/settings/page，CDN 观察与选项文本在 feature/settings/application/CdnSettingsStateHolder；数据来自 core/domain/CdnSettingsSource，当前 AppContainer 通过 CdnSettingsAdapter 接线既有测速实现。展示层不得回查 CdnAutoSelector 或 AppGraph.api；保留 400ms 轮询、Map 改变才重采选项、主题 260ms 延迟和原文件格式。外链及关于页路由由 SettingsScreenRoute 装配。

### 2026-11-12 后台唱片迁移检查点

后台唱片展示在 presentation/player/disc，播放输入通过 core/application/BackgroundDiscPlayback 显式注入并观察 StateFlow；位置计算显式接收 SettingsBindings。纯方向/阈值策略在 DiscGesturePolicy，保持横向同幅对角优先、32dp 触发、8dp 触摸阈值、380ms 长按与抬手结束倍速。静态玻璃和旋转层必须保持兄弟节点，退出手动补间先注册，再注册 280ms 停播放效果。既有 500ms 后台刷新只在 debug 附加 BGP_STATE 诊断，release 不写该日志；其三敏感路径的最终回归仍必须做。

唱片显示投影只允许 cover/playing 驱动重组，进度、缓冲和其它会话元数据不应触发重绘；点击时仍通过端口晚读取 video/position。独立 main 基准工作区 /tmp/bdfun-main-fidelity-671 与 APK /tmp/bdfun-main-671-debug.apk 可复用于后续同环境对照，当前主工作区和模拟器均已回到重构版本；最终验收仍需再次刷新 origin/main。

### 2026-11-12 富文本与表情检查点

富文本/表情展示在 presentation/text；命中区间与表情面板快照在 core/application，视频 token 与跳转识别在 core/domain，短链 HTTP 与标题请求在 infra:bili。UI 通过 LocalRichTextDependencies 显式取得绑定，不再从全局 EmoteCache/BiliLinkTitles 请求网络。保留链接→表情→提及优先级、裸 BV 首次 indexOf、纯文本快捷路径忽略 prefix、表情双括号 pick、面板 ensure 后单次快照等原规则。标题成功/失败共享 4096 项 LRU，账号变化清空并拒绝迟到结果；取消不缓存为失败。新加载不应逐个发布并行标题，仍等整批完成再发布。全量验收与 release 交付未完成。

### 2026-11-12 扫码登录检查点

扫码请求与轮询在 feature/login/application/QrLoginStateHolder，仓储在 infra:bili，展示在 presentation/login。初次加载/刷新均隐藏 bitmap，刷新失败保留旧 polling key；每次轮询先等 1500ms，Waiting/网络错误不覆盖已有扫描提示。二维码生成失败保留空串与缺失消息的差别。页面 close 与 key 更换均使迟到结果无效；成功即使 profile 为 null 也必须回调。二维码生成与系统相册写入仍是原 Android 展示代码，不更改尺寸/压缩/路径/权限行为。

### 2026-11-12 自动 CDN 边界检查点

自动测速实例由 AppContainer 持有；选择/并发与 HTTP 在 infra:bili，cdn_auto 的旧 SharedPreferences 格式在 infra:storage，domain 仅保留端口。禁止恢复全局 CdnAutoSelector 或在设置展示里构造测速客户端。两轮取慢、四路并行、一小时严格小于缓存 TTL、force 忽略缓存、失败保留既有最佳节点都沿用原规则。无结果会清空本轮分数但不清最佳节点，无法识别的缓存 bestId 仍可能视为 fresh；不要在迁移任务里悄悄修改。

### 2026-11-12 关注/粉丝页检查点

RelationActivity 仅组装入口；展示在 presentation/relations，纯分页/关系动作在 feature/relations/application，仓储在 infra:bili。失败刷新保留旧行与原游标，重复页停止自动翻页；未知关系的一次点击只重查状态，不能连带执行关注。展示订单 frequent 用 StateHolder + Saver 保留原 rememberSaveable 行为；controller 更换仅重建子协程作用域，不重建整个 Teak 页面/列表。账号和 close 在每次请求前后核对，取消释放 loading/busy，迟到结果不弹成功提示。

### 2026-11-12 离线缓存迁移约束

离线缓存将由 core/application 的进程下载队列持有不可变 StateFlow；domain 只定义传输/文件存储端口，HTTP 在 infra:bili，旧 video_cache/items JSON 与文件路径在 infra:storage。缓存按当前 P 的 bvid/cid/quality 标识，必须有音轨才入队；视频/音频依次下载，进度发布 220ms、持久化 5000ms，阶段上限 0.98/0.99，完成才 1。删除或同 id 重下必须使旧任务失效，旧回调/旧 finally 不得复活条目或清掉新任务。迁移不改变目录/文件名/JSON 字段/本地 URI 和 UI 布局。

### 2026-11-12 离线缓存检查点

离线缓存已按上述边界接入：presentation/cache 显式接收 core/application 队列，VideoDetailLoadEffects/视频动作/CacheService 共享 AppContainer 的同一实例；旧 VideoCacheManager 和 ui/CacheScreen 已删除。缓存文件 URI/播放投影在应用层通过存储端口取得，UI 不直接触碰 File/Uri/JSON/OkHttp。20 字段旧格式与中断/丢失回退已离线测试，四状态/删除/空页与固定 main 原渲染同数据内容区域逐像素一致；本地生成两轨的真实下载、落盘及离线起播已装机验证。任何临时验证 Activity/manifest 必须清理并重建 APK 后再交付；本批已按此清理，完整全量验收仍未完成。

### 2026-11-12 顶栏与弹幕控件拆分约束

首页顶栏拆分只分组输入与抽取现有组合片段，不新增布局包裹。保持 wide>=600dp 与折叠屏判定、宽度 45%/280..480dp、160ms 两端 Crossfade、搜索结果覆层拦截点击以及原 FocusRequester/键盘 effect key。弹幕输入保留 IME inset 跟随、关闭冻结 bottom、首帧后渐显、80ms 聚焦和 200ms 延迟移除，不能为拆分改变发送参数/文案或外部 close token 规则。

### 2026-11-12 顶栏与弹幕控件检查点

HomeTopBar 位于 presentation/shell/homebar，输入分为 content/search/actions；全部 15 个旧调用值原样保留，searchReveal 仍不新增视觉用途。弹幕原生 View 桥接位于 presentation/player/danmaku；过滤策略在 core/domain，保持先过滤、原输入去重、最后时间排序，mode 2/3 不视为 blockScroll，白色比较不掩码。半屏输入业务状态在 feature/video/application/DanmakuInputStateHolder，Compose 状态仅留展示 StateHolder；取消释放 busy 并传播，关闭后不响应迟到成功。全屏输入的既有发送调度仍需后续应用层审计，不因本批半屏完成而标为全部输入完成。danmaku-engine 生产代码未改。

### 2026-11-12 剩余尺寸清理约束

Activity 外壳长参数按 options/disc/slots 分组，旧默认值和每个调用表达式原样保留；分组本身不改变 chrome、采集层、覆盖层顺序。菜单图标只抽取 DrawScope 分支，不新增 Canvas 或改变路径坐标/描边。卡片滑动保持原积分顺序、8ms 间隔、选页预测和指针 effect key；缓冲点与开屏动画只拆状态/绘制/时序，不改动画常量。本机发送记录保持首次进入快照，不改成实时订阅。

### 2026-11-12 尺寸清理检查点

Activity 外壳改为 settings/onBack/options/disc/slots/pageState/content 七项输入，19 处 main/debug 调用与 17 个原默认值已逐值对照；debug LogActivity 是正常接线修改，不是临时探针。菜单七种图标仍在一个 Canvas 的同一 DrawScope 绘制。开屏与缓冲点状态集中在展示 StateHolder；原动画时序、色板 getter 和绘图不改。发送记录只在进入时通过 SentHistoryRepository 取快照，筛选/计数/视频跳转在 feature/sent/application；存储仍为原 sent_history，500 条上限不变。SwipeCardPager 保留积分/指针 key/选页回调捕获规则和 pageCount 缩小时原小位移回弹行为。app 初步命名块函数尺寸已无超标，完整 PSI/表达式/其它模块审计仍需继续。

### 2026-11-12 基础设施长函数拆分约束

动态 JSON 解析只拆内容/图片、视频、直播推荐与最终装配，保持原字段优先级、空对象行为及转发仅一层；不得根据类型名补猜字段。通知只提取媒体会话更新，保持 artwork 请求与读取顺序、metadata→playbackState 顺序、按钮 requestCode/Action、compact 三键和原速度字段 1f；该字段不是实际倍速证据。测试捕获公开响应必须不含账户 Cookie，宿主风控失败不当成接口字段定义。

### 2026-11-12 全量验收与边界补强检查点

完整收尾按 docs/REFACTOR_COMPLETION_AUDIT.md 的 44 张任务卡和最终门槛执行；包目录不等于 feature Gradle 模块，命名块函数尺寸通过不等于全量完成。动态解析与播放通知长函数已拆，原字段优先级和通知读取顺序保持。动态新增结构探针不是真实接口样本；匿名采样 412 不可计为成功。

全屏弹幕草稿/样式/发送在 feature/video/application 的纯 StateFlow；保留文本点击时读取、样式协程开始读取、普通失败仍收起且不清草稿。取消传播，关闭后忽略迟到完成；展示收集状态，不再用 Compose 状态执行业务发送。架构守卫增加完全限定名与 app/presentation 扫描，17 个反例/正例接入 Gradle；旧展示只明确暂留 LogCollector/LiveCounters 诊断例外，仍待注入，旧 ui 原生桥接和模块图也仍待审计。

### 2026-11-12 播放器平台接缝

播放器展示通过 feature/video/presentation 的 VideoPlaybackPlatform/VideoOutput 与 core/application 的 ForegroundMediaSession 访问平台能力；Media3、PlayerView XML 绑定、响度音效、视频轨道切换与媒体会话创建由 app/playback 装配适配。接口按播放器组合实例创建，不能变成全局缓存；原 XML、resize mode、三分之一纹理截图、600mB 响度增益及 effect key/释放时机都须保持。

### 2026-11-12 视频读取缓存作用域

视频详情与推荐的读取缓存由 AppContainer 持有 VideoReadCache，页面领取带账号 mid/revision 的访问句柄；页面结束关闭，真正退出仅按原规则清当前 bvid 推荐，旋转不清条目。旧账号/已关闭页面不得读写或清理新缓存。保留详情6条插入顺序、推荐12条 HashMap 原淘汰规则和空推荐不覆盖；不要换成 LRU。自动选档由纯 AutomaticVideoQualitySelector 配合 NetworkBandwidthSource，Android 仅提供原 Kbps，阈值与1024换算不改。Session资料刷新改变身份/失效时也须推进 revision，避免快速 A→B→A 复活旧句柄。

### 2026-11-12 外部/剪贴板视频入口

MainActivity 的链接去重与解析调度进入 core/application/IncomingVideoLinkStateHolder，产出 core/navigation/Route.Video；domain定义VideoLinkResolver，infra:bili保持旧BV识别/四跳短链/5秒/UA/静默未解析规则。主入口不再直接读取BiliApi或调用网络实现。保留紧邻原始链接去重（失败也记住）、剪贴板先记录再识别、不同请求并行按完成顺序打开、旋转后重新初始化；关闭页面或协程取消不得迟到导航。不要扩大av/时间点等旧入口支持范围。

### 2026-11-12 视频页设置契约

SettingsBindings 以原逐字段 Compose 投影实现 SettingsRepository，分组访问也返回该投影，保持观察/同步读写；repository 属性仍返回纯控制器，application继续注入纯控制器，不能把Compose投影传入业务层。VideoPagePresentationState显式接收设置契约，播放器/详情/弹幕开关从页面依赖读取，不回查AppGraph.settings；手势按DOWN读取、生命周期/退出/发送回调按原时刻读取，不更改effect/remember/pointerInput keys。删除无调用的DanmakuSettings全局代理，portraitGuard固定false与重置字段范围仍保持原规则。

### 2026-11-12 首页实际模块边界

feature:home拥有首页状态/反馈、Compose投影、网格/下拉/分页/错误/操作面板。纯状态仅依赖core接口。app仅通过createHomeFeedState与HomeScreenRoute装配；共享视频卡片也被空间/媒体库复用，保持在app的presentation/video/card过渡位置，通过HomePresentationHost的卡片/加载/宽屏网格能力接入，禁止feature反向依赖app或其他feature。原一镜到底anchor、卡片/加载渲染、布局尺寸/keys不改。共享UI下沉P3-07、P4作用域与全局对象前置验收仍未完成；此接线不代表P5-02或全量已验收。

### 2026-11-12 账号revision资源归属

SessionIdentity包含UID与SessionState.revision，首页/动态由core/application/SessionResourceSlot持有，获取时同步核对并释放过期状态，不能仅依赖异步Session collector或UID相同。Compose登录投影也保留identity，使A→B→A被StateFlow合并时读value的页面仍失效重组；同账号资料刷新/暂时网络错误不改变identity。个人页/媒体库工厂捕获identity并作为状态重建key；首页资源释放同时关闭纯控制器、拒绝迟到响应/反馈并取消原scope。其余空间/消息/缓存/Activity作用域仍按P4-06继续审计，不得以此宣称全部完成。

### 2026-11-12 空间页面状态归属

空间控制器按Activity的ViewModelStore持有，配置重建保留同页状态，真正退出由onCleared释放；同一个UP的两个页面不能共享可变状态，也不按mid批量删除别的页面缓存。账号归属继续使用UID+revision资源槽，SpaceStateHolder每次请求前后校验归属与协程取消、关闭后释放busy/loading且不接受迟到响应。Activity显式向SpaceScreen传入session/controller，展示不再从AppGraph按mid取进程缓存。保留原五请求并发、字段兜底、Tab/分页/头部布局；未授权不进行真实关注等账号写操作。

### 2026-11-12 Kotlin语法树尺寸审计

开发工具tools/kotlin-audit使用与项目锁定版本相同的kotlin-compiler-embeddable，仅进入工具JVM，不加入app/APK；模块图守卫禁止app依赖tools模块。审计自有app main/debug及core/infra/feature生产Kotlin，具名/匿名函数、表达式体、lambda（含lazy/初始化/字符串插值内代码）、访问器、主/次构造器、init块、函数类型和显式context输入参数都按语法树定位；解析失败不得当成通过。report任务只供定位，严格check任务才是门槛；在现存超标项收口前不可用report退出0宣称尺寸达标，也不能排除构造器/表达式来制造通过。TeakUI、弹幕引擎和内联供应商源码保持原边界。

### 2026-11-12 空间资料JSON边界

SpaceApi的space只负责原五个并发请求、原await顺序和条件获赞补查；成功响应经SpacePayloadsDto进入SpaceInfoMapper。VIP、充电、横幅和资料字段只在infra mapper组装，沿用原字段/默认值；获赞网络兜底仍在API，不把IO藏进mapper。parseSpaceVideo暂按既有薄解析入口注入，HTML处理保持旧Android边界，网页vlist真实成功样本未捕获时不得把结构探针记为真实接口验收。测试JSON记录来源/日期/截取方式，临时采样Activity/manifest必须移除并验证最终DEX无探针。

### 2026-11-12 最新main直播集成

直播以main 4c4e03b的界面/手势/弹幕协议为基准；HTTP与WebSocket/Brotli实现迁入infra:bili，纯直播实体进入core:model，读取/消息解码/连接经domain端口组装。页面业务状态使用application StateFlow，原TeakUI树只拆分与接线。DanmakuSettingsContent新增liveMode仅隐藏main已隐藏的直播不适用项，视频默认false。最终完成本步后构建默认签名release并按用户要求暂停目标；不继续其它迁移，也不重复已有全套测试。

### 2026-11-12 继续收尾与页面滚动容器

用户恢复全量重构，并要求只开发/执行必要的定向测试，已有全套不重复运行。用户另外授权修正设置等宽屏页面的双层同轴滚动：TeakUI新增自有TeakScrollContainer，内容区仍由原列表/ScrollState处理，左右留白作为并列手势区共享同一状态，禁止再在列表祖先叠加scrollable来转发留白拖动。centeredListWidth只负责原宽度/居中测量；接入通用页面外壳与搜索、普通说明页，视觉参数保持。此项是明确授权的滚动行为修复，不作为其他UI行为优化的依据。

### 2026-11-12 长构造器分组收尾

按职责拆分不可变数据中的可选扩展/收藏选择/会话偏好等值对象，所有原字段的只读属性、默认值与手写JSON协议保持。构造与copy调用显式写入对应分组，不引入超过12参数的兼容工厂/重载，也不删除字段来满足审计。验证复用受影响的映射、路由往返和状态操作测试，不给每个数据类堆重复样板测试。

### 2026-11-12 去全局对象收尾

无状态规则/协议参数改为有明确包与名称的纯函数、不可变常量；不把object改成companion或另建隐藏可变单例来降低计数。迁移保持原算法、默认值与调用顺序，已有规则测试只更新符号接线。HTTP连接池、进程诊断等有真实生命周期的对象单独记录保留理由；主题状态等属于容器的依赖应由AppContainer持有。结构审计已清零，后续代码仍必须通过checkLayering内的严格Kotlin PSI任务。

### 2026-11-13 视频模块接线

:feature:video承接纯业务状态、原生播放器presentation端口、播放器/全屏不可变契约与状态投影；只依赖core与现有TeakUI，不依赖app/infra或其它feature。app保留Activity/平台装配与原TeakUI渲染入口，逐步通过模块契约接线；搬迁不重写UI树。仅实际跨模块的状态/端口契约公开，内部实现函数保持internal/private；测试随被测实现移动，不复制双份。完整渲染/作用域审计与CI仍按全量目标继续。

### 2026-11-13 视频与评论显式依赖检查点

视频/播放器与评论展示从当前VideoPagePresentationState.dependencies取得控制器工厂、进度/下载/发送记录端口、播放队列、后台桥及诊断回调，不再回查AppGraph或旧PlaybackProgressStore/VideoPlayQueue入口。VideoStateFactory位于实际feature:video/application，CommentStateFactory位于feature/comment/application；每次创建独立控制器。原TeakUI渲染函数体保持。字幕/状态行/画质标签/全屏进度条及电视指示器已原样迁入feature:video/presentation/fullscreen，后续迁移不得恢复app/infra反向依赖。

跨视频/直播复用的资源播放图标、暂停Canvas和PixelIcon属于TeakUI纯展示，不通过feature:video供另一个feature调用；资源ID仍由app显式传入。全屏业务面板/控制器仍属于feature:video。

### 2026-11-13 全屏音频/输入迁移检查点

app/ui/player/fullscreen原三份剩余展示已迁入feature:video。听视频直接使用显式外观/封面请求；弹幕输入接收发送器getter，必须在点击submit时读取最新注册回调，不能改成composition时捕获sender值。DanmakuSettingsContent是设置/视频/直播共用设置展示，继续由app槽组装，禁止feature:video反向依赖settings feature。旧pointerInputSlider全仓无调用已删除；实际弹幕滑块私有androidPointerSlider保持原样。

### 2026-11-13 视频详情正文模块归属

作者/统计/简介/互动与分P展示、原生点赞水波纹已进入feature:video；app以VideoMetadataSlots注入原图片/图标/链接/复制与当前窗口。窗口只在页面组合内持有，不能进入进程容器。ResourceGlyph原样下沉TeakUI，业务资源目录仍在app。简介缓存键保持原链接标题映射，长按、动画和选择文本不改。

### 2026-11-13 详情尾部展示模块归属

分P/合集/相关推荐列表骨架进入feature:video；VideoDetailTailSlots提供原合集图片与跨空间/详情共享的视频行。合集198dp局部视口保留独立scroll/overscroll及原剩余速度拦截，它与宽屏重复外层不是同类问题，不并入整页滚动。

### 2026-11-13 视频主布局模块归属

详情pager、tab展示、分栏/视口/收起遮罩及瞬时UiState进入feature:video；app仅向tab传入原设置值和写入回调、原玻璃chip，保持TeakPagerTabs与原宽度/位置。分栏及视口既有7项测试随实现进入presentation测试，含Compose Dp的几何规则不属于application层。共享blockTouches原样进入TeakUI，所有旧调用改显式import。

### 2026-11-13 内联播放器与播放表面模块归属

内联控件/音频/更多及瞬时VideoPlayerUiState、只读播放投影、提示/缩放/画面覆盖和实际PlayerSurface进入feature:video。原生View仍经VideoPlaybackPlatform创建；UiState原资源引用随组合生命周期，不能升为单例。app仅提供原资源/图片/缓冲指示器和DanmakuLayer槽；设置以显式值传入。TeakActionSheet原共用外壳已下沉TeakUI，消息/详情调用同一函数，无新设计系统。

### 2026-11-13 播放器生命周期端口与读取时机

生命周期/控件显隐/方向/系统栏/HDR/来源/音效/弹幕时钟进入feature:video，通过PlayerPageHost读写原页面投影与暂停注册表，不新增第二份状态。设置在原回调/Effect读点使用getter，不能冻结为首次值。尤其graphicsLayer/draw中的状态读取迁移后必须保留在原绘制阶段：VideoPageTabs的playerBottomPx以getter输入，禁止提前取Float，否则旋转后tab会滞留旧高度。热门弹幕筛选为core:domain的selectHotDanmaku，原窗口/排序/去重顺序不变。

### 2026-11-13 媒体会话/退出/进度端口

前台挂接协议ForegroundPlaybackAttachment进入core:application，BackgroundPlaybackStateHolder仍实现原owner/token动作，禁止把具体app类型引回feature。媒体会话、进度轮询、连播/定时关闭与退出观察器归feature:video；退出Context由app的PlayerExitHooks捕获，设置仍在事件读取。播放来源选择/重试次序归core:domain的PlaybackSourcePolicy，保留空串与null、rawFallback不能单独触发原地址重试等原边界。

### 2026-11-13 播放器根绘制与Kotlin文件类

VideoPlayerLayout、手势识别/绑定、全屏/内联/输入覆盖与赞助展示归feature:video，app的PlayerLayout仅装配PlayerRenderPorts；getter/Composable槽保持原读点与条件分支。不得在不同物理模块里保留同package、同名且含顶层函数的Kotlin文件：它们会生成同名FooKt，可能编译通过但装机NoSuchMethodError。本批模块文件使用VideoPlayerLayout.kt，app装配仍为PlayerLayout.kt。文件路径迁移要连JVM文件类名一起检查。

### 2026-11-13 播放器模块入口

VideoPlayerEntry在feature:video内拥有原remember状态/平台实例/追踪控制器以及效果编排，app ui/VideoPlayer仅装配PlayerEntryPorts。初始化值继续在原remember中通过getter读取，Decoder异常fallback与平台工厂留app；蓝牙平台观察也由app槽提供。PlayerEnginePorts只传原页面/进度/挂接/退出/日志与设置回调，不创建第二份状态。VideoFrameLoader无frame调用及旧缩略帧槽/未消费动画已删除，不重建死资源管线。

### 2026-11-13 详情布局与收起策略模块化

详情根布局、metadata/tail/comments pager内容及InlinePlayerCollapseState进入feature:video；评论仍由app原槽提供，禁止feature间导入。VideoCollapseHost只桥接原页面回调，收起纯策略进入core/domain，PlayerCollapseMode进入core/model且删除旧枚举，不留双轨副本。原rememberSaveable/Saver、scroll阈值、220ms动画、暂停56dp恢复工具栏及事件时读取保持；tab底边仍须在graphicsLayer内通过getter读取。宽屏同轴外层只允许TeakScrollContainer的兄弟留白手势区，不能重新叠加祖先scrollable。

### 2026-11-13 详情加载与视口模块化

VideoDetailControllers/ScreenStateHolders、初次离线→内存→网络加载、作者/观看人数/字幕/收藏预取及全屏/pager效果进入feature:video。依赖由app的VideoDetailLoadAssembly提供原实例；下载保持provider延迟取值，设置按原事件读点读取，remember键及175ms元信息超时不改。VideoDetailPlayerViewport实际布局在模块，app只装配原播放器资源/动作；播放器底边仍通过SideEffect发布到同一页面状态，tab在graphicsLayer读取。禁止建立同package同文件名的跨模块JVM文件类。

### 2026-11-13 详情入口、动作与效果归属

VideoDetailEntry在feature:video拥有原状态观察/作用域/画质选择/效果/渲染顺序；app VideoScreen只转发并组装平台和跨feature槽。VideoDetailViewActions与播放器动作绑定移入视频模块，VideoDetailActionHost只提供原提示/分享/下载/进度/后台交接，不复制状态。remember动作的原bvid/控制器/scope键保持；onBack继续rememberUpdatedState。页面本身直接实现VideoDetailDanmakuHost，sender与aid/cid/进度使用原实例，DisposableEffect键和原释放行为不改。布局输入用具名对象跨模块传递，规避当前Compose编译器十参数回调changed-mask异常；没有改变原布局层级。

### 2026-11-13 视频页面壳展示与效果归属

VideoActivityPageStateHolder、VideoActivityHostActions中立契约、入口解析观察/队列/窗口回调效果位于feature:video；app继续创建原控制器并提供窗口/路由动作。VideoPageBody拥有原页面Box/渐变/评论背景，播放器底边必须在graphicsLayer内调用getter；评论内容仍由app跨feature槽提供。VideoPageOverlay拥有原输入/回复触摸遮罩和弹幕面板动画，VideoPageOverlayHost由同一页面状态直接实现，禁止复制reply/sender/panel状态。TeakUI动画已与原app体核对一致，原弹簧300/22、180ms回复渐变与所有阈值/对齐不改。

### 2026-11-13 后台播放平台边界

后台选集BackgroundQueueLoader是跨前后台应用逻辑，归core/application，原测试随实现迁移；infra禁止依赖feature:video。AndroidBackgroundPlayback在infra/playback持有原播放器/token/服务/释放线程，原服务类型、设置、UA和诊断由app显式提供。app BackgroundPlaybackStateHolder只保留原Compose投影：原收集时机与每次sync的同步更新必须保持，不能只改成异步Flow投影导致界面晚一拍。迁移不修改交接条件、后台500ms刷新、释放线程或动作/通知协议。

### 2026-11-13 播放响应映射边界

播放地址响应字段先在infra:bili/mapper读取为dto，再映射core:model；API分片只保留请求/错误/重试与转发。必须保留原同id分组/编码与带宽选择、blank URL跳过顺序、音频id优先级、原URL与CDN URL双份、timelength回退及accept_quality顺序；不按preferred删清晰度。真实匿名样本须记录来源，测试修改样本的边界变体必须与原响应区分。HTTP响应信封/请求编码仍为传输职责，不将全库org.json命中都误当业务泄漏。

### 2026-11-13 评论响应映射边界

评论/楼中楼/动态评论共用infra:bili/mapper字段解析，API只负责原请求。必须保留top优先且按rpid去重、page>=count的原结束判断（本次不另改分页含义）、acount总数、只取前三条一级preview且不继续嵌套、mid/oid/root/parent的Long及原图片/表情/@人字段规则。发送rpid/上传pictures属于传输结果/请求编码，本轮不更改发送或上传行为。

### 2026-11-13 消息响应映射边界

消息feed分桶/游标、会话/预览、私信、系统/新粉丝及未读字段读取进入infra:bili/mapper，API保留原请求、资料补全、ack和错误信封。followingOnly仍只认is_follow==1，nextTs必须来自未筛选的allSessions；私信仍按sequence排序、保留空白type1、撤回优先/未知type原文fallback，游标继续原领域策略。系统首屏ack和所有发送/已读/置顶/免打扰操作不得在mapper中执行或改时序。构造字段形状测试须标明不是实际登录响应。

### 2026-11-13 搜索与媒体库响应边界

搜索字段映射必须显式接入原HTML解码器，保留补全term优先/去重8项、热词请求回退顺序、视频/用户有效标识过滤及原字符串数字解析。媒体库mapper保留HistoryPage的cursor三种has_more形态与30项兜底、收藏视频按原始medias数量40项兜底、Number收藏夹id不额外过滤、关注列表不额外剔除0。关系查询ids来自原始有效mid去重，资料补全/已登录getter/请求顺序保持。云续播秒/毫秒判定原规则归core/domain并补纯JVM边界测试，不改阈值或跨P行为。

### 2026-11-13 空间正文与动态入口映射

空间合集/系列、投稿/文章/番剧/小店、brief/IP/计数与动态page/portal/up列表归infra:bili/mapper。请求、签名/小店POST、空间并行聚合和获赞回退时序保持。合集保留原seasons后series顺序与重复，不新增去重；文章有covers数组时即使首项空也不退回cover；小店价格前缀空格/标签竖线和source回退保持。空间投稿仍用原HTML解码器；动态UP同mid去重保留首项，直播UP不新增去重，长整数/Boolean读取不改。

### 2026-11-13 直播/账号/推荐/视频附加映射

直播响应字段、FLV+AVC优先/首url_info/稳定host排序、账号nav与QR非成功状态、App推荐卡片、字幕/空降及弹幕模型转换归infra:bili/mapper。直播plain→signed与WBI清钥重试、QR成功saveTvLogin→nav、Cookie/设备指纹及所有请求不改。App推荐仍限制32位aid，不凭现代aid含义另改；three_point_v2理由优先去重保持。弹幕转换仅机械移动，保留原分段过滤/排序、XML模式/4000上限与压缩解码，禁止借此修改danmaku-engine。

### 2026-11-13 main HyperOS 4 搜索接线

main d0a9ed3 的底部搜索仅属于 interfaceStyle=2，不新增独立设置开关；旧 bottom_search 迁移清理保持。底部搜索业务请求归 feature/search/application，展示在 presentation/search/bottom，主壳只装配入口与八条独立动画。保留180ms联想防抖、90ms搜索等待、先用户后视频、历史12项与热词展示10项，以及正文/顶栏/底栏各自的动画层级和参数。界面风格允许0..2，色彩风格仍0..1。唱片HyperOS 4统一贴右、偏移-70dp，不叠加页面lift。

直板机旋转策略按 Configuration.smallestScreenWidthDp 的600dp断点：紧凑设备自动横屏进入独占全屏，展开折叠屏/平板保持横屏分栏；竖屏退出全屏。不得使用旋转后的screenWidthDp判定设备大小，手动退出全屏不触发同一姿态下的再次强制进入。策略位于core/domain/VideoOrientationPolicy，展示只负责绑定。

### issue #36 共享搜索页面

HyperOS 4只拥有独立展开/关闭转场，内部复用顶栏的HomeTopBar及SearchOverlay（记录/联想/结果/筛选与同一SearchStateHolder实现）。两个入口会话独立，不保留BottomSearch业务/卡片副本。请求工厂从主壳装配注入，不在渲染组件回查AppGraph。

视频详情合集内部列表使用LazyColumn与LazyListState，不维护全部条目的测量坐标Map；定位当前集按索引跳转后只读取可见项尺寸居中，不能为测量远处条目而组合整个合集。198dp视口、6dp间隔、底部8dp与嵌套滚动余量拦截保留。顶部/底部/当前集的目标计算位于视频展示层，空列表不得发起滚动。

### 2026-11-13 MSG-01 消息行为对齐检查点

消息运行时收件箱使用 PiliPlus 固定参考版本的 gRPC SessionMain/SessionSecondary 与复合 offsets；旧 HTTP 门面只供兼容，不能把长整型时间戳当作新游标。会话身份、设置载荷仅在 infra 解析，路由走 core/navigation 的 MessageInbox/MessageTools 快照。设置、点赞人员、关键词、通讯录与单聊设置由 MessageToolsStateHolder 管理，私信使用 gRPC 发送/历史及 biz=im 图片上传；保留账号/页面迟到守卫与取消解锁。协议字段表通过 tools/generate-im-schema.py 从固定上游 pbjson 提取，无新增 protobuf 运行库。离线样本和游客启动通过不能替代登录账号的真实收发/撤回/服务端设置回归，验收范围见 docs/HYPERPILI_MESSAGE_ALIGNMENT.md。

### 2026-11-13 MY-01 我的页布局与预览卡片

我的页竖屏头图必须从页面顶部通栏铺开，不加外边距或卡片圆角；资料卡内缩14dp并上压30dp，头像上压24dp。窄屏设置入口在采样树外的顶栏右上，复用AdaptiveMiuixTopAppBar/TeakGlassIconButton；宽屏在资料卡下使用Miuix Card+ArrowPreference进入设置；头图复用SpaceBanner及其解码工具。宽度600dp起使用45:55的资料与内容双栏。历史/收藏/稍后再看大卡的三个独立开关仅在设置「我的页」区块，默认关闭；core/application/MineLayoutStateHolder统一发布，infra/storage独立mine_layout文件保存，不向旧SettingsStore追加字段。预览由feature/mine/application经LibraryRepository按需读取，每项最多两条；账号、页面关闭及配置代数拒收迟到结果。个人头图复用空间资料读取；收藏预览只使用真实FavoriteFolder字段，不猜公开性。游客不加载预览或展示本机发送记录正文。

### 2026-11-13 表情缓存约束

表情面板列表通过 core/domain 的 EmoteCache 端口按账号持久化，infra/storage 保存 JSON；
EmoteStore 先读缓存，24 小时内复用，过期保留旧列表并刷新，账号切换拒收迟到结果。
图片由 app 装配的独立 Coil loader 按显示需求缓存，不预下载整包；磁盘位于 filesDir/emote_images，
上限 128MiB。面板使用固定 220dp 的懒网格，禁止恢复一次组合全包图片的 Column。

### 2026-11-13 我的页玻璃采样循环修复

MainTabPager位于主壳layerBackdrop的录制树内，因此MineScreen及其资料/列表子组件不得读取该主壳Backdrop绘制玻璃。我的页窄屏设置TopAppBar由MainScaffold的MainMineChrome挂在录制树外，与首页顶栏同层；空间入口使用不采样的Miuix普通胶囊按钮。不得把设置顶栏重新放回MinePage，否则可能形成RenderNode循环，在RenderThread出现原生栈溢出（libhwui prepareTreeImpl/prepareListAndChildren递归）。

### 2026-11-13 我的页沉浸与宽屏设置入口

窄屏头图从状态栏后通栏绘制；主壳持有mineList与mine的TeakPageScrollState，经TrackTeakPageScroll接入采样树外的ProgressiveTopBarMaterial，不能只画透明TopAppBar而不接滚动进度。宽度600dp起顶栏仅保留滚动渐进模糊、不显示设置按钮，左45%资料栏下方使用Miuix Card+ArrowPreference进入设置，右55%保留功能内容。宽屏设置卡为普通组件，不读取主壳Backdrop。

### 2026-11-13 滚动视口与平板底栏 Insets

我的页宽屏LazyColumn、媒体库收藏/历史/稍后再看共用的HorizontalPager和LazyVerticalGrid必须铺满窗口；状态栏初始避让加入滚动容器的contentPadding，禁止在容器外加statusBarsPadding截断滚动视口，避免顶栏渐进模糊下留下固定空带。我的页设置ArrowPreference不带副标题，Card显式使用与资料卡一致的surface/onSurface。平板MainWideNavigation不叠加navigationBarsPadding，左右及底部均使用MainWideNavigationEdgePadding（20dp）；手机底栏规则不变。

### 2026-11-13 我的页预览刷新与高度

宽屏头图仅上角圆角，底角为直角。MinePreviewStateHolder前台刷新保留已成功的区块，失败/取消不清旧内容；关闭区块或账号失效仍清理并拒收迟到响应。展示按实际窗口宽度和字体缩放预留固定内容槽，初次加载用等高骨架，空/错误状态保留该高度；视频封面16:9、单项最大180dp、标题固定两行占位。不得恢复先清空列表再显示单行加载文字的收缩/膨胀行为。

### 2026-11-13 我的页五视频与四收藏夹

最近播放/稍后再看按HistoryItem.viewedAt倒序展示最近5项（分别为view_at/add_at的既有映射），以固定高度LazyRow横向滚动。收藏保留纵向4行，显示「共 n 视频」；封面使用现有LibraryRepository.videos(folderId,1)按mtime返回的第一条视频，不按pubdate排序。仅补全前4个收藏夹，空夹不请求；单项封面失败复用该账号上次封面，首次失败留空。所有详情请求前后核对页面/账号/代数，取消释放busy；初始骨架与加载完成占位高度相同，刷新保留旧内容。

### 2026-11-13 HyperOS 中性色对齐

HyperOS 中性色以 Deadliner 的 HyperOS 适配为准：surface 为页面层（浅色 F3F3F3 / 深色 000000），surfaceContainer 为卡片层（FFFFFF / 242424）。应用层旧 surface/surfaceContainer 引用及配对前景已交换，TeakUI 标准组件仍遵循自身语义；不要再次把两个 token 的定义反转。独立 Activity、直播与主壳 Material 色板统一由 appMaterialColors 投影，粉/蓝品牌设置保持。

原本显式启用平面背景的玻璃控件统一通过 TeakGlassDefaults.flatBackgroundAlpha 读取深色 0.9 / 浅色 0.96；无平面背景的默认值仍为 0，不要为纯图标按钮补背景。

### 2026-11-13 首页推荐优化设计入口

新增业务设计见 docs/HOME_RECOMMENDATION_DESIGN.md（REC-00 至 REC-05）。2026-11-14 用户要求在 PR #52 同一分支实现；阶段验收状态见 docs/HOME_RECOMMENDATION_IMPLEMENTATION.md。“推荐算法优化”计划默认关闭，关闭旁路新增排序与学习、保持既有推荐源和行为；账号/模式/清除代数拒收迟到事件，禁止伪造曝光或猜测推荐协议。不得将本设计检查点写成全部阶段均已验收。

### 2026-11-14 推荐优化本地 v1

推荐开关与学习归属由 core/application/RecommendationOptimizer 管理，存储为 infra/storage/FileRecommendationStorage 的独立 v1 文件；默认关闭，游客不落盘。首页只在新批次执行 core/domain 的有界排序，已发布条目不重排。关闭、清除和账号变化使旧 ticket 失效；播放学习仅关联首页真实点击，通过共享 PlaybackSessionStateHolder 观察，不改播放行为。不开新推荐接口、不伪造曝光/上报；本地算法实现不等于官方协议、登录账号体验或质量收益已验收。具体剩余项见实现记录。
