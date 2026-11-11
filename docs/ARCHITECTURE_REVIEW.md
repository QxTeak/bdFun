# bdFun 架构审查与分层重构方案（DDD 视角）

> 审查对象：`D:\work\bdFun`（提交到 GitHub 的干净源码）
> 审查日期：2026-11-11
> 审查方式：通读模块划分、统计代码规模与依赖方向、抽查关键文件（BiliApi / VideoScreen / VideoPlayer / SettingsStore / AppGraph / 各 Activity）

---

## 0. 一句话结论

**代码本身质量不差，问题是"没有边界"**：一个 3758 行的网络类同时干着"发请求、签名、解析 JSON、选清晰度、挑 CDN、缓存结果"六件事；一个 1701 行的界面函数同时管着播放、收藏、评论、旋转、弹幕、状态恢复。

所以拆分的目标不是"换框架"，而是**把三样东西各自归位**：

1. **规则**（什么算可以播放、清晰度怎么选、推荐怎么去重）→ 领域层，纯 Kotlin，能单测；
2. **数据从哪来**（B 站接口、本地设置、下载缓存）→ 基础设施层，可替换；
3. **界面怎么显示**（Compose 长什么样）→ 表现层，只负责画和转发用户动作。

---

## 1. 现状盘点（数字都是实测）

| 项目 | 规模 |
|---|---|
| Kotlin 文件总数 | 271 个，约 63,700 行 |
| `:app` 模块 | 130 个文件，44,390 行 |
| `:TeakUI` 模块 | 45 个文件，6,243 行 |
| `:danmaku-engine` 模块 | 65 个文件，8,336 行（源码含 Java） |
| Activity | 20 个 |
| `ui/` 下的 Compose 文件 | 76 个 |
| `data/` 下的文件 | 20 个 |
| `BiliApi.kt` | **3,758 行，102 个公开方法** |
| `SettingsStore.kt` | **888 行，81 个设置字段**（全部在一个类里） |
| UI 层直接调用 `api.xxx()` | **115 处**（另有 Activity 里 34 处） |
| UI 层直接读 `AppGraph.settings` | **63 处** |
| ViewModel / StateFlow / Repository / UseCase | **0 处**（全项目搜索为 0） |
| 进程级全局 `object`（含各种 Bus） | **23 个** |
| 单元测试 | app 7 个、TeakUI 2 个、danmaku-engine 19 个 |

超长函数排行（单个函数体行数）：

| 函数 | 行数 |
|---|---|
| `VideoScreen()` | **1,701** |
| `VideoPlayer()` | **1,680** |
| `SpaceScreen()` | **1,076** |
| `CommentThreadSheet()` | 706 |
| `CommentScreen()` | 653 |
| `VideoActivity` 类整体 | 598 |
| `LibraryScreen()` | 534 |
| `BdFunApp()` | 523 |
| `SettingsScreen()` | 490 |
| `BackgroundPlayDisc()` | 384 |

---

## 2. 现状架构图

```
┌───────────────────────────────────────────────────────────────────────────┐
│  :app  (130 文件 / 44,390 行)                                              │
│                                                                           │
│  ┌───────────────┐        ┌────────────────────────────────────────────┐  │
│  │ 20 个 Activity │        │ ui/  76 个 Compose 文件                     │  │
│  │ 生命周期、旋转  │───────▶│  VideoScreen 1701 行 / VideoPlayer 1680 行  │  │
│  │ PiP、媒体键、   │        │  SpaceScreen 1076 行 / Comment 653 行 ...   │  │
│  │ 通知、下载服务  │        │                                            │  │
│  └───────┬───────┘        │  状态 = remember { mutableStateOf(...) }    │  │
│          │                │  取数 = 直接写 api.xxx()      ← 115 处      │  │
│          │                │  配置 = AppGraph.settings.xxx ← 63 处      │  │
│          │                └────────────────────────────────────────────┘  │
│          │                              │                                  │
│          ▼                              ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ data/BiliApi.kt   3,758 行 / 102 个方法   ← 一个类承担 6 种职责       │ │
│  │  HTTP 请求 · WBI 签名 · Cookie · JSON 解析 · 业务规则 · CDN 选路      │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐  │
│  │ SettingsStore │ │ VideoCacheMgr │ │ Background    │ │ 23 个全局       │  │
│  │ 81 个设置项    │ │ 下载/缓存      │ │ Playback      │ │ object / Bus    │  │
│  └───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ AppGraph：全局可变单例 + pendingVideo / pendingDynamic / pendingSpace  │ │
│  │            ← 用它来"偷偷"在页面之间传参                                │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────┘
        │                                          │
        ▼                                          ▼
┌──────────────────────┐                 ┌──────────────────────────┐
│ :TeakUI  组件库       │                 │ :danmaku-engine          │
│ 已独立、有规范文档     │                 │ 分层清晰、19 个测试       │
│ （这块是好的）         │                 │ （这块也是好的）          │
└──────────────────────┘                 └──────────────────────────┘
```

