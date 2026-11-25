# 全仓清理与性能修复报告（2026-11-25）

> 分支：`perf/full-cleanup-20261125`，基线 `main`（`cb2f0e2`）。
> 独立 worktree：`D:\work\bdFun-perf`，原工作区未改动。
> 验证：`:app:assembleDebug`、`:app:assembleRelease`（R8）、`:app:testDebugUnitTest`、
> `:core:common:test` 均通过。

## 提交

| commit | 内容 |
|---|---|
| `a08b533` | 删除无调用死代码（29 个文件 / 1803 行） |
| `f1c73ff` | 减少重复编译、主线程 IO 与多余请求 |
| `2b14c3c` | 降低滚动/播放期间的重复计算与分配 |
| 本文档 | 清理记录与未改动项说明 |

## 一、删除的死代码（约 1100 行源码 + 2 个测试）

- 整包不可达：`presentation/common/splash`（6 文件）、`presentation/common/swipe`（6 文件）。
- 旧组件：`ui/TeakActionSheet.kt`、`ui/TabTopBar.kt`、`ui/video/VideoRelatedContent.kt`、
  `presentation/library/LibrarySearchHistory.kt`、`presentation/space/SpaceBangumiCard.kt`、
  `presentation/common/glyph/MenuGlyph.kt`、`presentation/feed/FeedViewerTopBar.kt`、
  `FeedCommentGlyph.kt`、`FeedListObservation.kt`。
- TeakUI 仓内 0 引用：`TeakNavigationRail`、`TeakGlassFloatingActionButton`、`TeakGlassPressFeedback`。
- 仅测试引用：`FeedImageTransform`、`FavoriteFolderOperations`（连同测试删除）。
- 零散无调用函数/常量约 250 行：`TermsScreen`、`rememberJellyProgress`、`TeakChevronUpDown`、
  `CapsuleOption`、`SubmissionChip`、`TeakGlassChip`、`MessageTab`、`saveLoginCookies` 等。
- 保留：`TERMS_SECTIONS`（引导页仍在使用）、`data/` 包类型别名（仍有调用者）。

## 二、已确认并修复的低效路径

1. **播放起播少发请求**：DASH 成功后的 progressive 兜底从最多 3 次串行减为 1 次 1080P。
2. **release 不再为日志白算**：playurl 的两次正则 + 整段替换只在 debug 执行。
3. **正则/摘要不再逐次编译**：WBI 非法字符、动态 PGC id、评论提及名单均改为常量或按快照缓存。
4. **MD5/数字格式化**：`%02x`/`%.1f` 改为查表与整数运算。
5. **直播表情弹幕缓存真正生效**：不再为每条消息新建合并 Map 冲掉缓存。
6. **弹幕合并/过滤**：去掉字符串去重键与中间 List，改为 id 集合 + 一次性分配。
7. **弹幕 UP 徽章**：FontMetrics/文字宽度在 measure 阶段算好，绘制阶段不再每帧重算。
8. **主线程 IO**：发送记录解析加缓存；播放进度写入与清理移到单线程后台；下载队列落盘移到 IO。
9. **消息会话首屏**：昵称补全并发 4→8，减少约一半往返。
10. **界面**：首页曝光 100ms→200ms 且不重复曝光；直播聊天不再逐条重启动画；
    评论/搜索布局回调复用坐标数组；动态自动播放滚动中不算最近项；评论分页不再每页重启；
    消息过滤/搜索元信息按输入缓存。

## 三、审查后主动不改的项（附原因）

- **全局 HTTP 缓存**：B 站 JSON 多数没有缓存头，直接挂 `Cache` 基本不生效；按接口手写
  `Cache-Control` 有拿到旧收藏夹/排行榜数据的风险，需要逐接口验证，放到下一版。
- **推荐学习记录写盘**：写盘已在 IO 线程，剩余成本是主线程上的归档重建；改动涉及
  状态一致性，收益需实测，暂不动。
- **历史页 N+1（每页最多 30 次 view 请求）**：批量 `archive/stat` 接口字段未经真实样本确认，
  按 AGENTS「接口字段不许猜」保留现状。
- **8ms 定时动画改帧同步**：`ManualAnimation.kt` 明确记录「本项目 frame clock 不推进，已验证」，
  改 `withFrameNanos` 有动画冻结风险，放弃。
- **`collectAsState` → `collectAsStateWithLifecycle`**：需新增依赖，且主 tab 只组合当前页，
  收益有限，不为此扩依赖。
- **未使用的 Gradle 依赖**：删除需要 release/R8 逐项验证，本轮已保留。

## 四、验证记录

```bash
gradlew.bat :app:compileDebugKotlin        # 通过
gradlew.bat :app:testDebugUnitTest         # 通过
gradlew.bat :core:common:test              # 通过（Format 用例覆盖改写的格式化）
gradlew.bat :app:assembleDebug             # 通过
gradlew.bat :app:assembleRelease           # 通过（R8 fullMode）
```

已知环境问题：`danmaku-engine` 的 Robolectric 图形用例在本机报 `UnsatisfiedLinkError`，
在 `main` 基线上同样失败，与本次改动无关。

## 五、产物

- debug：`app/build/outputs/apk/debug/app-debug.apk`
- release：`app/build/outputs/apk/release/app-release.apk`
