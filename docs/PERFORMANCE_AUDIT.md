# bdFun 性能与低效代码审查（2026-11-23）

范围：`:app` / `feature:*` / `core:*` / `infra:*` / `:TeakUI` 的生产代码。按「收益 × 改动风险」排序。
每条给出文件:行号、现象与代价、建议改法。行号基于当前工作区（含未提交改动），改前请复核。

## 2026-11-23 本轮已处理

- 播放器：常显进度条改为「每秒采样 + 绘制阶段读取」，只重画那一条线；播放器表面不再读弹幕时钟。
- 弹幕时钟：改用 `PlaybackController.currentPositionMs()`，不再每帧 `refresh()`；`refresh()` 值没变不重复发布。
- 网络抖动：第一次重试改为同一媒体源内 `prepare()`，不重建解码器；第二次仍按原逻辑换地址重搭。
- 空间页滚动：组合期读 + `SideEffect` 回写的自激回路改为 `snapshotFlow`；协程不再每帧重启。
- 动态列表：卡片回调记忆化，直播 UP 预计算成 `Set` 查表。
- 关于页背景：动画时间只在 Canvas 绘制阶段读，不再每帧重组整页。
- `ContinuousCornerShape` 补 `equals/hashCode`，裁剪缓存可复用。
- 骨架屏：`TeakUI/.../skeleton/TeakSkeleton.kt` 与各处骨架卡片全部删除，加载统一用 Miuix 动画。
- 磁盘/主线程：删除缓存改为后台递归删除后再更新列表；缓存页封面/可播地址记忆化（不再每次组合 stat 磁盘）；
  评论图片只读图片头拿宽高（不再整图解码）；二维码生成与保存移后台；Cookie 无变化不写盘；
  播放进度清理从构造期挪到首次写入；日志启动只读文件尾部 128KB。
- 启动健壮性：设置迁移/设置清单解析失败降级为默认值并记日志，不再每次冷启动崩溃。
- 内存：缩略图缓存 64MB→16MB 且支持低内存收缩；退后台不再清空 Coil 缓存（仅低内存/回收时）；
  三连整屏截图随页面销毁回收；空间头图按屏幕宽度解码。
- 后台耗电：前台通知只在标题/播放态/封面等变化时重建（进度由系统推算）；后台轮询 500ms→1s(播放)/5s(暂停)；
  `adopt()` 早退分支不再泄漏传入的播放器。
- 删掉 `AppGraph.init()` 死接口。

本轮刻意未改（原因见下）：退出视频页的播放器释放（用户要求不动）、playurl 渐进地址预取（去掉会削弱解码兜底）、
主壳/顶栏/搜索转场的 graphicsLayer 化（要逐帧重建模糊半径，无设备回归风险高）、长图按屏幕解码（会影响放大清晰度）、
直播表情弹幕位图回收（所有权在弹幕引擎，需要引擎侧接口）。

## P0 · 直接影响流畅度 / 稳定性

1. **播放器表面每帧重组** — `feature/video/.../player/PlayerSurface.kt:41,46,75,79`
   `frameMs`（弹幕时钟，60Hz）与 300ms 轮询的 `positionMs` 都在组合期读取，导致 `AndroidView` 的 update、
   `PlayerAlwaysProgress` 的 `fillMaxWidth(fraction)` 重排、弹幕槽 lambda 每帧重建。
   改法：这些值改成 `() -> Long` / `State` 传下去，只在 Canvas/graphicsLayer 绘制阶段读；进度条只让最小叶子失效。

2. **播放器覆盖层随进度整树重组** — `feature/video/.../player/VideoPlayerLayout.kt:29-33,59-79`
   根布局读 `positionMs/bufferedMs`，`fullscreenPlaybackProjection` 每次新建 `FullscreenPlaybackState`，
   全屏覆盖层每帧重建数据类。改法：根只读低频状态，进度下沉到真正逐帧的叶子。

3. **弹幕时钟每帧 `playback.refresh()`** — `feature/video/.../player/PlayerDanmakuEffects.kt:48-75` +
   `infra/playback/.../Media3PlaybackController.kt:70-89`
   每秒 60 次拿锁、读设置、生成并发布 `PlaybackSnapshot`，所有观察者被持续置脏。
   改法：加不发布状态的 `currentPositionMs()`；`refresh()` 限频 ≥250ms；`bufferOptions()` 只在换源/换账号时调用。

