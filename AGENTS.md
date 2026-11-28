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
- **技术栈**：Kotlin 2.4.21 + Jetpack Compose 1.13.0-rc01 + Miuix 0.9.5-rc01（HyperOS 风格组件库）+ OkHttp + Media3/ExoPlayer + Coil。
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

1. 本项目用**本地 Miuix 源码**参与 composite build。源码固定在上游 `bc1024d78a154f5167efff262ae5ed66264c481e`（0.9.5 线，AGP 9.5.3 / Kotlin 2.4.21 / Compose 1.13.0-rc01），放在 `../miuix-src/miuix-bc1024d78a154f5167efff262ae5ed66264c481e`，或用环境变量 `MIUIX_SOURCE` 指定。也可运行 `python3 tools/setup-miuix.py` 准备 `.local/` 下的固定快照；找不到时回退到 Maven 依赖，但**版本必须对齐**（AGP 9.5.3 不能和别的 AGP 混用）。composite build 不继承主工程的 `local.properties`，快照目录需要单独的 `sdk.dir`，`setup-miuix.py` 会按 `ANDROID_HOME` / 根工程 `local.properties` 自动写入。
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
| 主界面限帧 | `MainActivity` 按设置把窗口钉在固定刷新率（默认 120Hz，可选 60/自适应；自适应 = 0）。底栏玻璃每帧要录整屏，**别删这段**，否则会掉帧 |
| `BUILD_STAMP` | 每次构建都变，导致 `versionName` 变化。是特性，不是 bug |
| debug 包名 | 自动加 `.debug` 后缀；`-Ppkg=` 会覆盖并**取消**该后缀 |
| `minSdk 33` | 不要加降级兼容代码；也不要用 34+ 才有的 API 而不做版本判断 |
| `FreezeWatchdog` | 只在 debug 启动（`AppGraph.init`）。别在 release 打开 |
| `LogCollector` | release 不初始化、不写文件；只有 debug 有崩溃记录 |
| 大量 `remember` 状态 | 屏幕旋转/进程回收会丢；现有 `*Memory` 单例是补丁。新代码不要再加这种补丁 |
| `AppGraph.pendingXxx` | 隐式传参，少写一行就打开空白页。新代码禁止使用 |
| R8 fullMode | `android.enableR8.fullMode=true`；反射/序列化相关改动要在 release 构建下验证 |
| 浮层玻璃自采样 | 画在播放器/录制层之上的浮层（如全屏评论面板）若继续读 Activity 根部的 `LayerBackdrop`，`RenderNode` 会形成「底片里包含采样方」的自引用，`prepareTreeImpl` 递归到栈溢出直接 SIGSEGV。当前解法是面板内 `LocalTeakBackdrop provides null`（玻璃退化为平面底）；更好的解法是把浮层挂到 `layerBackdrop` 录制层之外 |
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

播放器展示通过 feature/video/presentation 的 VideoPlaybackPlatform/VideoOutput 与 core/application 的 ForegroundMediaSession 访问平台能力；Media3、PlayerView XML 绑定、视频轨道切换与媒体会话创建由 app/playback 装配适配。接口按播放器组合实例创建，不能变成全局缓存；原 XML、resize mode、三分之一纹理截图及 effect key/释放时机都须保持。响度均衡自 2026-11-20 起改为 infra:playback 的软件 AGC（对齐 BiliPai），不再使用平台 LoudnessEnhancer 的 600mB 固定增益。

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

### 2026-11-14 SET-01 设置索引与原型

设置索引位于 app/src/main/assets/settings-index.json，JSON 解析在 infra/storage，类型约束与搜索在 core/domain，写入通过 feature/settings/application 的显式白名单绑定调用原设置控制器，不能按任意 pref_name 直接落盘。当前索引包含首批36项开关/固定枚举；动态 CDN、文件选择、账号动作、我的页与推荐独立存储等仍沿用旧页，未宣称全量替换。HTML 原型在 docs/prototypes/settings，Android 页面尚未替换。

### 2026-11-14 SET-02 索引设置 UI 与编排器

设置页已改为 presentation/settings/page 的索引渲染器：一级分类、横屏双栏与页面作用域搜索。全量现有设置使用64个组件描述（含关于身份区及PR52的3个可选扩展）；不再沿用原型36项索引或旧卡片兜底。switch/dropdown/font_scale/cdn_dropdown/page_switches/mine_switch/action/developer/version保持原组件、条件与存储边界；select不得改成三级页面。一级入口现按settings-seperation.json分组，并使用彩色底板的Miuix Icons；功能按钮仍只用Miuix Icons。编排器为 docs/settings-organizer/index.html，导出必须完整保留组件键/type/default/options/storage/visible_when/inverted/value_type，允许调整分类、分组、顺序与文案；App资产是唯一来源，运行 tools/sync-settings-catalog.py 同步。浮点组件须保留value_type=decimal，防止JSON导出1.0变1后改变类型。关于页参考Deadliner 3b561656的AboutSettings.kt，来源与MIT许可见docs/SETTINGS_DEADLINER_PROVENANCE.md；光晕只在Canvas读取动画State，卡片仅背景随320dp滚动由0.5变1。推荐扩展由启动层注入，未合入PR52时隐藏，不建立跨功能实现依赖。

SET-02外壳约束：返回键、TopAppBar、大标题与滚动联动必须复用原TeakActivityScaffold/TeakLargeTitle/TrackTeakPageScroll，不画新的设置顶栏，不关掉原顶栏后自己仿制。二级页搜索仅加入原工具栏actions槽。

### 2026-11-14 SET-03 独立分栏与一级退出登录

横屏设置必须覆盖窗口全宽，左右分别使用原TeakPageScaffold（竖屏仍由TeakActivityScaffold装配），不在一个受内容最大宽度约束的外壳内摆两列。每栏独立PageChrome/滚动/搜索/popup/backdrop，并按栏尺寸提供窗口上下文。默认分配3:7，左栏下限360dp；不显示分隔线且不可拖动，右栏取剩余空间；一级页固定，右侧三级页只覆盖二级页，三级才显示右侧返回键。竖屏继续单壳。退出登录是layout=root_footer中的原动作，登录后在一级列表最底部独立卡片呈现，不生成账号二级页。

### 2026-11-14 SET-05 一级分组与宽屏TopBar

settings-seperation.json独立描述一级卡片分组及入口route/icon/color，设置值和二级页面仍以settings-index.json为唯一来源。不得复制设置项或把退出登录移入入口分组。左右栏保留真实容器大小供弹层使用，但通过LocalWideFoldTopBarOverride明确使用外层宽屏chrome策略，不能让360dp左栏误判为手机TopBar。

### 2026-11-15 SET-06 搜索覆盖层与Dialog动作

设置搜索覆盖层必须保留进出动画与退出帧的query/scope快照，返回先关闭搜索，不启动NavDisplay的页面预测返回。横屏仅左侧全局搜索，右侧不再显示搜索按钮；竖屏二级仍保留分类搜索。Dialog动作统一secondaryContainer.copy(alpha=.85f)底色，破坏性操作显式destructive=true并用error文字，普通按钮不受影响。

### 2026-11-14 直播页内容 / 分区 / 关注与图片保存检查点

直播首页接口 `/xlive/app-interface/v2/index/feed` 必须带 `fnval=912&network=wifi&scale=2`，少一个就 -400；`card_data.small_card_v1` 是**单个房间对象**，不是 `{list:[]}`，按 list 解析会把整页卡片丢光；观看人数取 `watched_show.text_large`。分区列表改用 BiliPai 验证过的 `/room/v3/area/getRoomList`（app 端 `second/getList` 与 web `getList` 实测均 -352，WBI 也救不回来），分区请求参数必须分开存 `area_id`/`parent_area_id`。登录态首页不下发 `area_entrance_v3`，按 PiliPlus 的做法用 `/room/v1/Area/getList` 补默认分区。关注直播不再用卡片横排，改成和动态页同源的 portal `live_users` 头像行（`LiveFollower`）。直播拉流必须在 ExoPlayer 数据源上带 `Referer: https://live.bilibili.com/`、web UA 和 Cookie，否则有地址也黑屏；竖屏顶栏不再用纯黑色块，改用页面 BackdropGlow + 顶部渐变。图片查看器长按弹 `TeakDropdownPanel`（保存该图片 / 保存全部图片 / 举报红字），位置按长按点换算锚点；保存走 MediaStore `Pictures/bdFun`。评论菜单锚点改成行内记录的按下坐标，带图评论追加「保存全部图片」；「自由复制」改为弹 `CommentCopySheet`，正文进 `SelectionContainer` 可自由拖选。首页三段与动态页顶栏过渡合并为单一 `MainTopBarChrome`，不要再各写一份。下拉刷新的 `canScrollBackward` 调用方不得再取反（会变成不在顶部也能刷新）；分页触发统一 `drop(1)` + 按卡片数比较。

### 2026-11-15 推荐优化与设置索引集成

PR #52 的推荐开关、清除和错误提示由 SettingsScreenRoute 向 main 设置索引的 recommendation_toggle/clear/error 三个扩展槽显式注入，沿用推荐独立存储，不恢复旧设置卡片列表。首页入口同时保留 main 的直播/番剧分段、按风格变化的顶栏高度，以及推荐曝光所需的 interactive 守卫。

### 2026-11-16 推荐优化与首页弱网缓存集成

首页 StateHolder 同时接收 optimizer/cache/requestTimeoutMs，保留 main 的首屏缓存和请求超时；缓存读取完成也须核对账号/模式 ticket。装配层按账号、来源与优化模式隔离缓存文件，关闭优化不复用优化后的缓存顺序。

### 2026-11-16 UPD-01 GitHub 更新与发布

更新策略与版本规则在 core/domain/update，检查状态在 feature/update/application，GitHub 适配在 infra/bili/update，系统后台下载在 infra/storage/update。preview 检查所有渠道、正式版仅正式渠道；设置关于版本行进入 UpdateActivity，release 冷启动每日检查。release 工作流输入 version 自动计算 versionCode，支持手填列表日志及 DeepSeek Agent 生成自审；密钥仅用于 CI。发布创建新标签，禁止覆盖既有标签。关于页背景全宽，列表独立使用 centeredListWidth。

