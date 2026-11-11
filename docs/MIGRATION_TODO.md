# 迁移任务清单（MIGRATION_TODO）

> 配套文档：`AGENTS.md`（规则与约定）、`docs/ARCHITECTURE_REVIEW.md`（为什么这么改）
> 本文件是**可领取的施工任务**。每个任务都能独立完成、独立验收、独立回滚。

---

## 一、怎么用这份清单（给人看的部分）

### 1. 一个任务 = 一次 PR = 一个分支

```
分支名：arch/P1-05-video-api
PR 标题：[P1-05] 视频域接口分片
```

一个 PR 里**只做任务卡上写的事**。发现别的问题，记到下面「附录 A：顺手发现」里，另开任务，不要夹带。

### 2. 领取规则

1. 看任务的「前置」是否都已完成（`[x]`）。
2. 把任务卡里的 `负责人` 改成自己，把 `状态` 改成 `进行中`。
3. 完成后：勾上验收项、填日期、`状态` 改成 `已完成`，再提 PR。
4. **同一时间一个人只领一个任务**，避免半成品堆积。

### 3. 什么叫"完成"

必须**同时**满足：

- [ ] `gradlew.bat :app:assembleDebug` 通过
- [ ] `gradlew.bat :app:testDebugUnitTest` 通过（如果有相关测试）
- [ ] 任务卡上的「验收」逐条满足
- [ ] 没有新增 `AGENTS.md §3` 里禁止的依赖方向
- [ ] PR 描述里贴了验证方式（命令输出 / 手动操作步骤）

### 4. 估算说明

工作量是**单人、熟悉代码后的粗估**，单位「人天」。第一次做同类任务通常要乘 1.5–2 倍。别拿去当排期承诺。

---

## 二、总进度看板

| 阶段 | 内容 | 任务数 | 完成 | 预计 |
|---|---|---|---|---|
| P0 | 立骨架、装依赖注入、上规矩 | 6 | 0 | 3.5 人天 |
| P1 | 拆网络层（最大的一块） | 11 | 0 | 9–11 人天 |
| P2 | 抽状态（风险最高） | 8 | 0 | 10–12 人天 |
| P3 | 拆大文件 | 8 | 0 | 8–10 人天 |
| P4 | 去全局单例 | 6 | 0 | 5–6 人天 |
| P5 | Gradle 模块化（可选） | 5 | 0 | 6–10 人天 |

**依赖关系**（`→` 表示"必须先做"）：

```
P0-01 → P0-02 → P1-01..P1-03 → P1-04 → P1-05..P1-09 → P1-10
P0-03（独立，随时可做）
P0-04（只依赖 P0-01）
P0-05（只依赖 P0-01/02/03）
P0-06（只依赖 P0-01/02，为 P2/P4 预留）
P0-06 → P2-01 / P4-03
P1-11（设置项拆分）依赖 P0-03，可与 P1-04 并行

P2-01 → P2-02..P2-08
P1-10 → P2-08（清晰度规则要先抽出来）
P2-07 → P3-01 → P3-02 → P3-03
P2-06 → P3-04
P2-02..P2-05 → P3-05..P3-07

P2-07 + P2-08 → P4-01 → P4-02 → P4-04
P4-03 独立（只依赖 P0-04）
P4-05 依赖全部 P4
P5-* 依赖 P4-05，且需要团队同意
```

---

## 三、任务卡

> 编号规则：`P<阶段>-<两位序号>`。新增任务接在对应阶段末尾，不要插入中间打乱编号。

---

### P0 立骨架

#### P0-01 · 新建 `:core:model` 模块
- **人话目标**：给"视频、评论、用户"这类业务名词找一个不依赖 Android 的家。
- **动作**：
  1. 新建 JVM 模块 `:core:model`（`kotlin("jvm")`，不是 Android 模块）。
  2. 把 `data/Models.kt` 里**纯数据**的类（`VideoItem`、`VideoDetail`、`CommentItem`、`UserProfile` 等）**复制**进去，按上下文分包：`video/`、`comment/`、`user/`、`feed/`、`message/`、`library/`。
  3. `:app` 添加 `implementation(project(":core:model"))`，暂时两边并存。