**依赖方向**：所有箭头都是 `UI → API`、`UI → 全局单例`。没有一处是"接口在里、实现在外"。这意味着：**你没法在没有网络、没有 Android 的环境里测试任何业务规则**，也没法把"推荐流"换成别的数据源而不动界面。

### 2.1 六个具体病灶

**病灶 1：`BiliApi` 是"万能柜台"**
一个类 3,758 行，从"发 HTTP"到"这块视频该用哪个清晰度"全包。类比：一家餐厅的前台既点菜、又炒菜、又收钱、又洗碗。
后果：改"清晰度选择逻辑"要在一个充满网络代码的文件里翻；任何一个接口字段变化都可能牵动其它方法；无法单独测试。

**病灶 2：界面直接做业务**
`VideoScreen()` 1,701 行里，同时存在：拉详情、拉播放地址、拉弹幕、拉相关推荐、点赞、投币、收藏、关注、选清晰度、记忆进度、自动暂停、恢复播放、评论跳转。
后果：改一个按钮的样式，要读懂一整套播放逻辑；两个人同时改这个文件必然冲突。

**病灶 3：状态存在 Composable 的 `remember` 里**
37 个 `mutableStateOf` 挤在 `VideoScreen` 里。
后果：屏幕一转、进程被杀、从后台回来，状态就散架。现在靠 `HomeFeedMemory`、`SpaceCache`、`RelatedMemory` 这些"内存补丁"去救，补丁越打越多。

**病灶 4：23 个全局 `object` + 各种 `Bus`**
`VideoPageBus`、`LibraryPageBus`、`MessagePageBus`、`ScrollTopBus`、`PlayerUiState`、`BackgroundPlayback`……
类比：办公室没有内部电话，大家靠"在走廊上喊"沟通。
后果：谁改了状态、什么时候改的，无法追踪；测试时无法隔离（跑一个测试会污染另一个）。

**病灶 5：`AppGraph` 兼职导航参数**
`pendingVideo` / `pendingDynamic` / `pendingSpaceMid` 写在全局单例里，下一个 Activity 自己去取。
后果：页面的入参变成"隐式"的，少写一行就打开空白页；返回栈和参数容易错位。

**病灶 6：配置和业务混在一个 888 行的类**
`SettingsStore` 81 个字段，涵盖外观、播放、弹幕、消息、缓存、引导。
后果：任何一处想读"播放速度"，都要依赖整个设置仓库；测试要构造全部 81 个字段。

---

## 3. DDD 在这类客户端里怎么用（以及不该怎么用）

先把话说白：
- **DDD 的核心不是"分四层文件夹"，而是"业务规则不依赖外部细节"**。只有这条做到了，分层才有意义。
- 手机 App 没有"多个部门共同维护的复杂账本"，所以**不需要**聚合根、领域事件、事件溯源、CQRS 那一套重装备。
- 真正有用的是三件：**限界上下文（按业务切块）**、**依赖倒置（接口在里面、实现在外面）**、**状态归属明确（谁的孩子谁抱）**。

### 3.1 限界上下文（Bounded Context）划分

按"用户能感知到的功能块"切，而不是按技术切：