UPD-01 页面无更新默认展示当前版本日志，历史日志由同一仓储列表提供；底部双玻璃按钮配渐进遮罩，有更新仅显示下载条，下载前申请通知权限。DownloadManager 系统通知持有后台进度。旧非标准标签只展示历史，不参与版本比较。

### 2026-11-16 UPD-02 设置更新入口与右栏

关于页顶部身份区整块通过 SettingsDestination.Update 进入内嵌更新页，横屏保留左侧设置目录。UpdateScreenRoute 为 Activity 和设置右栏共用的装配入口，展示不创建 Activity 或仓储；版本行只显示版本。更新页以 bdFun 品牌标题和版本号为主，状态作为次要说明；底部玻璃按钮最小高度为48dp。

关于页版本快照由 BuildConfig.SOURCE_REVISION/BUILD_TIME 提供，记录8位commit（含未提交改动时标记dirty）与带时区的编译时间，身份区与下方卡片保留独立留白。

更新日志入口集中在检查更新页（原历史版本按钮），关于页索引不再包含changelog动作。检查按钮禁用色显式从当前primary派生，避免回落到Miuix默认蓝色。

关于页的版本/版本快照/编译时间统一在“版本信息”设置组，顶部身份区不再显示快照和编译时间；讨论/帮助等在独立组。日志复用presentation/update/ChangelogScreen中的原列表、版本标签和彩色圆点布局，经应用装配自动加载GitHub，ui/ChangelogScreen.kt已删除。

快照为设置项展示名（键仍为version_snapshot），commit/dirty与编译时间均由Gradle自动生成。更新页与日志页使用AnimatedContent水平位移+淡入淡出，遵循animationsEnabled。

### 2026-11-16 ICON-16 图标预设

图标选择依据 docs/icon-presets/issue-16.json。Miuix 扩展图标只用 MiuixIcons.Name 的默认 Regular，不显式指定其他字重；Basic 系列没有字重 API，沿用预设选中的 Basic 图形。Lucide / Extended 仅打包已选择的32项矢量，来源固定在 tools/icons/lucide-selected.json，经 generate_lucide.py 生成 TeakUI 共享 drawable 与 LucideIcons 资源索引。线框使用24视口/1.8描边、填充图形92%光学收边；页面通过 AppGlyph 显式传入原场景尺寸和颜色，不改变触摸区域。相同语义与图形共用资源；预设明确不同的半屏/全屏投屏、后台听音频等不强行合并。预设不是运行时动态配置，不读取 Downloads；图标改动仍需视频旋转/画中画/后台及登录页面手动回归。

ICON-16 的 AppGlyph 与主底栏 VectorPainter 使用双倍采样缓存，保持布局尺寸不变；不要通过加粗、改图或移除原底栏缩放来掩盖低分辨率缓存。原字符预设 currentId 保持稳定，implementation/legacyName 记录矢量实现；本地提交的源码位置在工作台标为本地，不指向不存在的远端提交。

### 信息流帧率设置
主界面默认请求 120Hz，可在外观设置选择 60 或自适应（0，交给系统）；MainActivity 观察设置状态应用窗口偏好，不改变播放器智能刷新率策略。

### 2026-11-17 BUF-70 点播缓冲

点播加载统一走 core/domain/buffering 的 VideoBufferProvider(CacheStrategy)，固定分阶段规则和本地生存统计策略共享安全水位/倍速/内存约束，Media3适配在infra/playback。设置adaptive_buffering默认关闭、当前播放即时生效；关闭不更新模型。账号聚合在infra/storage，游客不落盘；不得把卡顿/错误/自然结束作为主动退出或在后台交接时重建会话。详见docs/BUFFERING_DESIGN.md；直播/预览/离线下载不在本轮范围，真机收益与敏感路径验收仍须实测。

BUF-70/PR81后台保活：同实例点播使用Media3 WAKE_MODE_NETWORK与WAKE_LOCK；Home/锁屏保留Activity时由onPause按播放意图启动现有mediaPlayback前台服务，onResume停止非detached服务。不要只在唱片adopt启动，也不要用isPlaying排除缓冲中的会话；清理会话须停服务。真机后台断流根因和完整路径仍需验证。

### 2026-11-16 经典布局全页面对齐检查点

经典样式（interfaceStyle=1）改为常驻栏形态，全部改动以 `TeakUiStyle.isClassic` 为门槛，官方与 HyperOS 4 分支不变。顶栏不再做大标题/小标题切换：页面标题常驻显示在整条顶栏中央，内容里的大标题位置只保留 56dp 占位，由 `LocalTeakClassicChrome` 区分页面壳与引导页；顶栏、底栏、动态分段栏、设置搜索栏和输入栏都用 Miuix 的整条纹理模糊（`teakClassicBarGlass`，非渐进式），叠加半透明底色保证可读，关闭高级材质时退化为平面底色；所有栏不再画分割线，顶栏按钮（返回、搜索、设置等）只保留图标不画圆底，只有回到顶部按钮保留玻璃圆钮。首页恢复推荐/直播/番剧分段（搜索框下一行），收藏/关注/消息/已发送/动态/空间等列表改为无行分割线的通栏行；评论折叠回复恢复 12dp 圆角分组框，与其他布局复用同一段实现。未改内容卡片、播放器覆盖层和登录/引导页的独立卡片造型。

### 2026-11-17 视频/搜索/圆角体验检查点

视频页评论页签的顶部间距不再用「24dp + 沉浸 36dp」的固定补偿：按 tab 行底边 + 12dp 减去列表自身的状态栏内边距反算，竖屏收紧、横屏分栏仍为 52dp；`CommentSortRow` 新增 compactSpacing，只由竖屏主评论区传入。视频页 tab 的柔光玻璃改走 `rememberManualProgress(…, 300f)`，与收藏页返回键同一套固定时长补间，不再直接吃滚动进度。频道页（直播/番剧）再次点击底栏当前页改为 `requestRefresh()`：先与推荐页同款并行「回顶 + 下拉」，再刷新。

搜索页筛选写回 `appliedFilters`（此前只写 `filters`，所以永远不触发重新搜索）；`SearchRequest` 增加 `page`，搜索结果按 20 条一页向下加载，`SearchStateHolder.loadMore()` 带 revision 守卫并在短页时置 end，列表底部显示加载中/没有更多。

设备圆角统一走 Android 12+ 的 `RoundedCorner`（四角取最大，读不到退回 framework dimen）：设置页单栏转场用 `rememberNavSystemCornerRadius()` 作为 `NavDisplayEffects.cornerClipRadius`；非小米机型的缩略图放大转场把截图按「全屏圆角 = 设备圆角」反算裁圆，避免四角直角。

半屏播放器按当前清晰度宽高比判断竖屏视频：竖屏改用 3:4 窗口并以窗口高度 60% 兜底（`resolveInlineVideoHeightDp`，带单测），横屏与分栏不变。进入视频的「内容依次上浮」改为骨架屏（灰条 + 白色扫光），加载完成直接替换。

投币不再用底部 sheet：`CoinChoicePopup` 在页面中间浮出两枚橙色硬币卡（1/2 硬币），点卡片直接投币，点空白关闭。关闭「退出视频后继续播放」时滑到后台只暂停播放，不再自动进入系统画中画；画中画只由显式返回键（exit_video_control）和全屏按钮触发。

### 2026-11-17 收口：刷新编排/骨架对齐/投币视觉

首页三段（推荐/直播/番剧）的「再次点击底栏当前页 → 回顶 + 下拉 + 刷新」不再各写一份：统一为 `feature:home` 的 `HomeSectionRefreshEffect(request, gridState, pullThresholdPx, animatePull, isBusy, onPulling, refresh)`，三处只传自己的网格、补间与刷新动作；`HomePresentationStateHolder` 的 `handledRequest` 随旧实现删除。

视频详情骨架按真实元信息结构重排（头像 + 昵称/粉丝 + 关注胶囊、两行标题、播放/弹幕/日期、AV 号/分P、两行简介、分类、五个等宽操作圆钮），扫光改为在每个灰条自己的坐标系里绘制（不再整条被扫白导致看起来像缺块）。

投币两枚硬币卡的视觉改为照抄同源参考实现（BiliPai `CoinDialog.CoinChoice`：`0xFFC48A52` 卡底、`0xFFE7B781` 3dp 描边、四角螺钉、`币` 字硬币、白色 `N 硬币` 标签）；PiliPlus 上游 main 只有整页 `PayCoinsPage`，没有这套两枚卡浮层。

### 2026-11-17 投币页与刷新手势收口

投币改为照搬 PiliPlus 的整页 `PayCoinsPage`（`lib/pages/video/pay_coins/view.dart`）：硬币箱两档（选中 100/未选中 70dp，硬币 50/35dp）、左右箭头与横向滑动切换、角色图（1 枚 / 2 枚 / 余额不足）点击或上滑投币、硬币余额、同时点赞复选框、关闭按钮；投币时角色起手 → 硬币上抛淡出 → 提交，约 650ms。资源取自 PiliPlus `assets/images/paycoins/`（GPL-3.0，与 bdFun 一致），落在 `res/drawable-nodpi/teak_coin_*`。`VideoActionsRepository.coin` 增加 `alsoLike`（对应 `select_like`），成功后同步 liked/likeCount。上游的 `hasCopyright`（转载最多 1 币）与雷电帧 bdFun 接口没有对应字段，未接。

下拉刷新（小电视）也收成一套：新增 `feature:home` 的 `PullRefreshSession`（8ms 手动缓动、0.6 阻尼、阈值/上限由调用方按 dp 传入、松手过阈值刷新否则回弹）与 `PullRefreshSettleEffect`（刷新结束：进度归零 → 160ms 收小电视 → 260ms 回弹）。推荐/直播/番剧/动态共用，删除了 `HomePullSession`、`DiscoverPullSession`、`FeedPullSession`、`FeedPullEffects` 四份重复实现；再次点击底栏当前页的回顶+下拉+刷新统一走 `RefreshToTopEffect`。