- **涉及**：`settings.gradle.kts`、`app/build.gradle.kts`、新建目录
- **前置**：无
- **验收**：`:app` 能编译；`Models.kt` 未被删除；`:core:model` 里没有任何 `import android.*` / `okhttp3` / `org.json`
- **预估**：0.5 人天 · **风险**：低 · **可并行**：与 P0-03 并行

#### P0-02 · 新建 `:core:domain` 并定义 3 个仓储接口
- **人话目标**：先把"要什么数据"写成菜单，不关心"从哪弄"。
- **动作**：
  1. 新建 JVM 模块 `:core:domain`，依赖 `:core:model`。
  2. 定义 `VideoRepository`、`CommentRepository`、`FeedRepository` 三个接口，每个先只放 2–3 个只读方法（如 `videoDetail(bvid)`、`comments(oid, page)`、`recommend(page)`）。
  3. 返回类型统一 `suspend fun ... : Result<T>`（或统一的 `Outcome<T>`，与 P0-03 对齐）。
- **前置**：P0-01
- **验收**：接口能编译；`core:domain` 零第三方依赖；有至少 1 个接口的 KDoc 说明失败语义
- **预估**：0.5 人天 · **风险**：低

#### P0-03 · 新建 `:core:common` 并迁入纯函数
- **人话目标**：把"数字转 1.2万""时间转 昨天"这类纯计算集中到一个地方。
- **动作**：
  1. 新建 JVM 模块 `:core:common`。
  2. 迁入 `ui/Format.kt` 的 4 个函数（`formatCount` / `formatDuration` / `formatDate` / `formatDateTime`），原文件改为一层转发（避免大改调用点）。
  3. 定义 `AppError`（网络/登录失效/风控/解析/未知）与 `Outcome<T>`。
  4. 补每个格式化函数的边界单测（0、负数、超大数、跨年）。
- **前置**：无
- **验收**：单测全绿；`:app` 编译通过；`ui/Format.kt` 只剩转发（≤10 行）
- **预估**：0.5 人天 · **风险**：低 · **可并行**：与 P0-01 并行

#### P0-04 · 建 `AppContainer`，收拢对象的创建
- **人话目标**：现在对象到处 new，改成"进门时统一发工牌"。
- **动作**：
  1. 在 `:app` 建 `di/AppContainer.kt`，集中创建 `BiliApi`、`SettingsStore`、`SentHistoryStore`、`VideoCacheManager`、`CdnAutoSelector`、`PlaybackProgressStore`。
  2. `AppGraph` 改为**委托** `AppContainer`（公开字段与签名不变），保证所有现有调用点零改动。
  3. `MainActivity`、`AppGraph.ensure()` 两条初始化路径合并到一处。
- **前置**：P0-01（可先做，不阻塞）
- **验收**：`AppGraph.api` / `AppGraph.settings` 的调用点无需修改；冷启动能进首页；`AppGraph.ensure()` 不再重复初始化
- **预估**：1 人天 · **风险**：中（涉及启动顺序）· **注意**：`ThemePalette.style` 的赋值时机不能变

#### P0-05 · 加"架构守卫"检查
- **人话目标**：装一个门禁，谁把网络代码写进业务层就报警。
- **动作**：
  1. 写一个 Gradle task 或 PowerShell/Kotlin 脚本 `tools/check-layering.kt`：
     - `core:model` / `core:domain` 出现 `import android.`、`okhttp3`、`org.json` → 失败
     - `feature:*` 之间互相 `import` → 失败（P5 之后才有 feature 模块，先留空跑通）
  2. 接到 `check` 任务之前，或写进 PR 检查清单。
- **前置**：P0-01 / P0-02 / P0-03
- **验收**：故意在 `core:domain` 加一行 `import android.content.Context`，脚本必须报错并退出非 0
- **预估**：0.5 人天 · **风险**：低