4. **播放器释放阻塞主线程** — `feature/video/.../player/PlayerObserverEffects.kt:68-88`
   `onDispose` 里同步 `player.release()`（部分机型秒级阻塞，项目自己在 `AndroidBackgroundPlayback.kt:66-69` 注明过）。
   改法：统一 post 到已有的 teardown 执行器，主线程只做状态失效。

5. **相关推荐行每帧离屏录制** — `app/.../ui/video/VideoRelatedRow.kt`（本次已改为复用搜索结果行，问题随之消失；
   `SearchResultRow` 本身没有离屏录制，只有 `onGloballyPositioned` 与指针位置数组，开销可接受）。

6. **收藏/缓存页在组合期做磁盘 IO 与整页高频重组** — `app/.../presentation/cache/CacheScreen.kt:31,61-65`
   + `infra/storage/.../AndroidVideoCacheStorage.kt:23`
   220ms 节流的下载队列让整页每次 tick 重组；行内 `storage.exists(coverPath)` 每次组合都 stat 磁盘。
   改法：`collectAsStateWithLifecycle` + 行内订阅自己的进度；封面 URI 在下载完成时算好并落进模型。

7. **删除/入队缓存同步删文件、同步写全量 JSON** — `core/application/.../DownloadQueueStateHolder.kt:57-82,103`
   `File.deleteRecursively()` 与 `persistNow()`（整列表 encode 写 prefs）都在点击线程。
   改法：状态先变，文件删除与持久化进 IO 作用域。

8. ~~骨架屏每个灰块一条 16ms 循环~~ — **已处理（2026-11-23）**：`TeakUI/.../skeleton/TeakSkeleton.kt` 与各处骨架卡片代码
   （动态卡片、消息会话/消息卡片、评论行、搜索结果行、详情元信息、详情尾部）全部删除，加载态统一用 Miuix 加载动画。
   这同时消掉了「一屏几十条 16ms 协程 + 每帧新建 Brush」的开销。

9. **关于页背景每帧重组 + 每帧新建 Brush** — `app/.../presentation/settings/page/SettingsAboutBackground.kt:36-63,100-135`
   改法：时间只在 Canvas draw 里读；`alpha<=0` 时停掉循环。

## P1 · 明显浪费，改动可控

10. **主壳在组合期读每帧动画量** — `app/.../presentation/shell/MainScaffold.kt:36-46`
    `pageFloat`、`bottomSearchProgress/underlayAlpha/blurProgress` 作为 Float 传入 → 切页/搜索转场整棵主壳每帧重组，
    且每帧重建 `blur(RenderEffect)` 的 Modifier 链。改法：改 `State`/lambda + graphicsLayer 内读。

11. **空间页组合期回写 chrome** — `app/.../presentation/space/SpaceChromeBinding.kt:18-41`、
    `SpaceScrollMaterialEffects.kt:20-34`、`SpaceCompactHeader.kt:22-31`
    滚动每帧读 derivedState 并在 `SideEffect` 里写另一组 state；`LaunchedEffect` 的 key 是每帧变化的值。
    改法：`snapshotFlow { ... }.distinctUntilChanged().collect { }`，滚动值在 measure/draw 阶段读。

12. **顶栏滚动进度下发给整棵子树** — `app/.../presentation/shell/MainTopBarChrome.kt:43-63`
    改法：CompositionLocal 改为 `() -> Float`，或按 1/8 步进量化。

13. **动态卡片参数每次新建** — `app/.../presentation/feed/FeedItems.kt:9-32`
    每项新建 7 个 lambda 的 `FeedCardActions`（按身份比较，跳过重组失效）+ `liveUsers.any{}`。
    改法：`remember(item.id)` 提升，live 用父级预计算 `Set<Long>`。

14. **进度轮询不随生命周期停、每 tick 新建协程** — `feature/video/.../player/PlayerProgressEffects.kt:35-63`
    改法：`repeatOnLifecycle(RESUMED)` 拆界面/上报两段；上报用事件 + 低速定时器；协程 await 复用。