### 2026-11-17 投币页与自由复制弹层细节

投币页：整列内容改为屏幕居中（原来贴底），遮罩改为 50% 黑（`0x80000000`）；左右切换键和关闭键改用 Miuix 图标（`MiuixIcons.ChevronBackward/ChevronForward/Close`），删除对应的 PNG；「同时点赞」的勾改为复用收藏夹 sheet 的 Miuix `Checkbox`（`ToggleableState.On/Off`）。评论「自由复制」弹层改用与收藏夹同款的 `MiuixBottomSheet`（标题 + 关闭按钮 + 底部主按钮），内容仍是可选择文本。

### BUF-70 后台与退出页面偏好分离

background_play（离开App或锁屏后继续播放）独立于show_disc_after_video/stopOnVideoPageExit（退出视频页后继续播放）。Home、锁屏、画中画不可见不代表页面finish，不得由stopOnVideoPageExit在onUserLeaveHint或历史pipEntered标记上暂停。后台关闭仍由生命周期ON_STOP暂停；真正返回/关闭视频页沿用退出偏好。两项都在设置索引展示，保持原存储值。

### 2026-11-17 风格配置收敛

TeakStyle 提供 Official/Classic/HyperOs4 三元入口，持久化编号保持 0/1/2；Official 与 HyperOs4 共享 TeakDesign.Modern，排列由 TeakLayout 单独描述。新增卡片形状与内容留白优先通过 design 解析，底部搜索通过 layout 解析。此次仅扩展 TeakUI 的主题契约，不重构材质引擎，不移动动画或状态作用域。

### 2026-11-17 宽屏直播/骨架屏/首页滑动检查点

骨架屏的统一实现在 `TeakUI/.../skeleton/TeakSkeleton.kt`（`rememberTeakSkeletonSweep`、`Modifier.teakSkeleton`、`TeakSkeletonVideoCard/VideoRow`）；灰块颜色和扫光只在这里定义，页面只按真实布局摆位置。视频详情的元信息、分P、合集、相关推荐，以及首页/直播/番剧/搜索/评论/媒体库首屏都复用这一套；分P/合集/相关推荐与元信息一样改为骨架直接原位替换，不再做上浮淡入。`VideoMetadataSkeleton` 保留真实结构，但不再自带扫光实现。

直播详情新增宽屏分屏（`presentation/live/WideLiveLayout.kt`）：只在 `smallestScreenWidthDp >= 600` 且横屏时启用，左侧播放器 + 右侧主播信息/弹幕/输入条，参照视频详情的 70/30 分栏；手机横屏仍是原来的全屏直播，竖屏布局不变。宽屏里点全屏只收起右侧栏（`wideFullscreen`），返回键先退回分屏，不触发转屏。

首页左右滑动改由 `HomeSectionPager` 承担，在推荐/直播/番剧之间切换；底栏首页/动态/我的不再响应横滑，只由点击切换（仍用 `animateScrollToPage` 保留横向转场）。顶栏胶囊指示器读取 pager 的连续位置（`currentPage + currentPageOffsetFraction`），手指拖动时跟手移动，不再等落页才跳；`RegisterScrollTop`、三段各自的刷新入口和搜索/底栏玻璃都继续读同一个 `homeSection`，刷新与滚动状态未改。视频页收起态在返回键右侧新增回首页键，复用半屏播放器的 `InlineHomeGlyph`，通过 `MainTabRequestStateHolder` 通知主壳回到推荐页。

投币页默认停在 2 币；`VideoDetail.copyright == 2`（转载）或已投过时退回 1 币，接口字段经 `VideoDetailDto/VideoDetailInfo` 透传并补 mapper 测试。两个硬币箱本身可点选，左右箭头、硬币箱点击和整页左右滑都能切换；遮罩由 50% 黑改为 60% 黑（`0x99000000`）。

### 2026-11-17 自适应分段控件检查点

首页分段基础控件位于 TeakUI 的 AdaptiveScrollableTabs；app TeakSegmentedBar 保留薄转发。
scrollable 默认 false，maxVisibleItems 默认 3；开启并超限时启用内部露字和居中滚动，
两端不得添加空占位，中间切换必须独立保持指示器位置。详见 TeakUI/docs/PAGE_CHROME_SPEC.md。
首页频道使用 core/model/HomeChannel 稳定 key 与 HomeChannelPolicy 归一化；推荐不可禁用。
设置三级页经 SettingsBindings/AppearanceSettingsStateHolder 写入既有 SettingsPreferences，
首页 pager 下标只表示可见顺序，不能直接当频道身份；热门、影视目前为明确占位页。
自适应分段动画用浮点位置直接驱动绘制层，不得恢复逐帧 LaunchedEffect + ScrollState
同步。手势期间由组件预览位置，松手才提交频道；动画取消必须用代数守卫，旧 finally
不得释放新手势的位置所有权。


### 2026-11-17 投币下移 / 宽屏限宽 / 预测性返回 / 头像与图片缓存

投币页整列按屏幕高度下移，左右切换箭头中心落在屏幕 55% 处（矮屏按内容完整可见兜底）；同时点赞勾选在 0.9 的基础上再缩 20% 至 0.72。收藏/历史/稍后再看取消 680dp 单列壳，网格改用首页同款 `BoundedVideoGridCells(280..560dp)` 自动加列；收藏搜索、消息中心主/右栏、设置宽屏详情栏统一以 `centeredListWidth(maxWidth = 680.dp)` 限制阅读宽度，禁止再叠加默认 `centeredListWidth()` 造成二次收缩。设置新增「预测性返回」，默认关闭，存储键 `predictive_back`；设置页 NavDisplay 直接读取该开关：开启用 Miuix 默认预测转场，关闭用 `NoPredictivePreviewTransition`（拖动不跟手、松手仍播完返回动画）。系统级跨 Activity 预览由平台 manifest/targetSdk 决定，不受该开关运行时控制。我的页与空间页头像描边改为 `drawBehind` 画在头像外侧的圆环，不再用 `border` 吃头像边缘。我的页窄屏头图改回与空间页一致的 16:10，避免同一张图两处裁切不一致。新增 `data/ProfileImageCache`：头像/头图按 mid 落盘到 `filesDir/profile_images`，展示先读缓存、接口照常请求，成功后写回；默认头图不覆盖 3 天内有效的自定义缓存，减少冷启动先闪默认图。

### 2026-11-18 同步 main / 头图 / 预测性返回 / 唱片堆叠

本轮先同步 origin/main（f4240d9，含 #86/#88/#89 等 37 个提交）。冲突集中在首页自适应分段、搜索网格与设置索引：这些文件 main 已是更新实现，按 main 版本收口；设置相关只保留本地新增的 `predictive_back`/`tab_click_refresh` 投影并补进同一张表。

空间头图：`orderedSpaceBanners(web, day, night)` 改为 App 空间装扮图（`images.imgUrl` / `night_imgurl`）优先、网页版 `top_photo` 最后兜底；收藏集装扮 `collection_top_simple` 的 cover 是 2:3 竖长原画，不再作为头图候选，`bannerAlignmentY` 一并删除。只有本人的账号同时有 3009×313 的网页头图和 1080×514 的 App 头图，旧顺序把 9.6:1 的超宽图裁进 16:10 通栏，才会只显示图片一角。

预测性返回：`NoPredictivePreviewTransition` 在拖动阶段不跟手，松手提交时用最后一次手势进度把退出进度重新归一化到 0→1，避免结算阶段停住再消失（返回动画不能省）；设置页 NavDisplay 直接读取 `runtime.settings.predictiveBackEnabled`，开启走 Miuix 默认预测转场，关闭走该转场。系统级跨 Activity 预览仍由平台 manifest/targetSdk 决定。

唱片与搜索堆叠：`MainDockStack` 不再由 `discSlot` 跨阈值时的条件分支创建（会整块重建、硬切），HyperOS 4 下始终保留同一组件；后台播放开始/结束时自动播放同一套堆叠动画，手势和自动切换共用 `Animatable` 与代数守卫，迟到的旧 finally 不能改新状态；去掉用 `discSlot` 乘出来的 alpha，避免唱片出现前先闪一下空白。被划走的卡先跟手下沉，再缩小/旋转收进后层并淡出，过半后后层升到最上；整卡 blur 会重建 RenderEffect 并让内层玻璃退化成方形，已移除。我的页在登录资料未回来时显示整页骨架（头图、资料行、快捷入口与预览块），且头图网络图未回来时用扫光骨架占位；游客不显示骨架。HyperOS 4 首页外页面的唱片改为 B 站悬浮播放器位置（右下 16dp、抬离导航栏 16dp），首页仍由 `MainDockStack` 叠进搜索位。\n\n材质阴影会被动画容器的方形边界裁掉：堆叠卡的变换层按 `slot + 40dp × 2` 用 `requiredSize` 放大、卡片在层内居中，阴影有了绘制余量，不再被 54dp 槽位裁成方形。底栏里的唱片关闭自己的 `dropShadow`（外层变换下阴影 RenderNode 会露出方形边），改用 TeakUI 新增的公共 `Modifier.teakGlassControlShadow(CircleShape)`：它与 `TeakGlassIconButton` 内部完全同一套 Deadliner 阴影 profile，所以唱片和搜索玻璃的阴影一致；唱片内容在阴影内侧再按 `CircleShape` 裁切；搜索卡不裁切，保证它的玻璃阴影在切换过程中一直存在（结束后才出现是不允许的）。

### 2026-11-18 唱片位置统一 / 空降助手三档 / 返回动画同一套

用户明确要求：B 站布局下首页外页面（动态详情除外）的唱片必须和首页底栏旁的位置一致，HyperOS 4 也照此对齐；空降助手改为三档下拉；关闭预测性返回后的返回动画必须和开启时同一套，不能丢压暗和圆角。

