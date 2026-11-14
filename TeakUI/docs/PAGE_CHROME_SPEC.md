# Teak 页面 Chrome 规范

更新时间：2026-11-02

这套规范定义「设置」「收藏 / 历史 / 稍后再看」等二级页面共用的：

- 顶部渐变模糊；
- 内容里的大标题；
- 顶栏小标题；
- 左上角玻璃返回键；
- 滚动折叠进度；
- 标题位置、字号、模糊半径、动画时长。

所有数值只有一个来源：`TeakChromeDefaults`。页面不要自己写 52dp、30sp、0.65f、12dp 这类魔法数字。

设置页 `TeakSettingsPage` 是当前的标准样板，其他页面按它的位置对齐。

## 固定位置

| 项目 | 值 | 说明 |
| --- | --- | --- |
| 顶栏内容高度 | `64dp` | 不含状态栏 |
| 顶部 overlay 高度 | `56dp` | 含返回键/操作按钮这一行 |
| 顶部 overlay 左右/上下内边距 | `12dp` / `8dp` | 返回键与操作按钮共用 |
| 顶部按钮间距 | `8dp` | 返回键与右侧操作之间 |
| 返回键尺寸 | `40dp` | 与顶部玻璃按钮一致 |
| 内容水平内边距 | `16dp` | 设置/收藏等列表页 |
| 内容顶部内边距 | `8dp` | 大标题 item 之前 |
| 大标题距内容顶部 | `52dp` | 落在返回键下方 |
| 大标题左边距 | `10dp` | 相对内容区 |
| 副标题左边距 | `12dp` | 相对内容区 |
| 大标题/副标题间距 | `6dp` | |
| 大标题字号 | `30sp` | |
| 小标题字号 | `16sp` | |
| 小标题相对顶栏中心上移 | `3dp` | `SmallTitleLift` |
| 小标题出现时上移 | `7dp` | `SmallTitleRise` |

设置页的最终位置关系：

```text
状态栏
└─ 顶部 overlay：56dp
   └─ 返回键：40dp，左边距 12dp，上边距 8dp
└─ 内容区：水平 16dp，顶部 8dp
   └─ 大标题 item：顶部留白 52dp
      └─ 大标题：左边距 10dp
      └─ 副标题：左边距 12dp，与大标题间距 6dp
```

## 组成

| 组件 | 作用 |
| --- | --- |
| `TeakChromeDefaults` | 顶栏高度、大/小标题字号、位置、模糊半径、折叠比例、返回键尺寸 |
| `TeakPageScaffold` | 页面背景、玻璃采样层、顶栏、返回键、overlay 插槽 |
| `TeakTopOverlay` | 顶部统一布局：返回键 + 左侧内容 + 中间内容 + 右侧操作 |
| `TeakTopGlassButton` | 顶部通用玻璃圆钮，与返回键共享 300ms 渐显 |
| `TeakTopBar` | 顶栏渐变模糊 + 小标题淡入 / 去模糊 / 上移 |
| `TeakLargeTitle` | 内容顶部的大标题，随滚动淡出并模糊 |
| `TeakPageScrollState` | 保存 `collapse` 和 `backRevealed` |
| `TrackTeakPageScroll` | 把 LazyColumn / LazyVerticalGrid / ScrollState 的滚动同步到 pageState |
| `TeakActivityScaffold` | App 侧主题 + 上述 page chrome 的 Activity 外壳 |

## 底部导航 / 玻璃 Tab

首页悬浮胶囊底栏和视频页 tab 栏也属于同一套规范。它们不是“参考重画”，
而是把 HyperPiliPlus 的原生实现直接收进 TeakCommonModule 复用；来源见 `CREDITS.md`。

| 项目 | 值 |
| --- | --- |
| 底栏展开 / 收起高度 | `52dp` / `40dp` |
| 底栏水平 / 垂直内边距 | `6dp` / `3dp` |
| 底栏最大宽度 | `320dp` |
| 单个 tab 最大宽度 | `80dp` |
| 底栏图标 / 独立图标 | `26dp` / 普通 `28dp`、宽折叠 `25dp` |
| 指示器左右溢出 | `3dp` |
| 视频 tab 高度 | `40dp` |
| 视频 tab 单项宽度 | `60dp` |
| 视频 tab 指示器高度 | `32dp` |
| 视频 tab 指示器弹簧 | `stiffness = 340` / `damping = 25` |

对应组件：

- `TeakFloatingTabBar` / `TeakFloatingTabBarDefaults`
- `TeakFloatingTabMotion` / `FloatingTabMotionState`
- `TeakFloatingNavigationShadow`
- `TeakPagerTabs` / `TeakPagerTabDefaults`

### 滚动收起与底部遮罩