#### P0-06 · 建 `:core:application` 与 `:core:navigation` 骨架（只建空壳）
- **人话目标**：给"跨页面共享的状态"和"页面地址清单"预留两个抽屉，现在不放东西，避免以后拆模块时返工。
- **动作**：
  1. 新建 JVM 模块 `:core:application`（依赖 `:core:model`、`:core:domain`），先只放一个 `SessionStore` 接口。
  2. 新建 JVM 模块 `:core:navigation`，先只放 `Route` 密封类骨架（视频详情、空间、动态详情三条）。
  3. **不接线、不替换现有导航**，只是让后续 P2/P4 有落点。
- **前置**：P0-01 / P0-02
- **验收**：两个模块能编译；`:app` 依赖它们但**行为零变化**；`core:application` 里没有 `@Composable`
- **预估**：0.5 人天 · **风险**：低 · **注意**：这一步只建空壳，任何"顺手接一下"都算超出范围

---

### P1 拆网络层

> 这一段全部围绕 `data/BiliApi.kt`（3,758 行 / 102 个方法）。原则：**先搬不改**，`BiliApi` 保留同名转发方法，保证 UI 零改动。

#### P1-01 · 抽出 `BiliHttpClient`
- **动作**：先新建 `:infra:bili` 模块（Android library），再把 `dataRequest` / `executeOnce` / `rawRequest` / `postRequest` / `awaitResponse` / `retryDelayMs` 搬进去，只负责：拼 URL、带 Cookie、超时、重试、把 HTTP 错误转成 `AppError`。
- **前置**：P0-02 / P0-03
- **验收**：`BiliApi` 里不再出现 `okhttp3.Call`；首页、视频页、消息页三个页面手动点一遍正常
- **预估**：1.5 人天 · **风险**：中高（重试/超时行为容易变）· **注意**：现有重试次数与退避策略要原样保留

#### P1-02 · 抽出 `WbiSigner`
- **动作**：把 `ensureWbiKeys` / `parseWbi` / `signAppParams` 及相关缓存搬进 `infra:bili/WbiSigner`，加一个"密钥过期自动重取"的显式状态。
- **前置**：P1-01
- **验收**：需要签名的接口（`view`/`playInfo`/`related`）全部正常；key 过期时能自动重试一次
- **预估**：1 人天 · **风险**：中（签名错=全站 403）

#### P1-03 · 抽出账号与登录态 `AccountApi`
- **动作**：把 `nav` / `qrGenerate` / `qrPoll` / `saveLoginCookies` / `logout` / `isLoggedIn` / `ensureBuvid` 搬进 `infra:bili/AccountApi` + `AccountRepository` 实现；`AppGraph.session` 暂时保留。
- **前置**：P1-01
- **验收**：扫码登录、退出登录、重启后保持登录三件事正常
- **预估**：1 人天 · **风险**：中

#### P1-04 · 建立 dto + mapper 基础设施
- **动作**：
  1. 建 `infra:bili/dto/` 与 `infra:bili/mapper/`。
  2. 先做**视频域**的完整示范：`VideoDetailDto` + `VideoDetailMapper`，把 `parseVideoDetail` / `parseFeedVideo` 的逻辑翻译过去。
  3. 建测试样本目录 `infra/bili/src/test/resources/json/`，放 3 段真实返回（脱敏），断言映射结果。
- **前置**：P1-01
- **验收**：mapper 单测 ≥ 3 个；`org.json` 不出现在 mapper 之外；字段缺失时不崩溃、给默认值
- **预估**：1.5 人天 · **风险**：中 · **注意**：这是后续所有分片的模板，值得慢一点

#### P1-05 · 视频域接口分片
- **动作**：`view` / `viewByAid` / `playInfo` / `related` / `danmaku` / `sponsorSegments` / `subtitles` / `subtitleCues` / `onlineTotal` → `VideoApi`；`BiliApi` 保留转发。
- **前置**：P1-04
- **验收**：视频页完整走一遍（进详情、切清晰度、切分P、看相关推荐、开弹幕、开字幕）；`BiliApi` 对应方法只剩一行转发
- **预估**：1.5 人天 · **风险**：高（播放链路最容易出事）