唱片位置：`rememberDiscPlacement` 删除 HyperOS 4 首页外页面的「右下 16dp、抬离导航栏 16dp」分支，首页外独立页面与首页共用同一组几何（Tab 234dp + 8dp 缝 + 52dp 槽整体居中；`endPadding = groupLeft - 8dp`、`bottomOffset = 4dp - bottomBarRaise`；HyperOS 4 首页叠加搜索槽时仍返回 -70dp 占位，由 `MainDockStack` 自行定位）。`TeakActivityScaffold` 的 `discLift` 不再对 HyperOS 4 清零，动态详情继续用 `discEndPadding = 0.dp` + `BackgroundDiscDockLift`。模拟器（1220×2656、420dpi）实测：B 站首页、HyperOS 4 首页、两种布局下的设置页唱片中心均为 x≈927px；不要再把 HyperOS 4 独立页面改回右下角悬浮播放器位置。

空降助手：存储键由布尔 `sponsor_skip` 改为三档整数 `sponsor_skip_mode`（0 关闭 / 1 提示 / 2 自动跳过，默认 1），设置索引用 dropdown 展示，`PlaybackSettings.sponsorSkipMode` 经 IndexedSettingBindings 接线。旧布尔值只在迁移时映射为 0/1，绝不自动打开 2；模式判定是 core/domain 的 `sponsorSkipAction(mode)`（未知值回落到提示），玩家侧 `rememberPlayerSponsor` 在提示模式返回 `prompt`、自动模式返回 `autoSkip`，`VideoPlayerLayout` 在进入片段时直接 `seekTo(endMs)`。自动跳过不写 dismissed，用户拖回片段会再次生效；提示模式仍按 uuid 记录已关闭。

预测性返回：`miuix-nav` 的 `NavDisplay` 新增 `predictiveBackPreview: Boolean = true`。为避免 CI 使用未打补丁的上游快照，完整替换文件放在 `tools/miuix-patches/NavDisplay.kt`，由 `tools/setup-miuix.py` 在下载/复制快照后覆盖；升级 Miuix COMMIT 时必须重新生成该文件。开启时手势跟手；关闭时只消费手势事件，不驱动 `animatedTop`、不发布 gesture、不传手势速度，松手直接 pop，页面从静止位置走与正常返回完全相同的 `NavTransitions.MiuixDefault` + `NavDisplayEffects`。App 侧设置页只传 `predictiveBackPreview = runtime.settings.predictiveBackEnabled`，不要再写第二份转场；压暗和圆角由同一份 effects 提供。

### 2026-11-18 独立唱片阴影 / 设置页避让 / 首页首行间距

独立唱片（`DiscSurface`）的拖拽、退出上滑和按压缩放统一放在 `requiredSize(size + DiscShadowRoom * 2)` 的动画层上，唱片本体只挂 `teakGlassControlShadow(CircleShape)`。原因和 `MainDockStack` 一样：同一节点一旦带 `graphicsLayer`，`dropShadow` 的 RenderNode 方形边界会把阴影裁成方块；放大图层后阴影有绘制余量。`DiscShadowRoom = 40.dp` 由 `MainDockStack` 与 `DiscSurface` 共用，不要再各写一份。

设置一级页的唱片与动态详情完全对齐（`discLift = BackgroundDiscDockLift`、`discEndPadding = 0.dp`），且任何搜索页都不显示唱片（设置搜索时 `showBackgroundDisc = false`，主壳搜索覆盖层打开时 `discVisible` 也立即置 false）。动态详情共用同一个 `BackgroundDiscDockLift`（74dp），不要在两处写魔数。`TeakActivityScaffold` 在「唱片有抬升 + 页面显示回到顶部 + `scrollTopEnabled` 且 `scrollTopSide == 1`（右侧）」时把唱片镜像到左下：`align(BottomStart)`，并把与右侧相同的 `endPadding` 用作 `startPadding`，视觉上与右侧位置对称。

哔哩哔哩布局（`TeakStyle.Official`）的首页推荐网格，首行卡片与分段 Tab 之间额外加 5dp（`HomeVideoGrid` 的 `sectionGap`）；HyperOS 4 与经典布局保持原间距。

头像点缀与大会员胶囊统一在 `ui/AccountBadges.kt`：`AccountVipBadge` 是个人空间与「我的」页共用的粉底白字/灰底胶囊（8.5sp、5×1dp 内边距），`BoxScope.AvatarAccent(outerSize)` 负责弧线+圆点并按外径等比缩放（canvas = outer−2dp、stroke = outer×0.03125、dot = outer×0.0833、dot 偏移 = 0.401/0.281 倍外径）。个人空间用 96dp，我的页用 76dp；不要再在任何页面单独复制弧线或大会员样式。

### 2026-11-18 首页影视页

官方客户端反编译与接口实测确认：影视不是单一番剧接口，而是电影/电视剧/纪录片/综艺聚合（`season_type` 2/5/3/7）。追剧列表 `/x/space/bangumi/follow/list` 用 `type=2`；推荐 `/pgc/season/index/result` 按四种 season_type 同页并行后 round-robin 交错去重；头图 `/pgc/web/timeline` 合并四种类型（影视时间线可能很稀疏，保留番剧页原有的榜单/推荐回退）；榜单 `/pgc/season/rank/tabs` + `/pgc/season/rank/v2/list` 使用 index_id 2/5/3/7，首节合并为「热门影视榜」。页面展示完全复用 `BangumiPage`（`CinemaPage` 只传独立 state/rankState 与「最近追剧」标题），禁止复制一套 UI。`MainHomeChrome`、`MainShellNavigation`、`MainShellStateFactory` 为影视频道绑定独立滚动进度、下拉指示器与回顶刷新，行为与番剧页一致；`MainShellStateFactory` 的滚动绑定拆到 `TrackMainShellPages` 以满足 80 行函数门槛。

### 2026-11-19 首页热门 / 大标题回弹 / 宽屏空间

首页「热门」段已从占位页换成分页列表：接口是 PiliPlus 同款 `/x/web-interface/popular?pn=&ps=20`（真实响应 2026-11-19 匿名抓取，分页结束标记是 `data.no_more`，不是 `has_more`）。`VideoItem` 走现有 feed 映射，新增 `core:model` 的 `PopularPage`、`core:domain` 的 `PopularRepository`、`infra:bili` 的 `BiliPopularRepository` 与 `readPopularPage`。展示在 `app/presentation/popular`，列表行直接复用搜索结果的 `videoResultRows` / `videoResultSkeleton` / `videoResultFooter`（搜索页与热门页共用，禁止再复制卡片）；滚动进度由 shell 的 `TrackTeakPageScroll` 绑定，频道 chrome 从 `placeholder` 改名为 `popular`，`HomeSectionPlaceholder` 已删除。热门没有下拉手势，再次点底栏「首页」通过 `requestRefresh()` 回顶并重拉第一页。

大标题回弹：`TeakUI` 新增 `TeakPinnedLargeTitleHost`。列表里的 `TeakLargeTitle` 只负责占位（`LocalTeakLargeTitleHidden` 时 alpha=0、不加模糊），可见的标题画在滚动容器之外，按 `firstVisibleItemScrollOffset` 上移，所以上滑照常被推走、顶部下拉回弹时标题固定在底位。已接入收藏/历史/稍后再看、关注/粉丝列表、消息会话列表、设置一级/分类/搜索列表；动态详情与评论页、更新日志、条款、引导页尚未接入（大标题仍在评论列表内）。新增页面若也用大标题，照这个模式把标题留在列表占位并包一层 host。

宽屏限宽：`ui/FeedLayout.kt` 新增 `RecommendedCardMinWidth/MaxWidth`（180/260），与首页推荐、直播共用；收藏/历史/稍后再看和收藏内搜索的宽屏网格由 280..560 改为这组常量，只加列不拉长卡片。

横屏个人空间：`WideSpaceProfile` 改为直接复用 `SpaceProfileAvatar`（含 `AvatarAccent` 弧线点缀）和 `SpaceProfileBadges`（LV 图片、大会员胶囊、粉丝牌同一行），竖屏组件不变。

验证：`:app:compileDebugKotlin`、`:app:lintDebug`、`:app:assembleDebug`、`:app:assembleRelease`、`:app:testDebugUnitTest`、`:core:domain:test`、`:core:application:test`、`checkLayering` 全部通过；模拟器实测热门分页/加载更多、收藏页顶部下拉时标题不动、上滑时标题正常推走、横屏空间头像点缀与三种标识。Windows 下直接跑 `checkLayering` 会被 Python 默认 GBK 读 UTF-8 源码卡住，需设置 `PYTHONUTF8=1`（CI 的 Linux 环境不需要）。

### 2026-11-19 preview-7.0 发布准备

大标题不再在页面里各包一层 host：`TeakPageScaffold` 统一持有 `TeakLargeTitleAnchorState`，列表里的 `TeakLargeTitle` 只占位并登记文案，页面外壳按统一 chrome 位置（状态栏 + 8dp、统一左右内边距）画一份固定副本，滚动偏移在布局阶段同步读列表状态。不要重新引入按页传 topInset/scrollOffset 的旧写法；页面只要用 `TeakPageScaffold` 并在列表首项放 `TeakLargeTitle` 就自动生效。省略号形变菜单的阴影跳变问题尚未修复（本轮回退），后续单独处理。

消息页统一用 `HomePullRefreshBox`（首页同一套 `PullRefreshSession` + 小电视 + 回顶/收尾时序）；首次进入用 `MessageSkeletons` 骨架屏，不再显示小电视；`MessageSessionsEffects`/`MessageFeedEffects`/联系人/消息设置已移除 resume 时重载，从回复页等二级页返回不重新拉取。消息页 Tab 与上方快捷入口卡片的间距为 14dp。

设置第一页「关于」卡片下方直接放「捐赠」入口（复用关于的图标），关于二级页里的捐赠项已删；入口走 `runtime.navigation.onAbout(SettingsDestination.Donation)`。版本默认值改为 `preview-7.0`，正式发版由 `.github/workflows/release.yml` 的 `workflow_dispatch` 传 `version=preview-7.0` 和更新日志；应用内更新日志读取 GitHub Release，发版后自动出现。