15. **后台两条 1Hz 通知/轮询** — `infra/playback/.../PlaybackForegroundService.kt:95-106`（1Hz 重建通知）、
    `AndroidBackgroundPlayback.kt:124-145`（`while(true){ refresh(); delay(500) }` 仅用于 debug 日志，且早退分支不 release）。
    改法：事件驱动；早退分支补 `release()`。

16. **网络抖动时重建整条媒体源** — `infra/playback/.../Media3PlaybackController.kt:120-136`
    改法：`LoadErrorHandlingPolicy` 退避重试，仅换源才 `setMediaSource`。

17. **评论图片上传整图解码** — `infra/bili/.../CommentApi.kt:164-171`
    只为取宽高 decode 全尺寸位图 → 大图 OOM。改法：`inJustDecodeBounds`。

18. **起播串行补发 1~3 次 playurl** — `infra/bili/.../VideoApi.kt:142-165`
    改法：按需（解码失败回调）或并行，替代串行 80/64/32。

19. **启动路径无兜底** — `app/.../feature/settings/application/SettingsStateHolder.kt:21-35`
    迁移/索引解析在主线程且失败即 `error()`；`AppGraph.ensure` 在 `onCreate` 同步建容器。
    改法：整体 `runCatching` + 默认值；容器改 IO 异步 + 首屏最小快照。

20. **冷启动/退后台的图片缓存策略** — `app/.../MainActivity.kt:22-28`
    `TRIM_MEMORY_UI_HIDDEN` 就 `memoryCache.clear()` → 回前台全量重解码。改法：只在 CRITICAL/COMPLETE trim，或 trimToSize。

21. **查看器/缩略图缓存不设上限** — `app/.../presentation/common/photo/PhotoViewerPage.kt:84-99,204-257`（长图按原尺寸解码）、
    `PhotoThumbCache.kt:19-28`（64MB 强引用，从不清）。
    改法：显式 `size()` + `beyondViewportPageCount=0`；缩略图缓存降到 8~12MB 并在 trim 时清空。

22. **直播表情弹幕位图不回收** — `app/.../presentation/live/LiveDanmakuBitmap.kt:170-203`（每条约 2MB，从不 recycle）

23. **三连水波纹整屏位图常驻** — `feature/video/.../metadata/LikeRipple.kt:86-102,141-150`（约 10MB，onDetached 不 recycle）

24. **直播弹幕每来一条 animateScrollToItem** — `app/.../presentation/live/LiveRoomChat.kt:54-59`
    改法：只在已贴底时 `scrollToItem`，并节流。

25. **Compose 生命周期** — 全仓 81 处 `collectAsState`、0 处 `collectAsStateWithLifecycle`。
    改法：页面级统一替换，后台不再收集。

## P2 · 小项 / 顺手修

26. `ContinuousCornerShape` 无 equals/hashCode 且调用点每次 new（`app/.../ui/ContinuousCornerShape.kt`）。
27. `RelatedRow`/`SearchResultRow` 的 `onGloballyPositioned` 里每次分配 2 个对象（`IntArray`、`Rect`）→ 可复用持有器。
28. `TeakSkeleton`/`SpaceBanner` 等：`allowHardware(false)` 与固定 1440 尺寸可按屏宽收缩。
29. `AppGraph.init()` 是死 API 且会覆盖容器不 close（`app/.../AppGraph.kt:93-105`）。
30. debug 诊断常驻：`FreezeWatchdog` 20Hz tick + 帧回调（`app/.../data/FreezeWatchdog.kt:45-61`）、
    `LogCollector` 启动同步读 2MB（`data/LogCollector.kt:42-67`）、`flushSync` 在 UI 日志里同步写盘。
31. `PhotoViewerPage` 的 `HorizontalPager` 未限制相邻页组合。
32. 搜索/热门结果行复用同一份 `SearchResultRow`（本次已做），后续把 `videoResultRows` 作为唯一入口，避免再出现第二份行 UI。

## 备注

- 第 1、2、3、8、9、10、11、12 条都属于同一类问题：**每帧变化的值在组合期被读成参数**。
  统一改成「传 `State`/lambda + 在绘制/测量阶段读」即可，收益集中且互不冲突，建议一次批量做。
- 第 6、7、17、18 条是主线程 IO / 无谓网络，属于「用户能感知的卡顿与耗电」。
- 第 21、22、23 条是内存峰值，长图/直播/三连场景最容易 OOM。