#### P1-06 · 评论域接口分片
- **动作**：`comments` / `commentsByOid` / `replies` / `repliesByOid` / `addComment` / `likeComment` / `reportComment` / `deleteComment` / `replyEmotes` / `uploadCommentImage` → `CommentApi`。
- **前置**：P1-04
- **验收**：评论列表、楼中楼、发评论、点赞、举报、删评论全部正常
- **预估**：1 人天 · **风险**：中

#### P1-07 · 信息流域接口分片
- **动作**：`recommend` / `webRecommend` / `appRecommend` / `popular` / `ranking` / `dislikeRecommendation` / `dynamics` / `userDynamics` / `dynamicDetail` / `likeDynamic` / `dynamicComments` → `FeedApi`（内部可再分 `recommend`/`dynamic`）。
- **前置**：P1-04
- **验收**：首页下拉刷新、翻页、不感兴趣、动态页全部正常
- **预估**：1 人天 · **风险**：中

#### P1-08 · 消息域接口分片
- **动作**：`messages*` / `messageSessions` / `privateMessages` / `sendPrivateMessage` / `newFollowers` / `messageUnreadCount` / `clearAllUnread` / `markSessionsRead` / 会话的 pin/mute/remove → `MessageApi`。
- **前置**：P1-04
- **验收**：四个消息 tab、私信收发、未读数清零、会话置顶/免打扰/删除正常
- **预估**：1 人天 · **风险**：中

#### P1-09 · 空间 / 搜索 / 媒体库接口分片
- **动作**：`space*` → `SpaceApi`；`search*` / `hotSearch` / `hotKeywords` / `searchUsers` → `SearchApi`；`history*` / `watchLater*` / `favorite*` / `followUser` / `relation*` → `LibraryApi`。
- **前置**：P1-04
- **验收**：空间页六个 tab、搜索联想+结果+筛选、历史/收藏/稍后再看三个页面正常
- **预估**：2 人天 · **风险**：中（方法最多，可再拆成 3 个子任务）

#### P1-10 · 把"业务规则"从网络层拔出来
- **动作**：
  1. 清晰度选择：`parsePlayInfo` 里的 `chooseAudio` / `codecPriority` / `codecMatches` 抽成 `core:domain` 的 `PlaybackPolicy`（纯函数）。
  2. CDN 选路：`CdnService` / `isMcdnResource` / `isUpgcxcode` / `bestPortableUrl` → `core:domain` 的 `CdnPolicy`（保留 `CdnAutoSelector` 的测速逻辑在 infra）。
  3. 补单测：给定一组清晰度/编码/CDN 候选，断言选哪个。
- **前置**：P1-05
- **验收**：`PlaybackPolicy` 有 ≥ 6 个单测；改规则只改这一个文件；视频仍能正常起播
- **预估**：1.5 人天 · **风险**：高（选错=黑屏/无声）· **注意**：先把现有行为写成测试，再抽；不要"顺手改进"

#### P1-11 · 拆分 `SettingsStore`（888 行 / 81 个字段）
- **人话目标**：把"一个装满 81 个开关的抽屉"分成五个抽屉：外观、播放、弹幕、消息、通用。
- **动作**：
  1. 新建 `:infra:storage` 模块；定义 `SettingsRepository` 总接口 + 5 个分组接口（`AppearanceSettings` / `PlaybackSettings` / `DanmakuSettings` / `MessageSettings` / `GeneralSettings`）。
  2. 用 `SharedPreferences` 或 DataStore 实现（**沿用现有存储格式，先做兼容读取**，不要一次性迁移数据导致用户设置丢失）。
  3. `SettingsStore` 暂时保留为门面，内部转发到分组接口，保证 63 处调用点零改动。
  4. 补"读写往返"单测：每个字段写进去能读出来、默认值正确。