### 2026-11-19 番剧/影视 PGC 集数与换集评论

番剧/影视卡片只带 `season_id` 进视频页（`Route.Video.seasonId`），视频页经 `VideoRepository.pgcEpisode(epId, seasonId)` 一次 `/pgc/view/web/season` 拿整季；`openBangumi` 不再先查首集 bvid 再按普通稿件打开（柯南这类上千集的老番因此少下载一整季）。有 `ep_id` 时按 ep 查；只有 season 时优先 `user_status.progress.last_ep_id`，否则首个正片。

`VideoDetailInfo` 增加 `epId/seasonId`，`VideoEpisode` 增加 `epId`；PGC 的 `pages` 必须为空，剧集只走现有合集卡片（`VideoDetailCollection` → `VideoCollectionCard`），不再有第二套选集 UI。播放源由 `BiliBackend.playInfo` 按 `detail.isPgc` 分流到 `/pgc/player/web/playurl`，清晰度/编码/CDN 仍复用 `parsePlayInfo`；番剧分享链接改为 `bangumi/play/ep{epId}`。

评论身份统一走 `core:domain` 的 `videoCommentTarget`：普通稿件与 PGC 都用**当前集 aid** + `type=1`（与 PiliPlus 的 `oid = isPugv ? epId : aid` 一致；2026-11-19 实测把 `ep_id` 当 oid 请求 `/x/v2/reply/main` 会返回 0 条）。视频页两个评论入口（详情 tab 与独立评论页）都把 oid/type 放进 `key`，换集时 aid 变化，评论列表、楼中楼、滚动位置一起重建；PGC 的 `upMid` 传 0，不把出品方当 UP 主。

已删除不再使用的 `PgcPlaybackRepository`/`BiliPgcPlaybackRepository`/`pgcStream`/`BangumiStream`，避免两套 PGC 播放路径并存。测试：`VideoCommentTargetTest`、`PgcPlaybackMapperTest`（真实样本 43161；注意预告花絮 key 是 `section` 不是 `sections`）、`VideoDetailStateHolderTest` 的 season-only 用例。

### 2026-11-19 全仓性能与精简审计（低风险项）

- 表情正则：`analyzeRichText` 按表情快照实例缓存编译结果；评论列表原来每条评论都会排序并编译一次全量表情正则。直播表情弹幕同样缓存。
- 时间格式化：消息列表/聊天/发送记录/全屏时钟改用线程级 formatter 或不可变 `DateTimeFormatter`，去掉逐行 `SimpleDateFormat` 与双 `Calendar`。
- 正则常量：消息 feed 解析、聊天卡片、消息跳转、BAS 目标、番剧时间线、空间数字格式等把 `Regex(...)` 提升为文件级常量，避免解析/组合时重复编译。
- 番剧入口省一次整季请求（见上）。
- 本轮未动（有行为或稳定性风险）：约 20 处 `delay(8)` 手动动画循环、`collectAsState` 到 lifecycle 版本的迁移、弹幕引擎内部实现、首页连续位置每帧回写 shell。要继续优化这些项，先补基准再改。

### 2026-11-20 视频页 Tab 玻璃淡入卡顿（已回退）

曾尝试把视频页 Tab 材质改成 `animateFloatAsState` + `TeakGlassSurface.materialAlphaState`（绘制阶段读 alpha），但该方案已按用户要求回退，当前仍保持原实现：`VideoPageTabs` 用 `rememberManualProgress(…, 300f)`，`TeakPagerTabs`/`VideoDanmakuControl` 在组合期读 `LocalTopBarButtonMaterialProgress`。不要再按回退前的 State 方案改，除非重新评估。

### 2026-11-21 本轮 UI 批处理与投屏

同步 origin/main 到 d40f364 后处理一批体验问题：投币整页浮层的空白遮罩可点击关闭；弹幕互动浮层的坐标改为按解码画面矩形而非播放器 View 计算，并按实际卡片高度做边界收口；互动投票每个选项独占一行，投完票从 `/x/vote/vote_info` 读 `options[].cnt` 显示比例条。搜索新增「番剧 / 影视」Tab（`search_type=media_bangumi/media_ft`，`SearchPgcItem`，点击只带 season_id 进详情），用户 Tab 改为无卡片底的平铺行并给头像留 2dp 外圈。Coil 统一 ImageLoader 补 `Referer: https://www.bilibili.com/`，`biliImageUrl` 的 `@尺寸` 改到查询串之前。

视频卡片的操作菜单统一为 TeakUI `TeakVideoActionMenu`（评论区同款 Miuix 玻璃下拉），首页/热门/收藏历史随后看共用；卡片右下角 `VideoCardMoreButton` 与长按走同一锚点。首页把长按回调从无参改为带卡片 `Rect`，`HomePresentationHost.card` 随之扩展。收藏/历史/稍后再看复用 `videoResultRows` 的 `onSwipeDelete`，由 `SwipeToDeleteVideo` 左右滑动删除。

投屏新增 `app/cast/DlnaCaster`（SSDP + UPnP AVTransport，无新增依赖）与 `CastDialog`；`PlaybackSource.castUrl` 优先使用可带声音的 durl，`PlaybackController.currentMediaUrl` 供全屏/半屏共用入口读取。注意：DASH-only 视频没有 durl 时电视端可能只有画面；Android 17 局域网运行时权限尚未接入。

后续修正：取消收藏页左右滑动删除（红色背景在透明行上会整行透出，且容易误触）；视频卡省略号移进 UP 行并缩小；搜索用户头像改用 1:1 `@160w_160h_1c` 而非 16:9 封面裁切；搜索列表 key 带 index，避免接口重复 bvid 触发 LazyGrid 崩溃；搜索 Tab 的 pager 只认 `settledPage`，不能再在 effect 启动时用当前页和 tab 比较（会把刚点的 Tab 改回去并停在两页之间）；投票选项改胶囊、比例条透明粉。视频页 Tab 与一镜到底本轮的尝试全部回退到 main，不保留自制退出动画。

### 2026-11-20 倍速音质与音画同步：对齐 BiliPai

用户反馈倍速声音扭曲；按 A 打开 AudioTrack 系统时间伸缩后音画不同步复现。对照 BiliPai 后确认它的做法是：

- 不打开 AudioTrack playback params（`setEnableAudioTrackPlaybackParams(false)`），倍速统一走 ExoPlayer 的 Sonic；
- 每次设速都显式写 `PlaybackParameters(speed, 1.0f)`，强制自然音高；
- 响度均衡不用平台 `LoudnessEnhancer`，而是在音频处理器链里注入软件 AGC（RMS 收敛 + 增益上下限 + 削波保护），只在设置开启时选用；
- 高速（>1.5x）时对 Dolby/Hi-Res 这类速度敏感音轨降级；bdFun 的 `choosePlaybackAudio` 本来就只选 30216/30232/30280 标准音轨，这一点已满足。

bdFun 对应调整：`Media3PlayerBuilder` 恢复 `setEnableAudioTrackPlaybackParams(false)`；`Media3PlaybackController` 用 `PlaybackParameters(speed, 1f)`；新增 `LoudnessNormalizationAudioProcessor` 与 `LoudnessAwareRenderersFactory`，按 `settings.loudnessEq` 在播放器创建时选择工厂；删除 `PlaybackAudioEffect`、`PlayerLoudnessEffects` 与平台 effect 路径。

注意：响度开关在播放器创建时读取，切换设置后需重新进入视频页才生效（与 BiliPai 的工厂选择方式一致）；软件 AGC 的听感是“响度趋于一致”，不再是无条件 +6dB。测试：`LoudnessNormalizationAudioProcessorTest`（3 项，含 16-bit 削顶与 float limiter）。

### 2026-11-21 非小米退出动画 / 动态分页 / 查看器与列表性能

非小米机型的「一镜到底」退出补齐：`TeakOneTake` 在公开缩略图路径把起飞矩形/源 Activity/屏幕与旋转写入 Intent；`OneTakeExit` 在视频页/动态详情 `finish()` 前用 PixelCopy 抓整屏，交给源 Activity 在 resume 时按系统同一组参数（336ms、PathInterpolator(0.3,0,0.1,1)、后半程 LinearOutSlowIn 淡出、设备圆角等比缩放）缩回卡片。视频页与动态详情统一继承 `OneTakeExitActivity`，相关推荐（源是上一个 VideoActivity）也走同一交接。小米机型不写 extra，系统原生退出动画不受影响；Android 14+ 必须用 `overrideActivityTransition` 关系统关闭转场，`overridePendingTransition` 已无效。

动态页改为和首页同一套 `HorizontalPager`：每个分类独立 `FeedState`（`AppContainer.dynamicFeedStateFor`，按账号/分类复用），顶栏只读 shell 的 `dynamicTab`，滑动与点选共用 pager；`FeedTopBar` 不再自己 `switchTo`。删除了 `FeedTopBar` 里旧的默认分类副作用。

图片查看器：入场进度/拖动位移只在 `graphicsLayer`/`drawWithContent` 读取，组合期只依赖宽高比；去掉开场前等 320ms 宽高比的阻塞；玻璃采样只在长按菜单打开时挂 `layerBackdrop`（常驻会让整张原图每帧多录一遍）。列表滚动时的 `onGloballyPositioned` 不再写 Compose State（`CardBoundsHolder`、普通 Map/数组），避免可见卡片逐帧重组。热门页菜单保持常驻到收起动画结束；`?` 不能再用 `?.let` 整体包住菜单。

尺寸门槛：新增代码已拆到 80 行内、单函数参数 12 个内；`reportKotlinSizes` 里仍有 `CastDialog`、`DanmakuCommandOverlay`、`SearchUiState` 三处历史超标，未在本轮一并处理。

### 2026-11-21 尺寸门槛清零与互动面板拆分