- 主页面仅在宽折叠竖屏启用 Tab 滚动收起：向下累计实际消费 6dp 收起、反向累计 6dp 展开。横屏、普通窄竖屏始终展开。
- 页面底部渐进遮罩只能伴随可见的悬浮底部控件存在；控件隐藏后必须同步隐藏。经典贴边底栏和无悬浮控件页面不绘制。
- 主页面竖屏悬浮 Tab 的遮罩在内容采样层之外、底栏之前绘制，不拦截触摸；高度随导航栏 inset、底栏抬高和实际 Tab 高度变化，顶部额外留 24dp 渐隐区域。
- 楼中楼仅在悬浮回复框显示（`replyTarget != null`）时绘制底部遮罩。

## 页面接入规则

1. 页面根节点使用 `TeakActivityScaffold`（独立 Activity）或 `TeakPageScaffold`（页面内）。
2. 页面自己持有 `TeakPageScrollState`，推荐 `rememberTeakPageScrollState()`；多页面同时组合时不要共用 `TeakPageChrome.state`。
3. 列表 / 网格 / verticalScroll 的滚动状态传给对应的 `TrackTeakPageScroll(...)`。
4. 大标题必须用 `TeakLargeTitle(title, subtitle, pageState)`，不要再手写 `Text + graphicsLayer + blur`。
5. 标题与内容的间距由 `TeakLargeTitle` 的 `topSpacer` 和页面 `contentPadding` 控制，不要在页面里再补 52dp。
6. 返回键由 scaffold 内部统一交给 `TeakTopOverlay` 绘制；页面只传 `onBack`，不要再自己放 `TeakGlassBackButton`。
7. 右上角操作放进 `topBarActions`，用 `TeakTopGlassButton`，不要再手写 300ms 渐显玻璃按钮。
8. 需要覆盖层时放在 `overlay` 插槽，不要放进 `layerBackdrop` 子树。

## 参考实现

```kotlin
val pageState = rememberTeakPageScrollState()

TeakActivityScaffold(
    settings = settings,
    title = "收藏",
    onBack = onBack,
    pageState = pageState,
) {
    val listState = rememberLazyGridState()
    TrackTeakPageScroll(listState, pageState)

    LazyVerticalGrid(state = listState, ...) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            TeakLargeTitle(title = "收藏", pageState = pageState)
        }
        // 页面内容
    }
}
```

## 迁移状态

| 页面 | 状态 |
| --- | --- |
| 设置 | 已使用 `TeakSettingsPage`（内部是 `TeakLargeTitle`） |
| 收藏 / 历史 / 稍后再看 | 已统一到 `TeakLargeTitle`（`LibraryScreen`） |
| 消息 | 已迁移到 `TeakTopOverlay` + `topBarActions`，返回键和右上角操作共享同一套渐显 |
| 缓存 / 我的评论与弹幕 / 个人空间 | 待逐步迁移 |
| 视频页 | 特殊页面：返回键跟随播放器控件显隐，暂不走 `TeakTopOverlay` |

## 检查清单

- 大标题位置、字号、折叠模糊与其他页面一致；
- 小标题出现时机、260ms 补间一致；
- 返回键位置和玻璃显隐一致；
- 页面没有重复的 52dp / 30sp / 0.65f / 12dp 常量；
- 滚动到第二屏时返回键、小标题状态正确；
- 多页面切换时标题状态不会互相污染。

## 2026-11-12 · 宽屏滚动视口

`TeakScrollContainer(scrollState, modifier, maxWidth) { content() }`是项目自有视口。内容保持原LazyColumn/LazyGrid/verticalScroll及其状态，容器沿用600dp断点、70%宽度和680dp上限；左右留白以兄弟节点处理手势，共享内容的状态。禁止在内容祖先再叠加`scrollable(state = 同一个状态)`来实现留白滚动，否则内容拖动/fling会经过重复的同轴处理。

`TeakPageScaffold`在限定内容宽度时统一接入，覆盖设置、帮助、捐赠、致谢、缓存、日志及共用该外壳的列表。搜索落地页与两个结果列表显式接入；普通说明页同样接入。`centeredListWidth`仅做宽度测量，不再接受scrollState。空间的头部折叠联动、独立弹窗/播放器面板、合集内滚动不是此处的重复转发，保留各自的滚动契约。

只用一个列表/ScrollState记录位置；切换页面由原页面管理状态。窄屏没有可点击留白区域；容器不新增滚动偏移、不积分delta、不创建第二份列表状态。弹出宿主、backdrop、内容padding与标题几何保持原值。

反向LazyColumn/Grid的宽屏留白拖动从layoutInfo.reverseLayout读取方向；固定顶栏页面使用BindTeakPageScrollState单独绑定，不增加折叠/材质观察。绑定随composition释放，旧页面释放不会清理后来绑定的另一个state。