- **前置**：P0-03
- **验收**：升级安装后老用户的设置不丢；80 个字段全部有往返测试；`SettingsStore.kt` 行数降到 ≤ 200
- **预估**：1.5 人天 · **风险**：中高（丢设置是用户最不能忍的）· **可并行**：与 P1-04 起的分片并行

---

### P2 抽状态

> 每个任务的结构都一样：把 Composable 里的 `remember { mutableStateOf }` 搬进 StateHolder，Composable 只读 `UiState`。

#### P2-01 · 定义 StateHolder 模板，先改造 `HomeFeedState`
- **动作**：
  1. 在 `:core:application` 定模板（约定 + 一个 `UiState` 基础接口）：`data class XxxUiState`（不可变）+ `class XxxStateHolder` 暴露 `StateFlow<XxxUiState>` + `onEvent(...)`。具体实现放各 feature，不放进 `core`。
  2. 把现有 `HomeFeedState`（已是不错的雏形）改造成模板的标准样例，保留"退出再回来不重新拉取"的现有行为。
- **前置**：P0-04
- **验收**：`HomeFeedState` 不再被 `remember` 创建，而是从容器取；首页行为完全不变；文件内有一句"这是模板，照抄"的注释
- **预估**：1 人天 · **风险**：中

#### P2-02 · 首页状态迁移
- **前置**：P2-01 · **验收**：下拉刷新/翻页/不感兴趣/滚动位置保持/进详情返回不重拉，全部不变
- **预估**：1 人天

#### P2-03 · 动态页状态迁移
- **动作**：`DynamicFeedState` 已经是 StateHolder，主要是把它的创建、生命周期、缓存（`HomeFeedMemory` 同类补丁）理顺。
- **前置**：P2-01 · **验收**：作者侧栏切换、自动播放、分页、返回不丢失
- **预估**：1 人天 · **风险**：低

#### P2-04 · 消息页状态迁移
- **前置**：P2-01 · **验收**：四个 tab 的独立分页游标不互相污染；未读数正确；进程被杀后重建不崩溃
- **预估**：1.5 人天 · **风险**：中（现有 `MessagePageBus` 会干扰）

#### P2-05 · 媒体库状态迁移
- **前置**：P2-01 · **验收**：收藏夹切换、批量选择、历史分页、离线缓存列表状态正常
- **预估**：1.5 人天 · **风险**：中

#### P2-06 · 空间页状态迁移
- **动作**：`SpaceActivity` 的 `SpaceCache` + 1,076 行 `SpaceScreen` 里的状态先集中到 `SpaceViewModel`，界面先不动。
- **前置**：P2-01 · **验收**：六个 tab 切换、下拉刷新、返回重进不白屏
- **预估**：1.5 人天 · **风险**：中

#### P2-07 · 视频详情状态迁移（**全项目最危险，必须拆两步**）
- **动作**：
  - **第一步（只读）**：把 `detail` / `playInfo` / `related` / `danmaku` / `error` / `loading` 六个状态搬进 `VideoDetailViewModel`，交互动作（点赞/投币/收藏）仍留在原处。
  - **第二步（交互）**：再搬 `followed` / `qualityId` / `resumePositionMs` 与各动作。
  - 每步单独一个 PR，单独可发布。
- **前置**：P2-01 / P1-10
- **验收（第一步）**：进视频页、切分P、切清晰度、相关推荐跳转、返回，全部不变
- **验收（第二步）**：点赞/投币/收藏/关注/稍后再看全部正确；断网时的错误提示不变
- **预估**：3 人天（两步合计）· **风险**：**高** · **注意**：动手前先录一段"改造前行为"的视频作为对照

#### P2-08 · 播放器状态迁移
- **动作**：把 `VideoPlayer` / `VideoActivity` / `BackgroundPlayback` 三处的播放状态收敛为一个 `PlaybackController` 接口 + ExoPlayer 实现（放 `infra:playback`），加一个 `FakePlaybackController` 供测试。
- **前置**：P2-07
- **验收**：旋转、画中画、后台播放、通知栏控制、蓝牙耳机按键、进度记忆，七项全过
- **预估**：2 人天 · **风险**：**高**

---

### P3 拆大文件