`checkLayering` 现已通过（Kotlin PSI 0 violations）。投屏面板拆出 `CastSheet`/`CastLifecycleEffect`；`SearchUiState` 的番剧/影视四字段收成 `SearchPgcState`；弹幕互动从 `DanmakuCommandOverlay` 拆成 `Session`（拉取/提交）、`Host`（画面矩形与卡片定位）、`Card`（头部/投票/评分/关注）与 `Controls`（投票行、胶囊按钮），提交时序、票数刷新和 `pending` 占位保持原样。不要为了拆文件改变投票/评分的先后顺序。

动态页 portal 共享在 `di/DynamicFeedSlots.kt`：只有「全部」分类请求 UP/直播 portal，其它分类订阅同一 StateFlow；账号切换会让非「全部」槽重建。Miuix 下拉菜单外壳统一在 `TeakUI` 的 `TeakAnchoredGlassMenu`，`TeakGlassMenu` 与 `TeakVideoActionMenu` 都只是薄封装；热门页菜单必须常驻到收起动画结束，不能再被 `?.let` 整块卸载。

`PopularPageScreen` 不再直接 `AppGraph.api.addWatchLater`，走 `AppGraph.videoActions` 的仓储接口，避免表现层直接依赖基础设施。

### 2026-11-22 图片查看器改为 Telegram 式窗口内单 Bitmap 转场

查看器不再用 Dialog：`PhotoViewerRoot` 直接画在当前窗口最上层，楼中楼在自己的 Dialog 内容里再包一层宿主。入场/退场由 `PhotoMorphView`（一个自定义 View + 一个 ValueAnimator）用列表已缓存的缩略图 bitmap 跑 200ms AccelerateDecelerate；动画期间不组合 pager、不解码原图。动画结束后缩略图继续盖住查看器，等 `fullAlpha` 真正到 1 再淡出盖层（`markCoverReady`），避免原图首次加载时闪一帧；超时 4s 兜底。列表缩略图在 `PhotoThumbCache` 里按原始 URL 缓存（LruCache 上限 64MB，sizeOf 单位是 KB）。不要改回 Dialog 或“先显示 pager 再等原图”。

### 2026-11-23 评论 AI 人格屏蔽（大肥鱼规则）
评论区新增 AI 人格账号屏蔽，规则移植自 ybyz2/bilibili-dafeiyu-blocker（MIT，提交与边界见 `docs/DAFEIYU_BLOCK_PROVENANCE.md`）。纯判定在 `core:domain` 的 `CommentAiBlockPolicy`（账号名含肥鱼 → 品牌词 → 真 @ → 单字 → 全角标点，档位切段，置顶跳过，简繁靠字符类），过滤发生在 `CommentListStateHolder.fetch`：主评论与楼中楼（`root>0`）共用同一状态容器，因此一处接线两边都生效。整页被屏蔽时最多补取 3 页再结束，避免首屏空白。

设置落在 `MessageSettings`：`comment_ai_block` / `comment_ai_block_strength` / `comment_ai_block_reply_mode`，默认关闭（未开启前行为与上一版完全一致）。`CommentStateFactory` 收 `() -> CommentAiBlockConfig`，由 AppContainer 在每次拉取时现读 `settings.repository.message`，不做组合期快照。删除/拉黑、计数横幅、自定义屏蔽词/用户名都没有接入，不要顺手把写操作接进来。

### 2026-11-25 标签胶囊滑动指示器 / 页内内容切换 / 查看器与楼中楼输入

`TeakUI/navigation/TeakSlidingChipRow` 是可复用的「分开胶囊 + 会滑动的选中底色」控件，已在直播分区、收藏夹、历史分类、空间投稿（视频/图文/合集与系列）接入。实现要点，改它之前先读：

- 位置在组合期用 `TextMeasurer` 量好（`LocalTextStyle` 合并后的样式，不能另 new 一个），坐标是内容坐标，滚动时不需要重新测量；胶囊宽度用 px 写死（`chipSize`），px→dp→px 会逐枚累计误差，粉色块就和胶囊对不齐。
- 灰底与粉色块都由 Row 的 `drawBehind` 画，粉色块按**每枚胶囊的圆角路径裁切**：标签之间的空隙必须被盖住，不能在整行上直接擦过去；高度始终等于胶囊高度，禁止再压缩高度（会在标签内留出上下白边）。
- 动效：位置用一根带回弹的弹簧（距离越远阻尼越大），位移速度换算成横向拉伸（对称鼓出、停下收回），选中时**整枚胶囊**先缩到 0.94 再回原尺寸（不是放大、也不是只缩文字）；高度始终等于胶囊高度，不做纵向压缩。
- 首帧不播动画；`TeakMotion.enabled=false` 时所有值直接落到目标。

分段控件（`AdaptiveScrollableTabs` / `TeakPagerTabs`）是另一套实现，本轮没有改、也不要为了这个胶囊去改它。

页内标签切换的内容过渡在 `presentation/common/ContentSwitch`：`rememberContentSwitch(key, direction)` 只按 key 变化重播一次 320ms 的滑入 + 淡入，列表项通过 `Modifier.contentSwitchItem()` 读取**同一份**进度；不要把动画写进每个列表项，否则滚动时新组合出来的卡片会再播一次。直播、收藏/历史/稍后再看已接入，顶部标签与关注行保持不动。

图片查看器：双击以点击点为中心放大（倍率仍 2.5，补间 220ms，公式见 `PhotoViewerRotation.kt` 与单测）；已删除打开时强制 `SCREEN_ORIENTATION_SENSOR` 的自动旋转，改为底部中间的手动旋转键（`MiuixIcons.RotateLeft`），旋转角度存在 `PhotoViewerTransitionState.rotationQuarter`，翻页/关闭时归零。

楼中楼仍是独立 Dialog 窗口（`CommentThreadDialog` + `ui/comment/CommentThreadWindow.kt` 负责窗口起点）：曾试过改成页面覆盖层（`CommentThreadOverlay`）让输入栏浮在最上层，但展开后顶部标题/关闭键会被页面 Tab 行压住、输入栏层级与玻璃采样也出问题，已整体回退。不要再把它改成页内覆盖层，除非能同时解决 Tab 遮挡和玻璃自采样。表情输入保留在楼中楼自己的输入栏里（`ComposerIcon.Emoji` + `EmojiPanel`）。

标签胶囊的选中反馈是整枚胶囊一起缩（先缩到 0.93 再回原尺寸），不是只缩文字、也不是放大：灰底和粉色块都画在每枚标签自己的 draw 里，否则胶囊不跟着缩放。

### 2026-11-26 设备分类、菜单跟手与加载态

设备分类统一用 `core:domain/DeviceClassPolicy.kt` 的 `isTabletDevice(smallestScreenWidthDp)`（sw600dp；不要用旋转后的 `screenWidthDp`）。`BdFunActivity` 在手机创建时锁竖屏，平板/展开折叠屏不干预；视频页退出全屏由 `PlayerWindowEffects` 恢复手机竖屏、平板 `USER`。视频 70/30 分栏现在同时要求 `smallestWidthDp >= 600`，手机横屏只能进入全屏播放器，不能再出现平板分栏。

`TeakAnchoredGlassMenu` 的退场保活窗口在 `visible=false` 时加 `FLAG_NOT_TOUCHABLE|FLAG_NOT_FOCUSABLE`，动画继续播但下一次触摸/输入法焦点立即交还页面；评论菜单的回复和自由复制改为先执行动作、只延迟清理 target，不要再在动作前 `delay(360)`。

视频页评论输入栏改为按 `pager.currentPage + currentPageOffsetFraction` 连续平移，位移必须在 `graphicsLayer` 里读，不能把 pager 偏移读进组合期。评论 @ 统一普通用户灰、大会员主题色；折叠回复在用户名和「回复」之间补空格。

图片查看器把状态读取收进 `PhotoViewerHost`，打开时只重组 Host，不带着整页 content；双击缩放改用与搜索一镜到底相同的弹簧曲线（0.97 / 247），缩放过程直接写 `view.zoom`，不要恢复 `LaunchedEffect(page.zoom)` 逐帧重启。首次打开仍需在真机看帧，不能再新增常驻全屏 AndroidView 除非有测量收益。

首页分页 footer 用 `TeakLoadingIndicator` + “正在刷新…”；`TeakLoadingRow`/`TeakLoadingArea`/`PageCenteredLoading` 默认带“正在加载…”，按钮和卡片内的小图标传 `label = null`。空间投稿分类标签补 12dp 起始内边距以对齐收藏夹；评分选中星用 `MiuixIcons.FavoritesFill`；消息页点进“回复与@”先本地清 reply/at 红点，不调服务端。\n
### 2026-11-26 Miuix 0.9.5 升级与图片查看器动效

Miuix 源码固定到上游 \c1024d78a154f5167efff262ae5ed66264c481e\（0.9.5 线，AGP 9.5.3 / Kotlin 2.4.21 / Compose 1.13.0-rc01）；\miuix-shader\ 已并入 \miuix-material\，\GlassDefaults/GlassShape/GlassMaterials/EdgeStroke/RuntimeShader\ 改从 \	op.yukonga.miuix.kmp.material\ 导入，\overScrollVertical\ 的 estedScrollToParent\ 参数移除。主工程与所有 Android 库模块统一 \compileSdk 37.2\（\minorApiLevel = 2\），否则 Compose 1.13 的 AAR 元数据会拒绝构建。两个本地补丁（NavDisplay 的 \predictiveBackPreview\、GlassPopupSurface 退场立即释放输入）已按新文件重新生成，升级快照时必须同步刷新 \	ools/miuix-patches\。
图片查看器：入场改为 HyperOS 4 搜索展开同款 \spring(0.97, 0.4s)\ 非线性速率（\PhotoViewerMotion\，归一化到 0/1），关闭用同一曲线 0.3s；\PhotoMorphView\ 预创建为 1dp 热视图，首次打开不再现场实例化。长按菜单收起时通过 Dialog \FLAG_NOT_TOUCHABLE\ 立即释放输入，GlassPopupSurface 非交互态不再挂全屏消费层；查看器退出会先撤菜单与 backdrop 再回落，消除闪一帧。

