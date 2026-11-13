# Deadliner glass and adaptive layout sync

Source: AritxOnly/Deadliner local working tree, based on `3b561656e1ddca43220a39763f12aaa0c67fdaba`, captured 2026-11-10. This includes the author's uncommitted wide/fold header, readable-list width and glass button changes. Source file hashes are recorded in `deadliner-source-sha256.json`. Remote HEAD `d17b21e0c3483cb563421f83a6d8b249eaca77c7` was checked; the compared remote changes do not replace the glass implementation used here.

This replays PR #4 onto the rewritten preview-5.0 main (`37bef5f`, including the documentation updates after `a2e9b94`). It preserves the new search experience, onboarding, collapsible space header, scroll-to-top registration, and background playback disc. Search tracks only the active result tab; the space header combines actual header/list displacement with its existing banner reveal gate.

The app toolchain is aligned with that pinned Miuix source (AGP 9.5.1, Kotlin 2.4.21, Compose 1.12.2). The original app AGP 9.4.1 cannot participate in the composite build with Miuix AGP 9.5.1. This alignment avoids overriding or editing third-party source build files.

## Material compatibility

The full optical stack, shader, sampling padding, edge helpers, surface/icon/button/FAB, shadow host, and progressive header are ported into TeakUI. Package names and theme/backdrop adapters use the existing Teak contracts. The preview-5.0 Miuix source pin remains unchanged: Miuix 0.9.4 coordinates are substituted with the pinned source snapshot `45645854cd46c32275c5666e3aafa9980d8f104e` when available (see the repository README).

bdFun's existing `TeakMaterialTuning`, `TeakMaterialFineTuning`, `TeakModalMaterialTuning`, recipe coefficients and light/dark shadow radius/spread/alpha remain unchanged. The progressive host also uses bdFun's existing top-bar blur radius (16), tint alpha (0.66), gradient start (0.62), end (1) and curve (1.25). The existing base wash is retained, now inside materialAlpha so it disappears together with the glass. The new rim-width field uses Deadliner's 0.6 dp default. Official MIUIX glass is supplied by preview-5.0's existing dependency, with no duplicate package vendored; the optional official-material path defaults off to preserve bdFun's material settings. Non-shader and no-backdrop fallbacks remain available.

The new general button and FAB are reusable library components; existing app controls retain their callbacks. The app's navigation-specific badges and classic style remain in the existing tab component. No unrelated Deadliner business screens or assets are imported. Deadliner MIT license is retained under `TeakUI/third_party/deadliner`.

## Layout and scroll ownership

- Actual list/grid position drives smoothstep glass reveal over 4–28 dp, independent of large-title collapse. Returning to the first item clears the material. Home and dynamic tabs have independent state; Activity pages provide scoped state.
- One backdrop records content only. The progressive background and button shadow foreground sit outside that recorder. Header buttons share the same animated material progress.
- Fixed wide/fold headers use Deadliner's window policy: width >= 400 dp and height/width <= 1.65. The main navigation moves to a rail for these windows at width >= 600 dp, preserving the same pager selection.
- Home uses adaptive video columns on wide viewports; dynamic uses an author sidebar and bounded feed at >= 840 dp; Mine uses two card columns at >= 600 dp.
- Secondary titled pages center content using Deadliner's policy: at >= 600 dp use min(70% of viewport, 680 dp). Registered lists can be scrolled from the gutters. Search and terms content also use bounded columns. The video page uses its dedicated player/details layout instead of the generic readable-list width.
- Video landscape windows at least 600 dp wide use a 70/30 split with a 300 dp minimum details column. The existing player and controls fill the remaining left column with aspect-fit video. Details, comments, reply input and expanded replies stay on the right; danmaku controls stay on the left. Rotation enters this layout, while the fullscreen control explicitly enters/exits exclusive video and restores the entry orientation.
- Main/terms Activity portrait locks are removed so window rotation reaches the adaptive layout.

## Validation

Run `:app:assembleDebug :app:testDebugUnitTest :TeakUI:testDebugUnitTest`. Host tests cover wide/fold boundaries, density-independent reveal, top reset and independent tab states, and video pane minimum-width/ratio/compact-window boundaries. Emulator checks and any limitations are recorded in the PR.

### 2026-11-11 宽屏反馈修正