> 只搬不改行为。每个任务的目标都是"让下次改动能只看 300 行"。

#### P3-01 · 拆 `VideoScreen`（1,701 行）
- **动作**：按区块拆成 `VideoDetailScreen` + `sections/VideoHeader`、`VideoActionBar`、`VideoDescription`、`VideoPageStrip`、`VideoRelatedList`、`VideoCacheEntry`。
- **前置**：P2-07 · **验收**：单文件 ≤ 400 行；播放页视觉与行为零变化
- **预估**：2 人天 · **风险**：中

#### P3-02 · 拆 `VideoPlayer`（1,680 行）
- **动作**：拆成 `player/PlayerSurface`、`player/gestures`、`player/controls`、`player/panels`。
- **前置**：P3-01 · **验收**：手势（亮度/音量/快进/长按倍速）与面板全部正常
- **预估**：2 人天 · **风险**：高

#### P3-03 · 拆 `FullscreenPlayerUi`（928 行）
- **动作**：`Chrome` 保留骨架，`panels/*` 各自成文件（清晰度、倍速、弹幕设置、收藏、更多、字幕）。
- **前置**：P3-02 · **验收**：全屏六个面板逐一点开验证
- **预估**：1.5 人天 · **风险**：中

#### P3-04 · 拆 `SpaceScreen`（1,076 行）
- **前置**：P2-06 · **验收**：空间页六个 tab + 折叠头 + 横幅全部正常
- **预估**：2 人天 · **风险**：中

#### P3-05 · 拆评论相关（653 + 706 行）
- **前置**：P2-07 · **验收**：评论列表、楼中楼、图片查看、回复条、菜单全部正常
- **预估**：1.5 人天 · **风险**：中

#### P3-06 · 拆 `App.kt` 的 `BdFunApp`（523 行）
- **动作**：拆成 `MainScaffold` + `navigation/MainTabBar` + `navigation/ClassicTabBar` + 各 tab 的路由装配。
- **前置**：P2-02 ~ P2-05 · **验收**：底栏切换、返回栈、去重点击回到顶部，全部不变
- **预估**：1 人天 · **风险**：中

#### P3-07 · 清点重复 UI 并下沉到 `:core:ui`
- **动作**：先把重复项列成清单（例如多个页面各写一遍的 `EmptyHint`、日期格式化、头像、加载态、错误态），再逐个下沉。**先出清单再动手**，避免边找边改。
- **前置**：P3-01 ~ P3-06 · **验收**：清单里每一项要么下沉、要么写明"不合并的理由"
- **预估**：1.5 人天 · **风险**：低

#### P3-08 · 删除 `*Memory` 内存补丁
- **动作**：`HomeFeedMemory`、`SpaceCache`、`RelatedMemory`、`MineProfileCache`、`EmoteCache`、`InlineVideoCache` 逐个评估：能用正式缓存替代的删掉，不能的改成有上限的 `LruCache` 并写注释。
- **前置**：P3-01 ~ P3-06 · **验收**：无上限的静态 Map 全部消除
- **预估**：1 人天 · **风险**：中

---

### P4 去全局单例

#### P4-01 · `VideoPageBus` → `StateFlow`
- **动作**：把播放/暂停/评论定位/当前 bvid 改为 `core:application` 的 `PlaybackSession`，用 `StateFlow` 暴露；`VideoPageBus` 保留一个转发壳，等调用点改完再删。
- **前置**：P2-08 · **验收**：视频页与后台播放的联动、消息跳转到指定评论，行为不变
- **预估**：1.5 人天 · **风险**：高

#### P4-02 · 其余三个 Bus 收敛
- **动作**：`LibraryPageBus` / `MessagePageBus` / `ScrollTopBus` → 各自 StateHolder 暴露的 `SharedFlow`（一次性事件）或 `StateFlow`（状态）。
- **前置**：P4-01 · **验收**：跨页面触发的动作（清空选择、回到顶部、消息刷新）仍然生效
- **预估**：1 人天 · **风险**：中