### 2026-11-27 大标题一镜到底、点顶栏回顶与视频转屏

大标题仍画在顶栏前景（不进 backdrop 录制，避免 progressive blur 二次采样）；页面内容自己只对局部做退让时（设置页搜索要留住底部搜索框和结果面板），由内容侧调用 `TeakLargeTitleSearchUnderlay(progress, alpha, blur)` 把同一份变换登记给 `TeakLargeTitleAnchorState`，吸顶大标题在绘制阶段按同一位移/淡出/模糊跟随。设置页搜索的空态没有标题，提示固定在屏幕中间，列表从 `TopBarHeight` 之下开始。

搜索页 chrome 与旧页 chrome 是两层：外壳按 `searchTitle`(可为空串) + 搜索退让进度画第二份顶栏/返回键，旧的一份缩到 0.9 淡出、新的一份从下方抬入（`SearchedChromeRise`），不再复用同一个返回键。`TeakTopBar` 不再有 `overlayTitle`，宽屏走 `WideFoldTopAppBar` 自己的交叉淡出。

点顶栏回顶（`TeakStatusBarScrollToTop`）常开无开关：铺在状态栏 + TopBarHeight 上并画在顶栏按钮下面，返回键/搜索框/分段优先；因为系统状态栏窗口会吃掉最上面那一段点击，实际可点区域是顶栏空白处。

视频转屏：`rotationTargetForPose` 在 `core:domain`，半屏时紧凑手机（smallestWidthDp<600）转到横屏才请求横屏，靠 `fullscreenAfterVideoRotation` 进全屏；已全屏时姿态两向跟随，平板/关闭自动旋转/锁方向都不干预。`VideoActivity` 的 idle 锁必须用 `autoLandscapePending()` 让路，否则 onConfigurationChanged 会把刚请求的横屏立刻锁回竖屏。

### 2026-11-27 首页撤销态、哔哩哔哩分段弹簧、视频标签展开与搜索勾选尺寸

首页卡片点“不感兴趣”后的撤销态不再用固定高度占位：`GridVideoContent`/`LargeVideoContent` 会把正常标题与 UP 行整块以 `alpha=0` 保留，再把“撤销”盖在 `matchParentSize` 的居中 Box 上。这样文字落在整块文字区域的几何中心，卡片高度也与正常态逐像素一致，不要再改回 `Spacer(22.dp)` 之类的固定高度。

哔哩哔哩（interfaceStyle=0）布局的顶部分段不传 pager 连续位置，走 `AdaptiveScrollableTabs` 的无 position 分支：指示器用 `spring(DampingRatioLowBouncy, StiffnessMediumLow)`，与悬浮底栏一致；经典布局仍保留连续位置，HyperOS 4 的内联分段不变。官方横屏走同一份 `HomeTopBarTabs`，position 同样为空。

视频标签必须作为简介的尾部内容传给 `CollapsibleDescription(trailing=...)`，由 `SubcomposeLayout` 把正文和标签一起量高、共用同一段 420ms 裁剪高度动画；禁止再给标签套独立的 `AnimatedVisibility`/fade，否则会出现“标签先撑开、正文后收缩”的两套动画。收起高度始终是两行正文高度，标签只在展开后随整块高度出现。

搜索历史编辑态的 Miuix `Checkbox` 内部用 `requiredSize(26.dp)` 固定尺寸，外层 `.size()` 不生效；要缩小只能用 `scale`。当前按一半显示（0.5 倍 = 13dp），并用 offset 补偿缩放前后的位置；未勾选底色用 `onBackground.copy(alpha=0.10f)`，不要再用纯白。

“不感兴趣”成功后不再弹 toast：提示写进 `HomeFeedState.undoMessages`（bvid -> 文案），由 `LocalHomeUndoMessages` 投到对应模糊封面的 `BlurredCoverNotice`（白字+阴影、最多两行）；撤销按钮左侧用 `MiuixIcons.Undo`。多张卡可以同时待撤销，每张保留自己的文案和撤销动作，`HomeFeedStateHolder.pendingDismisses` 也按 bvid 分开存，禁止再改回单个 `undoVideo`/`PendingDismiss`。撤销调用 `RecommendationActionsRepository.cancelDislike(video, reason)` 走 App 端 `/x/feed/dislike/cancel`，参数必须和提交时一致（`goto`、`id`、`reason_id`、`build`、`mobi_app` 再签名），成功后才清除该卡；失败只把该卡的封面提示改成重试文案。

“不感兴趣/撤销”不能瞬间切模糊：封面 blur 半径用 `animateFloatAsState` 0→48dp，播放量/时长等覆盖层按 `1-blurProgress` 淡出，模糊提示按 `blurProgress` 淡入；标题区和撤销按钮各用 240ms alpha 动画。新增卡片状态时保持这套渐变，不要恢复硬切。

搜索历史删除使用粒子飘散：通用实现在 TeakUI `common/motion/TeakParticleFlow.kt`（`TeakParticleFlow` + `TeakParticleDefaults`），业务侧只剩 `HistoryPillsFlow` 接线（`ui/search/SearchHistoryParticles.kt`）。组件删除前用 `onGloballyPositioned` 记住每项 bounds，数据变化时对消失项在原位置炸出小方块粒子，剩余项用 `LookaheadScope + Modifier.animateBounds` 滑到新位置，卡片高度用 `animateContentSize` 一起收；粒子调色板由 `burstColor` 按被删项现算（只取胶囊自己的底色 `surfaceContainerHigh`，层次靠同色透明度；**不要**混 `primary`/`onBackground`，灰色胶囊会炸出红黑碎屑）。每项必须按 key 分组，否则删中间项时 Compose 复用槽位，后一项会“原地换字”而不是滑动。MIUIX 0.9.5-rc01 **没有**任何粒子/删除动效组件，不要再去找。

长按/省略号形变菜单挂在主壳玻璃采样树外，不随首页 pager 离开；底栏 `onBarItem` 必须先调用 `HomeFeedState.dismissMenu()`（即 `menuState.close()`），否则菜单会停在新页面并用旧锚点错位。

### 2026-11-27 搜索历史粒子下沉 TeakUI

先核对过 MIUIX 再动手：固定快照 `bc1024d`（0.9.5-rc01）与仓库里的新版快照 `45645854` 的全部 Kotlin 源码、`miuix-src` 的 git 全历史（`git log -S particle`）以及 `docs/components/**` 组件目录，都**没有**粒子飘散或删除类动效组件。搜索相关的只有 `SearchBar` / `InputField` / `SearchClearButton` / `GlassSearchOverlay`，`MiuixIcons.Basic.SearchCleanup` 是一张静态图标，MIUIX 自身连 `Modifier.animateItem` 都不用。所以这个动画只能自研，别再按“MIUIX 里有”去找。

自研实现已从业务包下沉到 TeakUI `common/motion/TeakParticleFlow.kt`：`TeakParticleFlow` 负责按 key 记录每项 bounds、删除时在原位置爆炸、剩余项 `LookaheadScope + Modifier.animateBounds` 滑动；参数集中在 `TeakParticleDefaults`；`TeakMotion.enabled=false` 时跳过粒子并让重排直接落位。业务侧 `HistoryPillsFlow`（`ui/search/SearchHistoryParticles.kt`）只保留胶囊内容与 `burstColor` 取色——**只取被删胶囊自己的底色**（`surfaceContainerHigh`），层次靠同色透明度；不要混 `primary` 或 `onBackground`，灰色胶囊会炸出红/黑碎屑。其他列表要同样的删除过渡时复用 `TeakParticleFlow`，不要再往业务包里复制一份 Canvas 粒子。

### 2026-11-27 视频分P整页列表与动态页 UP 模式

分P：`VideoPagesSection` 必须自己套一层 `Column`——调用方把它放在 `Box` 里，`Row`/`Spacer`/`VideoPageStrip` 若直接当 Box 的三个子项会全部重叠，横向分P条会盖住「分P」行，看起来就是"没有分P字样、也没有箭头、点不开 sheet"。标题行不带卡片底色和横向内边距，让「分P」和分P首卡左边缘对齐、箭头压右边缘；上下留白要小。sheet 必须**常驻组合**、用 `show` 控制（不要 `if (open)` 包住），否则自带的收起动画来不及播。列表每项封面用 `pages[].first_frame`（`VideoPage.firstFrame`，映射在 `VideoDetailMapper`，回退整稿 `pic`，与 PiliPlus `episode_panel` 一致）；右上角回顶/回底/定位与合集标题栏同尺寸（24dp 圆 + 14dp 图标）。

动态页 UP 模式：选中某个 UP 后 `HorizontalPager` 锁住横向滑动（`userScrollEnabled=false`），改由 `FeedScreen.feedUpModeSwipe` 左右滑切换上一个/下一个 UP（顺序见 `feedUpMids`，不含「全部动态」）；返回键、点顶部分段（含点当前项，靠 `shell.ui.dynamicTabClickTick` 记数）、点「全部动态」都退出，退出时用 `FeedState.exitUpFocus` → `switchTo(tabKey)` 回到该分段自己的分类，**不要**在视频/番剧分段里统一切成 all。选中环 `FeedAvatarRingState` 必须由页面（`FeedTabPage`）持有、动画 `moveTo` 由头像卡片里的 `LaunchedEffect(selectedKey)` 驱动，不能挂在懒列表里的头像卡片上当状态，否则过滤一变卡片重建就"瞬间跳变"；环在源头像底部解体、沿直线滑到目标底部再缠绕成圈。**不要用 `PathMeasure` 截窗口**：它只返回"当前 contour"的长度，`arcTo(±360°) + lineTo + arcTo` 会变成多 contour，`measure.length` 只等于圆周长，`getSegment` 直接画不出东西（表现为环消失再出现＝瞬间跳变）。现在用 `drawArc` + `drawLine` 分两段画：前半段源环 sweep 从 360 收到 0、线从源底部伸出；后半段线收进目标底部、目标环 sweep 从 0 涨到 360。锚点用 `boundsInRoot`、覆盖层用 `positionInRoot` 换算；滚出视口的锚点必须在 `DisposableEffect.onDispose` 里删掉。**切换时不要自动把选中头像滚到第一位**——已在视口内就不动，目标在屏幕外才往前滚一屏（落在最后一格），退出 UP 模式才滚回行首；否则头像行边滑、环的起止坐标被冻结，看起来就是跳变。