| 上下文 | 用户看到的东西 | 现在散落在哪 |
|---|---|---|
| 账号 / 登录 | 扫码登录、登录态、Cookie | BiliApi + LoginScreen + AppGraph.session |
| 视频详情 / 播放 | 播放页、清晰度、进度、字幕、倍速 | VideoScreen / VideoPlayer / BiliApi / VideoActivity |
| 互动 | 点赞、投币、收藏、关注、评论、举报 | VideoScreen / CommentScreen / BiliApi |
| 弹幕 | 弹幕显示、发送、过滤、样式 | danmaku-engine + VideoPlayer + SettingsStore |
| 信息流 / 动态 | 首页推荐、动态、排行、热门 | HomeScreen / DynamicScreen / BiliApi |
| 消息 / 私信 | 回复我的、@我的、私信、会话 | MessageScreen / PrivateChatActivity / BiliApi |
| 搜索 | 搜索联想、结果、筛选 | SearchExperience / BiliApi |
| 用户空间 | UP 主主页、投稿、专栏、合集 | SpaceActivity（1,076 行一个函数） |
| 媒体库 | 收藏夹、历史、稍后再看、离线缓存 | LibraryScreen / VideoCacheManager / CacheService |
| 直播 | 直播间、聊天（目前是 mock） | LiveRoomScreen / LiveRoomActivity |
| 设置 / 外观 | 主题、字体、玻璃材质、引导页 | SettingsStore / SettingsScreen / App.kt |

### 3.2 四层的职责（用"餐厅"打比方）

| 层 | 餐厅里的角色 | 代码里是什么 | 允许依赖 |
|---|---|---|---|
| **表现层 Presentation** | 服务员：点单、上菜、看客人表情 | Compose 屏幕、UI State、ViewModel / StateHolder | 只能依赖应用层 |
| **应用层 Application** | 领班：协调后厨，决定先做哪道菜 | UseCase、跨屏会话、播放器控制器 | 只能依赖领域层 |
| **领域层 Domain** | 菜谱：什么算一道合格的菜 | 实体、值对象、仓储接口、纯策略函数 | 不依赖任何东西（纯 Kotlin） |
| **基础设施层 Infrastructure** | 供应商：送菜、送调料 | B 站接口、JSON、数据库、ExoPlayer、文件 | 实现领域层接口 |

**唯一铁律**：箭头只能从外往里。领域层不知道"网络"和"Android"存在。

### 3.3 刻意不做的三件事

1. **不做 20 个 Gradle 模块开局**。模块多了编译变慢、样板代码翻倍。先在 `:app` 里按包分层，边界稳了再抽模块（第 7 节 P5）。
2. **不引入 Hilt 全家桶**。现在手动 DI（`AppContainer`）足够；等模块拆完再说。
3. **不为每个功能造聚合根**。`VideoItem` 这类对象是"共享内核"，被 6 个上下文共用，硬套聚合只会制造转换地狱。

---

## 4. 新架构图

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            :app  （组装壳，很薄）                          │
│   MainActivity · 路由表 · AppContainer（依赖注入）· 启动/升级/深链          │
└──────────────────────────────────────────────────────────────────────────┘
        │ 只做一件事：把下面的模块拼起来
        ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  feature 模块（每个都是一个限界上下文，竖着切到底）                          │
│                                                                          │
│  :feature:home      :feature:video      :feature:dynamic   :feature:search │
│  :feature:message   :feature:space      :feature:library   :feature:live   │
│  :feature:settings  :feature:onboarding                                    │
│                                                                          │
│  每个 feature 内部：                                                       │
│  ┌──────────────────────────┐   ┌──────────────────────────┐              │
│  │ presentation/            │   │ application/              │              │
│  │  XxxScreen (Compose)     │──▶│  XxxViewModel / StateHolder│             │
│  │  XxxSection / XxxCard    │   │  XxxUseCase               │              │
│  │  UiState (不可变数据)     │   │  （只这一个入口能碰领域）   │              │
│  └──────────────────────────┘   └───────────┬──────────────┘              │
└──────────────────────────────────────────────┼───────────────────────────┘
                                               │ 依赖接口
┌──────────────────────────────────────────────▼───────────────────────────┐
│  共享内核（core，按上下文分包，不做实现）                                  │
│                                                                          │
│  :core:model      实体 / 值对象（Video, Comment, Message, User, Quality） │
│  :core:domain     仓储接口 + 纯策略（PlaybackPolicy, FeedPolicy, ...）    │
│  :core:application 跨屏状态（Session, PlaybackController, DownloadQueue） │
│  :core:navigation 路由契约（Route + Navigator 接口，feature 之间不互相依赖）│
│  :core:common     结果类型、错误、日志、时间/数字格式化                    │
│  :core:ui         = 现在的 :TeakUI（设计系统，保持不变）                   │
└──────────────────────────────────────────────▲───────────────────────────┘
                                               │ 实现接口（依赖倒置）