#### P4-03 · `AppGraph.pendingXxx` → 路由参数
- **动作**：`pendingVideo` / `pendingDynamic` / `pendingSpaceMid` 改为 `Route` 的显式字段；`BiliLink` 的深链解析改为返回 `Route` 对象。
- **前置**：P0-04 · **验收**：所有入口（首页卡片、搜索、消息、空间、剪贴板、外部链接）都能正确打开对应页面；**少传参数时应当明确报错而不是白屏**
- **预估**：1 人天 · **风险**：中

#### P4-04 · `BackgroundPlayback` 单例 → `PlaybackController`
- **前置**：P4-01 · **验收**：与 P2-08 的验收清单相同，且服务销毁/重建后状态一致
- **预估**：1 人天 · **风险**：高

#### P4-05 · 全局 `object` 审计
- **动作**：23 个 `object` 逐个判定：改成注入的类 / 保留为无状态工具 / 删除。产出审计表（保留原因必须写清楚）。
- **前置**：P4-01 ~ P4-04 · **验收**：数量 < 8，且剩下的全部无状态或有明确生命周期
- **预估**：0.5 人天 · **风险**：低

#### P4-06 · `AppContainer` 引入作用域
- **动作**：区分 application 级 / Activity 级依赖；把 `Activity` 里 `lateinit var api` 的写法改成统一取容器。
- **前置**：P0-04 / P4-05 · **验收**：20 个 Activity 的依赖获取方式一致；没有 Activity 之间共享实例
- **预估**：1 人天 · **风险**：中

---

### P5 Gradle 模块化（可选，需团队同意后执行）

> **不推荐现在做**。等 P4 完成、边界稳定、编译时间开始变长时再评估。

#### P5-01 · 抽 `:infra:bili`
- **前置**：P1 全部完成 · **验收**：`:app` 仍能编译；`:infra:bili` 不依赖 `:app`
- **预估**：1.5 人天

#### P5-02 · 抽 `:feature:home`
- **前置**：P4-05 · **验收**：首页功能不变；模块间无循环依赖
- **预估**：1.5 人天

#### P5-03 · 抽 `:feature:video`
- **前置**：P5-02（先用小模块验证流程，再动最大的）
- **验收**：视频链路完整；播放器仍在 `:infra:playback`
- **预估**：2 人天 · **风险**：高

#### P5-04 · CI 依赖规则校验
- **动作**：把 `tools/check-layering.kt` 扩展成模块级校验（`feature` 互不依赖、`domain` 无 Android）。
- **前置**：P0-05 / P5-01 · **验收**：故意加一条违规依赖，CI 必须失败
- **预估**：1 人天

#### P5-05 · 模块化收益复盘
- **动作**：记录改造前后的**冷/热编译时间**、APK 体积、CI 时长，判断是否继续铺开剩余 feature。
- **前置**：P5-01 ~ P5-04 · **验收**：产出一页结论（继续 / 停止 / 部分回退）
- **预估**：0.5 人天

---

## 附录 A：顺手发现（不在本次任务范围内）

> 做任务时发现的问题记在这里，别夹带修改。等积累到一定数量再评估是否立项。

| 日期 | 发现 | 发现人 | 建议 |
|---|---|---|---|
| | | | |

---

## 附录 B：新增任务卡模板

```markdown
#### P<x>-<nn> · <一句话标题>
- **人话目标**：<不用术语，说清楚完成后用户/开发者能感受到什么>
- **动作**：
  1. ...
- **涉及**：<文件/模块>
- **前置**：<任务编号，或"无">
- **验收**：<可执行、可观察的判断标准，至少 2 条>
- **预估**：<人天> · **风险**：低/中/高 · **可并行**：<与谁并行>
- **负责人**：待领取 · **状态**：待开始
```

---

## 附录 C：任务状态标记法

| 标记 | 含义 |
|---|---|
| `[ ]` | 未开始 |
| `[~]` | 进行中（同时只能有一个） |
| `[x]` | 已完成（需填完成日期） |
| `[!]` | 阻塞（必须写明阻塞原因和等谁） |