### 2026-11-27 动态卡片菜单与 UP 环出屏方向

环的方向必须是**显式记忆**，不能等播动画时再拿旧坐标猜：`FeedAvatarRingState` 在 `onAnchor`/`clearAnchor` 里用**卡片内坐标**和覆盖层宽度算出 `FeedRingSide`（左/右）；源头像被回收后，起点圆由 `feedRingOffscreenStart` 完全放到该侧视口外、纵向对齐目标行，前半段只画线（`startOffscreen` 时跳过源环），线才会真的从边缘滑进来。两个关键坑：①`anchors` 是 root 坐标，判断左右必须先 `toLocal()`，宽屏带侧栏时拿 root 坐标和卡片宽度比会差整个侧栏；②头像也会因为**整张头像卡纵向滚出列表**被回收，那时最后一次测量是完整可见的，不能按它的水平位置猜左右，只能用「头像行此刻的滚动方向」兜底（`AvatarRowScrollState`），拿不到就保持未知。切换前会为把目标滚进视口而滚动头像行：**只有源头像滚前还有实时锚点**时，才用滚动方向推断出屏侧；源头像本来就在屏幕外时，滚动方向会把它猜反，必须保留记忆。几何规则全在 `FeedRingGeometry.kt`，有单测。

UP 之间切换必须用**真正的 `HorizontalPager`**（和空间页 `SpaceContentPager` 同一套：`beyondViewportPageCount = 1`、`overscrollEffect = null`、默认翻页物理），**不要再手写阻尼/位移/阈值**——手感对不上，拖动时也没有邻页。`FeedUpPagerLayout` 把头像行放在 pager **外面**固定住（窄屏固定在顶部，宽屏用固定作者侧栏），所以 UP 栏不参与翻页、也不会跟着页面滑走；每个 UP 是 pager 的一页，页内只放动态卡片（`FeedCardsGrid`）。**停稳（`settledPage`）后才提交 `filterMid`**；点选头像只让 pager 滑过去，不直接改筛选，否则正在看的那页会在滑动中途变成预览/骨架。相邻页用 `FeedState.previewItems` + `prefetchUp` 预取首屏（`FeedStateHolder.prefetchUp` 写回同一份 cache，停稳后 `filterTo` 直接命中、不二次请求），离开某个 UP 时 `seedPreview` 把当前首屏留下，往回滑不白屏。宽屏作者侧栏的点击也必须走 pager 的 `selectUp`。UP 模式的下拉刷新/分页/自动播放只作用于常驻（live）页，预览页只读。切页时不要再给整个内容挂 `graphicsLayer`/快照/alpha：那会闪帧并搬走 UP 栏。

动态卡片菜单：省略号在作者行最右（与头像垂直居中），长按整卡；两者都复用首页的 `TeakMorphMenuHost` + 形变锚点（省略号从图标长、长按从手指位置长且不画图标副本），宿主必须挂在主壳玻璃采样树之外的 `MainFeedChrome`。每张卡有独立 `TeakMorphMenuState`，页面级只有一份长按点锚点（`FeedCardMenuUi`）。条目固定为：添加至稍后再看、复制、关闭自动播放、分享、取消关注、举报，后两项用 `0xFFE5484D`；「关闭自动播放」写 `settings.dynamicAutoPlay=false`。举报走 `FeedPageRepository.reportDynamic`，端点和参数对齐 PiliPlus `UserHttp.dynamicReport`（`/x/dynamic/feed/dynamic_report/add`，`accused_uid`/`dynamic_id`/`reason_type`/`reason_desc`），理由编号在 `core/domain/DynamicReportReason`；「其他」需要文字说明，暂不提供。切换底栏或顶部分类时先 `FeedState.dismissMenu()`，否则菜单会停在新页面并沿用旧锚点。

`FeedGrid.kt`/`FeedImageGrid.kt` 在 HEAD 里带 UTF-8 BOM，Kotlin PSI 尺寸审计会把它当语法错误（`Expecting a top level declaration`）；checkout 后必须去掉 BOM，否则 `checkLayering` 永远红。当前 `checkKotlinSizes` 仍剩 4 条 HEAD 自带超标（`LargeVideoContent` 86 行、`VideoGridCard` 16 参、`VideoLargeCard` 13 参、`HomeVideoMorphHost` 93 行），正在 PR 分支处理，不是本轮新增。

### 2026-11-28 一镜到底大标题收缩、删除粒子、动态 UP 页

一镜到底期间页面大标题不再跟内容上移，改为原位向中心收缩（TeakSearchTransitionDefaults.LargeTitleShrink）；改动在 TeakLargeTitleForeground，收藏/历史/稍后再看（PageUnderlayOptions）与设置页搜索（TeakLargeTitleSearchUnderlay）共用同一条路径。删除粒子下沉为 TeakUI 的 TeakParticleBurst：卡片登记窗口矩形与自己的颜色（视频卡片取封面平均色，coverAverageColor 必须处理 Config#HARDWARE，否则 getPixel 直接崩），数据里真的少了一项才炸，爆炸范围按被删项矩形铺开，切收藏夹/分类只重置基线。动态页点头像改成开新 UP 页（FeedUpTransition，与搜索一镜到底同一条弹簧）：窄屏顶栏是悬浮头像行 + 页面自建 backdrop 的渐进模糊，无圆角卡片、无分类分段、不排直播头像，「全部动态」为退出键；返回键、全部动态、顶栏下沿的下拉条是唯三退出入口（下拉条跟手倒放一镜到底：慢拉按位移逐帧倒放，快甩直接播完）；顶部「全部/视频/番剧」分段与 UP 页共用同一份转场进度；环只在 UP 页参与动画（进页 snapTo，切换才播环绕），动态页的环固定在「全部动态」。服务端分类与 host_mid 互斥，UP 页的「视频/番剧」用 eedItemsForTab 在客户端筛。

D
### 2026-11-28 全屏评论面板（PR #157 收尾）

- 入场动画：`FullscreenCommentPanel` 的 `revealed` 初值改为 `false`。宿主只在 `panelVisible` 时组合它，初值取 `visible`（=true）会让 `rememberManualProgress` 起始值等于目标值，264ms 滑出动画一帧都不播（只有关闭方向有）；现在打开和关闭都是同一条时间线。
- 已知坑补录：全屏评论面板画在录制树之上，内部玻璃控件再采样 Activity 根部 `LayerBackdrop` 会自引用并 SIGSEGV（真机 `RenderThread` 栈溢出）。面板内 `LocalTeakBackdrop provides null` 是当前解法，根治要把浮层挂到录制层之外。
- 去掉未使用的 `FullscreenCommentIdentity.isPgc`（oid/type 规则以 `core.domain.videoCommentTarget` 为准），补两个新文件的末尾换行。
- 验证：PR 分支合并当前 main 后 `:app:assembleDebug`、`:app:testDebugUnitTest`、`:core:domain:test` 全过。真机走查项见 PR #157 描述（横屏点评论、楼中楼、关闭仍横屏、关掉互动按钮后「更多」可用、竖屏不变）。

### 2026-11-28 顶栏材质收缝、UP 页头像名称淡出

- 顶栏渐进模糊的配方（8dp 半径、0.94 染色、60% 起点、trim 18dp）没改。改的是三处**边界**：
  1. 材质比顶栏内容高三分之一，用来收尾底部模糊；调用方原先把半透明层按顶栏高度套 `graphicsLayer { alpha < 1 }`（搜索一镜到底、切首页三页、进出 UP 页、我的页换页），HWUI 按节点边界裁剪，裁口落在渐变还有强度处，滚动画面穿过就是硬边。`ProgressiveTopBarMaterial` 现在回调材质高度（`onMaterialHeightPx`），`MainTopBarChrome`/`MainMineChrome` 把半透明层撑到同高。
  2. 材质末端 12%（`ProgressiveTopBarEdgeFadeStart = 0.88`）用 `progressiveTopBarEdgeLayer` 把采样像素交还给正文：模糊半径到 0 的那一段不再以半透明糊片压在滚动内容上。这是本轮唯一的观感微调，范围只在顶栏下沿之外。
  3. 一镜到底不再给顶栏补 `underlayLiftPx/underlayBlurPx`；`layerBackdrop` 挪到 `teakSearchUnderlay` 外层（`MainScaffold` / `TeakPageScaffold`），录的是**已经上移并模糊**的正文，顶栏和屏幕用同一份画面。
- UP 页顶栏（窄屏头像行）的模糊改为滚动驱动：停在顶部时材质值为 0（没有模糊），离开顶部后 260ms 补到 1（`rememberFeedUpHeaderProgress`）；`teakProgressiveBarBlur(materialProgress = ...)` 接这个进度。头像下面的**名称**（`AuthorAvatar`/`AllFeedAvatar`/`LiveAvatar` 的那行小字）通过 `LocalFeedAvatarLabelFade` 拿同一个 State，`feedAvatarLabelFade` 只改绘制期 alpha，**不动布局**，所以行高、列表顶部留白、卡片位置都不变，名字和模糊是同一拍。
- 验证：`:app:assembleDebug`、`:app:testDebugUnitTest`、`:TeakUI:testDebugUnitTest` 通过。装机核对（MuMu player 2 / `127.0.0.1:16448`，1220×2656）：UP 页顶部名称可见且顶栏无模糊；上滑后名称淡出、模糊淡入、卡片位置不跳；首页滚动后的顶栏模糊正常；搜索展开正常。一镜到底过程中的单帧接缝仍需人工慢放确认（截图抓不到那一帧）。