- 宽屏主页保留横向浮动 Tab，左下对齐；右下独立搜索按钮打开同一搜索状态。移除侧边导航占位；宽屏不显示后台播放黑胶。搜索框靠左，宽度为窗口的 45%，限制在 280–480dp。
- 首页顶栏在该轮曾改为常亮；后续真机反馈已恢复实际滚动驱动的渐显，见下节。
- 推荐网格按 180–260dp 卡片宽度自适应（常见展开宽度四列），窄屏保留原卡片样式设置。
- 宽折叠顶栏 Actions 使用和窄屏相同的 12dp 右侧安全区内边距、8dp 按钮间距；正文限宽不作用于顶栏。消息操作图标使用固定 Miuix 源码中的 `MiuixIcons.Delete`，保留原有全部已读行为。
- 播放页背景铺满窗口，安全区只约束内容与控件；右侧详情延伸至状态栏，弹幕输入入口与开关放在简介/评论右侧。窄栏统计按整项换行，五个互动操作等分可用宽度。
- 非全屏播放采用系统用户方向策略，旋转/折叠变化不会留下强制竖屏限制；分栏按当前测量约束更新。Manifest 显式关闭系统 Activity embedding override，由应用维护自己的分栏，参见 [Android activity embedding](https://developer.android.com/develop/ui/views/layout/activity-embedding)。这不宣称能覆盖所有厂商自定义的平行视窗实现。

### 平板与状态栏边界修复（2026-11-11）

- Android 窗口启用 edge-to-edge / display-cutout ALWAYS，并使用当前主题填充 window background；关闭 navigation bar contrast scrim，避免播放器侧边安全区露出默认白底。
- 固定宽屏顶栏仅保留顶部系统栏间距，不再对整个标题栏施加横向系统 inset；标题栏仍位于内容限宽层之外。
- 消息和设置列表将状态栏留白放入 contentPadding，滚动视口及 backdrop 覆盖窗口顶部。
- 空间页关闭外壳单列限宽，改为自行管理宽屏资料/内容分栏；窄屏使用同一 LazyColumn 承载头图与内容，去掉消费拖动距离的手动折叠头部。
- 上述修改只涉及布局、窗口和滚动状态；玻璃的光学参数、配方与渲染 fallback 保持原值。

### 真机反馈：吸顶分段栏、瀑布流及播放窗口（2026-11-11）

- 首页和动态页重新使用实际滚动位置驱动 flat → soft-glass；按钮、搜索框、分类栏材质与 progressive background 共享同一进度。新增 LazyStaggeredGridState 的 chrome / 回顶适配。
- 动态页仅显示全部、视频、番剧三个固定分类；UP 筛选仍使用 host_mid。736dp 起显示 148–196dp 作者侧栏，右侧使用真正的 LazyVerticalStaggeredGrid，每卡 280–560dp；窄屏仍为单列与横向作者栏。
- 空间宽屏资料区占约 40%（280–440dp），内容独立滚动，分段栏固定在右侧顶栏。窄屏通过列表占位标记定位分段栏，滚至顶栏下方后吸附；实际玻璃分段栏只画在 backdrop 采集层之外。左右顶栏分别拥有一个 material host，保留独立滚动进度。
- 空间头图不再套用视频封面裁剪参数，支持 Drawable 解码与缓存；移除固定明暗滤镜。返回图标按头图亮度和滚动位置在黑白/主题色间作 240ms 过渡，不再显示空间顶栏标题。
- 通用大标题上方留白缩短 12dp；消息容器与设置统一为 22dp 连续圆角。未读角标封装为 UnreadBadgeHost，在按钮玻璃裁剪层之外绘制并预留过渡动画所需边界。
- 播放面板与 tabs 移除横向 safeDrawing inset；播放器只保留顶部状态栏和底部导航栏避让。移除左侧状态栏纯色 surface 遮罩，以及外壳空顶栏的第二层渐进模糊；保留视频页自己的过渡。返回键统一 Miuix ChevronBackward，搜索清空使用 Miuix Close。
- 新增“视频页退出不会继续播放”（默认关闭以保留已有偏好）。开启后，返回离开视频页或关闭 PiP 会暂停该 Activity 的播放器并禁止交接到后台；切到桌面仍进入 PiP。按 Activity 隔离暂停回调，避免旧页销毁误停新视频。PiP 隐藏详情、页级悬浮控件及播放操作层。生命周期依据 [Android PiP guidance](https://developer.android.com/develop/ui/views/picture-in-picture)。
- 本轮依用户要求不运行模拟器；编译、单元测试与真机视觉验证分开记录。未更改柔光玻璃的配方、光学参数及 fallback。

### 2026-11-13 当前阴影同步

按用户要求，全局玻璃阴影以 Deadliner 当前本地工作区的 `ui/material/glass/SoftGlassShadow.kt` 为源，基于提交 `3b561656e1ddca43220a39763f12aaa0c67fdaba`（源文件为工作区新增/修改内容，并非声称全部来自该提交）。源文件 SHA-256：`baa6a84f6bbd8654ea7f51a3b95110f052fdfa00c06c64fda6c340e6caafe0a3`。同步完整三层阴影参数解析、明暗/官方材质分支、预设和顶栏补偿；不修改其他光学配方。

默认浅色 BalancedDiffuse：ambient 26dp / spread -2dp / alpha .03459375，outer 10dp / -1dp / .06490625，edge 2dp / -.5dp / .0094375；顶栏 ambient 为34dp / .043453125。深色与官方浅色参数保持源实现。视频详情在对应 Standard 或 TopBarControl 基线上仅将三层 alpha 乘 1.10，半径、spread、offset 与预设选择不变；移除上一轮独立的浓阴影配方。历史段落的“阴影参数不变”描述的是当时同步范围，由本检查点更新。


### 2026-11-13 · issue #16 底栏几何与宽折叠滚动对齐（部分）

对照本地 Deadliner 工作区（HEAD `3b561656e1ddca43220a39763f12aaa0c67fdaba`，含尚未提交的滚动收起实现）：

- 两边展开高度均统一为 52dp，收起高度保持 40dp；Deadliner 不再按屏幕比例切换 54/52dp。
- bdFun 胶囊上限由 380dp 改为 320dp，单项上限由 80dp 改为 72dp；水平内容边距由 7dp 改为 6dp。垂直边距和指示器横向溢出保持共同的 3dp，tab 图标保持 26dp。
- 独立搜索按钮随底栏缩高；图标普通形态 28dp、宽折叠 25dp，紧邻胶囊的间距分别 8/12dp，dock 水平内边距 8dp。宽屏左右分置的产品布局保留。
- 宽折叠外层水平留白 12dp、底部基础留白 0dp，普通形态分别 16dp/4dp；统一避让系统导航栏，保留 bdFun 的额外抬高选项。
- 宽折叠窗口（包含不足 600dp 的外屏）向下滚动累计 6dp 收起，反向累计 6dp 展开；仅统计实际消费的用户竖向滚动，不统计 fling、横向翻页或边界剩余位移。切 tab、切形态与打开搜索均重建方向状态。标签跟随收起隐藏，Tab 保留完整无障碍名称。
- 几何默认值和标签测量从既有底栏文件拆出；滚动状态由主页面壳持有，未改玻璃材质、角标或业务动作。

验证：bdFun debug 构建及 app JVM 测试通过（包含 2 个新增方向/阈值测试）。未做装机视觉验收。待手动核对：普通/宽折叠/600dp 两侧的窗口切换、慢拖与反向恢复、切 tab、搜索开关、导航栏与额外抬高、角标和后台唱片。不代表 issue #16 的图标、Dialog 等其他事项已完成。


### 2026-11-13 · 底部遮罩与收起范围补充（issue #16 部分）

- 主页面竖屏悬浮 Tab 新增从透明到页面底色的底部渐进遮罩，随底栏的搜索/退场动画同步显隐，不绘入玻璃采样树，不拦截操作；经典贴边底栏与横屏不添加。
- 楼中楼的 54dp 底部渐变改为与 `replyTarget != null` 的悬浮回复框共同显示，浏览回复时移除。
- bdFun 滚动收起范围进一步限定为宽折叠竖屏（窗口高大于宽，且满足既有宽折叠比例策略）；横屏与普通窄竖屏固定展开至 52dp。这覆盖上节更宽的滚动适用范围。
- 手动视觉验收待完成：浅色/深色、回复框开关、竖横屏切换、搜索进退场、底栏抬高和系统导航栏。此项不代表 issue #16 已全量完成。

验证：最新 `:app:assembleRelease :app:testDebugUnitTest` 通过，424 个 app JVM 测试全部通过，分层检查与 diff 空白检查通过。release APK 沿用本地 debug 签名回退；当前无连接模拟器，装机视觉验收仍待完成。