┌──────────────────────────────────────────────┴───────────────────────────┐
│  基础设施（infra，只被 :app 在启动时装配，feature 看不见具体实现）          │
│                                                                          │
│  :infra:bili        B站接口分片：AccountApi / VideoApi / CommentApi /      │
│                     FeedApi / MessageApi / SpaceApi / SearchApi           │
│                     + BiliHttpClient + WbiSigner + CookieJar              │
│                     + dto/* + mapper/*（JSON 只活在这一层）               │
│  :infra:storage     Settings(DataStore) / WatchProgress / DownloadCache   │
│  :infra:playback    ExoPlayer 适配、后台播放 Service、媒体通知、PiP        │
│  （:infra:image 为可选项，现阶段 Coil 配置留在 :app）                       │
└──────────────────────────────────────────────────────────────────────────┘
        │                                        │
        ▼                                        ▼
┌──────────────────────┐              ┌──────────────────────────┐
│ :danmaku-engine      │              │ 外部：B站接口 / 本地存储  │
│ 保持现状（已分层）     │              │ ExoPlayer / 系统媒体      │
└──────────────────────┘              └──────────────────────────┘
```

### 4.1 依赖规则（可写进 CI 检查）

```
presentation ──▶ application ──▶ domain ──▶ (无依赖，纯 Kotlin)
      │                │            ▲
      │                │            │ 实现
      └──▶ core:ui     └──▶ core:model
                                    │
                            infra ──┘（只被 :app 引用）
```

| 规则 | 用一句话解释 | 违反时的症状 |
|---|---|---|
| 领域层不许出现 `import android.*` / `okhttp` / `org.json` | 菜谱不该提到"哪家供应商送菜" | 业务逻辑没法单测 |
| 表现层不许出现 `api.xxx()` | 服务员不许自己进后厨 | 就是现在这 115 处的样子 |
| feature 之间不许互相 import | 部门之间走正式流程，不串门 | 一个功能改动引发连锁编译错误 |
| 跨 feature 通信只走 `core:application` 的状态或 `core:navigation` 的契约 | 用公司邮箱，不在走廊喊 | 就是现在 23 个 Bus 的样子 |
| 每个屏幕 ≤ 400 行、每个函数 ≤ 80 行 | 一次能看懂的量 | 现在 1,701 行的样子 |

### 4.2 一次"打开视频"的流程对比

**现在**（所有事挤在一个函数里）：

```
VideoActivity.onCreate
  └─ VideoScreen(api, video, ...)            ← 1701 行
       ├─ LaunchedEffect { api.view() }      ← 网络
       ├─ LaunchedEffect { api.playInfo() }  ← 网络 + 选清晰度 + 选 CDN
       ├─ LaunchedEffect { api.danmaku() }   ← 网络 + 解析
       ├─ LaunchedEffect { api.related() }   ← 网络
       ├─ 80+ 个 remember 状态
       └─ 按钮点击 → 直接 api.likeVideo() / api.coinVideo() ...
```

**之后**：

```
VideoDetailScreen（只画）
  └─ 观察 VideoDetailUiState
       ▲
VideoDetailViewModel（管状态、管顺序、管失败重试）
  ├─ LoadVideoDetailUseCase ──▶ VideoRepository（接口）
  ├─ ResolvePlaybackUseCase ──▶ PlaybackRepository（接口）
  └─ ToggleLikeUseCase ──────▶ InteractionRepository（接口）
                                      ▲
                         BiliVideoRepository（实现，在 :infra:bili）
                         ├─ BiliHttpClient（重试、超时、Cookie）
                         ├─ WbiSigner（签名）
                         └─ VideoDto → Video（mapper，唯一知道 JSON 的地方）
```

好处不是"看起来整齐"，而是：
- 把 `BiliVideoRepository` 换成假的，就能**不联网跑完整播放页测试**；
- 清晰度规则写在 `PlaybackPolicy`，改规则只改一个纯函数；
- B 站接口改字段，只改 `mapper`，界面一行不动。

---

## 5. 新旧对比总表

| 维度 | 现在 | 新架构 |
|---|---|---|
| 切分方式 | 按技术切（`ui/`、`data/`） | 按业务切（feature）+ 内部按层切 |
| 依赖方向 | UI → API（侵入式） | UI → 应用 → 领域 ← 基础设施 |
| 业务规则位置 | 散在 API 和 Composable 里 | `:core:domain` 纯函数 |
| 状态归属 | `remember` + 23 个全局单例 | 每个屏幕一个 StateHolder；跨屏状态进 `core:application` |
| 网络细节 | `org.json` 全项目可见 | 只在 `:infra:bili` 的 dto/mapper |
| 页面传参 | `AppGraph.pendingXxx` 全局变量 | 显式 Route 参数 |
| 测试 | 只能测零碎纯函数（app 7 个测试） | 领域层纯 JVM 可测；仓储可用假实现 |
| 大文件 | 1,701 / 1,680 / 1,076 行 | 单文件 ≤ 400 行，单函数 ≤ 80 行 |
| 复用 | 复制粘贴为主 | `:core:ui` + 共享 model |
| 弹幕引擎 | 已是独立模块 ✅ | 保持不动 |
| 设计系统 | 已是独立模块 ✅ | 保持不动 |

---

## 6. 现有大文件的去向

| 现有文件 | 行数 | 新位置 |
|---|---|---|
| `data/BiliApi.kt` | 3,758 | 拆成 `:infra:bili` 下的 `BiliHttpClient` / `WbiSigner` / `AccountApi` / `VideoApi` / `CommentApi` / `FeedApi` / `MessageApi` / `SpaceApi` / `SearchApi` + `dto/` + `mapper/`；业务规则移到 `:core:domain` |
| `data/Models.kt` | 13 KB | 领域实体进 `:core:model`（按上下文分包）；接口 JSON 结构进 `:infra:bili/dto` |
| `data/SettingsStore.kt` | 888 | 接口 `SettingsRepository` 按 5 组拆（外观 / 播放 / 弹幕 / 消息 / 通用）+ `:infra:storage` 的 DataStore 实现 |
| `ui/VideoScreen.kt` | 1,701 | `:feature:video`：`VideoDetailScreen` + `VideoDetailViewModel` + `sections/*`（简介、相关推荐、选集、操作栏） |
| `ui/VideoPlayer.kt` | 1,680 | `:infra:playback`（ExoPlayer 封装、手势）+ `:feature:video` 的 `player/*`（界面） |
| `ui/FullscreenPlayerUi.kt` | 928 | `:feature:video/fullscreen/*`，拆成 `Chrome` / `panels/*` |
| `SpaceActivity.kt` | 1,076 | `:feature:space`：`SpaceScreen` + `SpaceViewModel` + `sections/*` |
| `ui/CommentScreen.kt` / `CommentThreadSheet.kt` | 653 / 706 | `:feature:video/comment/*`（评论属于视频上下文的子域） |
| `ui/LibraryScreen.kt` + `VideoCacheManager` + `CacheService` | 534 + 391 + 服务 | `:feature:library` + `:infra:storage/download` |
| `ui/DynamicScreen.kt` + `DynamicFeedState` | 321 + 状态类 | `:feature:dynamic`（现在的 `DynamicFeedState` 已是正确的 StateHolder，直接搬） |
| `App.kt`（`BdFunApp` 523 行） | | `:app/navigation/MainScaffold`，底栏/顶栏复用 `:core:ui` |
| `AppGraph.kt` | | `:app/AppContainer`（启动时装配）+ `:core:application` 的 `SessionStore` |
| `PlayerUiState` / `VideoPageBus` / `LibraryPageBus` / `MessagePageBus` / `ScrollTopBus` | | 改为 `:core:application` 的 `StateFlow`；页面间一次性事件改为 Route 参数 + `SharedFlow` |
| `BackgroundPlayback` / `BackgroundPlaybackService` / `MediaPlayback` | | `:infra:playback`（服务）+ `:core:application` 的 `PlaybackController` 接口 |
| `top/yukonga/.../bg/`（自带 miuix 背景特效） | 5 文件 | 归入 `:core:ui` 或独立 `:core:ui-effect`（与业务无关） |
| `:TeakUI` / `:danmaku-engine` | | **不动** |

---

## 7. 迁移路线（每步都能编译、能装机、能发布）

> 原则：不做"大爆炸重写"。用"绞杀者模式"——新壳子包旧代码，逐步把旧代码搬出去。

**P0 立骨架（2–3 天，风险极低）**
- 新增 `:core:model`、`:core:domain`、`:core:common` 三个纯 Kotlin 模块，同时建 `:core:application`、`:core:navigation` 两个空壳（为 P2/P4 预留落点）；
- `:app` 内建 `AppContainer`，把 `BiliApi` / `SettingsStore` 的创建收进去；
- 给 `BiliApi` 抽一个 `BiliRepository` 接口（先只包 5 个方法），验证依赖方向可行。
- **验收**：功能零变化，`./gradlew test` 全绿。

**P1 拆网络层（1–2 周，风险中）**
- 把 `BiliApi` 按"HTTP / 签名 / 解析 / 业务"四刀切开，`org.json` 只留在 `mapper`；
- 每搬一个方法，补一个 mapper 单测（拿真实 JSON 当固定样本）；
- UI 暂不改调用点 → 靠一层薄 Facade 过渡。
- **验收**：`BiliApi` 缩减到只做转发；mapper 测试 ≥ 30 个。

**P2 抽状态（2 周，风险中高）**
- 顺序：`Home` → `Dynamic` → `Message` → `Library` → `Video` → `Space`（由易到难）；
- 把 `remember` 状态搬进 StateHolder/ViewModel，屏幕只读 `UiState`；
- 顺手把 `HomeFeedMemory` 这类内存补丁换成真正的缓存策略。
- **验收**：每个屏幕的状态都能在"进程被杀后重建"时正确恢复。

**P3 拆大文件（2 周，风险低但工作量碎）**
- `VideoScreen` 1701 行 → 1 个 Screen + 6~8 个 section 文件；
- `VideoPlayer` 同理，手势/控制器/面板分离；
- **验收**：单文件 ≤ 400 行，单函数 ≤ 80 行，无超长参数列表（>12 个参数视为信号）。

**P4 去全局化（1 周）**
- 23 个 `object` 收敛为：注入的控制器 + `StateFlow` + 显式路由参数；
- `AppGraph.pendingXxx` 改为 Route 参数。
- **验收**：全项目 `object` 数量 < 8，且都是无状态的工具/常量。

**P5 Gradle 模块化（1–2 周，可选）**
- 按 feature 拆模块，加 CI 规则（`feature` 之间禁止互相依赖）；
- **建议先做 `:feature:video`、`:feature:home` 两个验证收益**，再决定是否铺开。
- **注意**：模块化会让编译变慢、改代码要跨文件，只有在边界稳定后才划算。

---

## 8. 怎么判断分层成功（可测量的验收标准）

1. 领域层能跑 `./gradlew :core:domain:test`，且**不启动模拟器、不联网**；
2. 全项目搜索 `import org.json` 只出现在 `:infra:bili`；
3. 全项目搜索 `api.` 在 `presentation` 层为 **0 处**（现在是 115 处）；
4. 全项目跨屏 `object ...Bus` 为 **0 个**（现在是 5 个）；
5. 最长文件 < 500 行，最长函数 < 100 行（现在是 1,701 行）；
6. 换一个"假数据源"能把首页跑起来（证明依赖倒置生效）；
7. 新增一个功能（例如"稍后再看排序"）只需改 1 个 feature + 1 个 usecase，不碰其它模块。

---

## 9. 需要先拍板的三件事

| 选项 | 含义 | 适合什么情况 | 代价 |
|---|---|---|---|
| **A. 只做 P0–P2（保守）** | 立骨架、拆网络、抽状态，不拆 Gradle 模块 | 你还要频繁加功能、改 UI | 收益 70%，改动可控 |
| **B. 做到 P4（推荐）** | A + 拆大文件 + 去全局单例 | 想长期维护、想加测试 | 约 6–8 周，中途功能开发会变慢 |
| **C. 做到 P5（彻底）** | B + 完整模块化 | 多人协作、打算做多端 | 编译变慢，样板变多，收益递减 |

**我的建议是 B**：先拿到 90% 的收益，把"要不要拆 Gradle 模块"留到边界稳定后再决定。

---

## 附：本方案刻意保留的东西

- `:danmaku-engine` 的现有分层（parser / engine / render / control），它已经是本项目最规范的部分，不要动；
- `:TeakUI` 的设计系统定位（玻璃材质、动效、页面骨架），它不承载业务，天然符合"核心不依赖外部"；
- 现有测试（尤其是 danmaku 的 19 个测试）作为回归基线；
- 主题/外观相关的 5 个 miuix 背景特效文件，属于纯 UI，跟业务分层无关。
