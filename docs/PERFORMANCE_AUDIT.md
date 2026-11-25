# bdFun 性能审查（2026-11-25）

> 目标：在不改变行为的前提下，找出并消掉真正的低效路径。按「已修 / 待办」分组，
> 每条都写清位置、原因、验证方式。没有实测依据的“优化”不要合入。

## 已修

### 1. `TeakSlidingChipRow` 每帧重建胶囊路径

- 位置：`TeakUI/.../navigation/TeakSlidingChipRow.kt`
- 原因：滑动指示器在 `drawBehind` 里每帧为每枚标签 `Path()` + `Capsule.createOutline`，
  一屏 6～10 枚、动画 20～40 帧，就是每帧几百次路径分配与曲线构建。
- 处理：灰底胶囊路径按 `(宽,高,layoutDirection)` `remember` 一次；粉色块复用同一个
  `Path`，宽度没变就不重建；平移改在绘制阶段 `translate`，不再改路径本身。
- 顺带修复：胶囊恢复成 Kyant `Capsule(Continuous)` 的连续圆角（之前 `drawRoundRect`
  是普通圆角，端点和项目其它胶囊不一致）。

### 2. 图片查看器放大后吞掉点击事件

- 位置：`presentation/common/photo/PhotoViewerPage.kt`
- 原因：`photoZoomGestures` 在 `zoom > 1.01` 时对**所有**指针事件无条件 `consume()`，
  同一条链上的 `detectTapGestures` 再也收不到 down，于是放大后双击无法缩回、单击也无法关闭。
- 处理：只在 `calculatePan()` 真的非零（确实在拖动）时消费。双击缩回、单击关闭恢复。

## 待办（按收益/风险排序）

1. **`collectAsState()` 全量排查**（收益大、面广）：`:app` 里大量页面直接 `collectAsState()`，
   后台/不可见时仍会重组。应逐步换成 `collectAsStateWithLifecycle`，或在页面可见性上收口。
   迁移期间保持每个页面的收集时机不变，按页面分批做。
2. **组合期 `.map {}` / `toList()`**：搜索结果、消息列表、评论列表里仍有在 item 的
   lambda 内构造列表/映射的写法。逐个确认后挪到 `remember(输入)` 外面或预计算。
3. **`Regex(...)` 构造位置**：`DlnaCaster`、`ChatMessageCard`、`SpaceWidgets`、
   `BasScenePainter`、`SpecialVideoDanmakuOverlay` 等文件里有 `Regex(...)`；
   需要逐个确认是否在每次调用/每帧执行，是的话提升为文件级常量。
4. **图片查看器 `PhotoViewerPage` 的 `remember` 键**：`url` 变化时重建整页状态是预期行为，
   但 `PhotoMorphImage` 的两个 `AsyncImage` 在放大手势期间不应重组；目前缩放走
   `graphicsLayer`，已符合，继续守住。
5. **首页/直播分页**：滚动触发用 `snapshotFlow` + `drop(1)`，已经统一；不要改回
   在 item 组合时直接请求。
6. **`LogCollector`**：debug 才启用，release 不初始化；不要在热路径新增日志。

## 验证门槛

- 每批改完必须过 `:app:assembleDebug` + `:app:testDebugUnitTest`，涉及纯逻辑的补 JVM 测试。
- UI 行为改动在模拟器（1220×2656 / 420dpi）上截图或逐帧对照，尤其：
  图片查看器双击放大/缩回/单击关闭、标签滑动、收藏/历史切换动画。
