# 底栏搜索（BottomSearch）

2026-11-12 新增，2026-11-13 并入「界面风格」。

在「设置 → 外观 → 界面风格」里选 **HyperOS 4** 才会出现这一套：
悬浮胶囊底栏 + 右侧圆形搜索按钮 + 搜索页一镜到底转场。
选「官方」或「经典」时，底栏、搜索入口和唱片位置都和原来完全一致。

HyperOS 4 下首页/主壳的唱片叠进搜索键（`MainDockStack`），不再单独占位；
首页外的独立页面没有底栏，唱片按 B 站悬浮播放器的位置贴右下角（16dp 边距、抬离导航栏 16dp）。

## 入口与交互

- 入口：悬浮胶囊底栏右侧多一个圆形玻璃按钮（Miuix 默认 `Search` 图标，48dp）。
  宽屏（平板/折叠展开）下沿用底栏右端那个已有的圆形按钮，行为随开关切换。
- 点击：整块首页内容上移、缩小并模糊，搜索页从下方推入；返回键先退回搜索首页
  （记录 / 热搜），再退出搜索。

## 动效参数来源

反编译 `文件管理_9.3.0.8.apk`（包名 `com.android.fileexplorer`，MIUI 文件管理 9.x）
得到的 `com.android.fileexplorer.fragment.SearchTransitionAnimator` 是纯 APP 自研实现，
没有调用系统接口；它用的是 `miuix.animation.Folme` + `RenderEffect`。关键数值：

| 元素 | 原实现 | 本项目对应 |
| --- | --- | --- |
| 底层内容位移 | `TRANSLATION_Y = -106dp` | `TeakSearchTransitionDefaults.ContentLift` |
| 弹簧 | `FolmeEase.spring(0.97f, 0.4f)` | `spring(dampingRatio=0.97, stiffness=247)` |
| 底层顶栏/吸顶栏 | `SCALE = 0.9`、`ALPHA` 线性 150ms | `UnderlayScale = 0.92` + 整体上移 |
| 底层模糊 | `RenderEffect` 0 → 50px，线性 150ms | `UnderlayBlurPx = 50f`（随进度插值） |
| 搜索遮罩 | `ALPHA` 线性 100ms | `ChromeFadeMs = 100` |
| 搜索栏 / 容器 | `ALPHA` 线性 100ms + `delay(50)` | `ChromeFadeDelayMs = 50` |
| 退出 | 上述全部反向 | 同一个 Animatable 反向弹回 |

参数集中在 `TeakUI/.../common/search/TeakSearchTransition.kt`，
改一处就能整体调手感。

> 备注：需求截图里那个「照片 / 影集」大胶囊不在这份 APK 里（反编译后
> `resources.arsc` 里没有「影集」字符串），所以按 APK 实际存在的搜索转场来实现。

## 搜索页

- 顶部：返回按钮 + 实色搜索框（可清除）+ 右侧「搜索 / 取消」。
- 未提交：输入联想（防抖 180ms）→「搜索记录」→「大家都在搜」前十。
- 已提交：`视频 / UP主` 分段栏 + 结果列表，数据走 `BiliApi.search` 与
  `BiliApi.searchUsers`，点击进入现有视频页 / 个人主页。

## 观感修正（第二轮）

- **页面内不用玻璃**：搜索页的返回键、搜索框、结果页分段栏都改成实色表面
  （`surfaceContainerHigh` / `surface`），不再采样 backdrop。只有底栏那个入口按钮还是玻璃。
- **两段式渐隐/渐显**：展开时底层首页整体 `alpha 1 → 0`（卡片渐隐退出，同时上移 + 缩 + 模糊）；
  搜索页底色按 100ms 淡入，卡片按 `bottomSearchReveal` 每张错开 0.12 的进度渐显并上浮 16dp。
  退出时全部反向。
- **入口按钮尺寸**：圆形按钮直径改成和胶囊底栏同高（`TeakFloatingTabBarDefaults.Height`，54dp）。
- **性能**：完全展开后底层直接 `alpha = 0` 并跳过全屏模糊，debug 包里不再每帧跑一遍 RenderEffect。


## 第三轮：逐项对齐反编译

| 元素 | 官方实现 | 现状 |
| --- | --- | --- |
| 正文位移 | `TRANSLATION_Y = -106dp`，`spring(0.97, 0.4)` | 对齐 |
| 正文缩放 | 不缩放（只平移） | 对齐（`UnderlayScale = 1f`） |
| 底层模糊 | 独立 `ValueAnimator` 0→50px，150ms 线性 | 对齐（独立进度，不再跟弹簧） |
| 底层淡出 | `ALPHA → 0`，`linear(150ms)` | 对齐 |
| 遮罩 | 进入 `linear(100ms)` / 退出 `linear(100ms) + 50ms` | 对齐 |
| 容器与搜索栏 | 进入 `linear(100ms) + 50ms` / 退出 `linear(100ms)` | 对齐 |
| 顶栏 | 独立 View：缩到 0.9（跟弹簧）+ `linear(150ms)` 淡出 | 对齐（已从内容层拆出，单独变换） |
| 底栏 | 独立 miuix Navigator：`spring(0.97, 0.3)` + 下坠 + 淡出 + 1→0.6 缩放 | 对齐 |
| Activity 窗口过渡 | 新 Activity：进入 `Fade 300ms + startDelay 60ms + SineEaseInOut`，返回 `Fade 300ms + spring(0.99, 0.67)` | 对齐（同一 Activity 内用整页 alpha 复刻同一条时间线） |

搜索页顶栏布局按**首页顶栏**对齐：`12dp` 外边距 + `64dp` 高 + 状态栏内边距，
左 38dp 圆形（对应头像位）、10dp 间距、40dp 搜索胶囊（weight 1）、10dp 间距、右 40dp 圆形（对应消息位）。
Activity 窗口过渡现在是同一 Activity 内的一层整页 alpha，时间线与官方一致；
差别只在合成层级（官方是独立 window surface），肉眼不可见。

## 涉及文件
- `TeakUI/src/main/kotlin/com/aritxonly/teak/common/search/TeakSearchTransition.kt`（新增）
- `app/src/main/kotlin/com/aritxonly/bdfun/ui/BottomSearchPage.kt`（新增）
- `app/src/main/kotlin/com/aritxonly/bdfun/App.kt`（入口按钮、动画驱动、返回键、触控拦截）
- `app/src/main/kotlin/com/aritxonly/bdfun/data/SettingsStore.kt`（`bottom_search`，默认 false）
- `app/src/main/kotlin/com/aritxonly/bdfun/ui/SettingsScreen.kt`（开关项）

## 分层分支接线（2026-11-13）

以main d0a9ed3最终代码为准：入口仅在界面风格=2（HyperOS 4）显示，不保留早期文案中的独立开关。旧bottom_search键在原设置迁移流程中清理。原TeakSearchTransition参数保持，搜索请求进入feature/search/application/BottomSearchStateHolder，原布局拆到presentation/search/bottom；主壳拥有动画和输入生命周期，AppContainer提供仓储装配。两套入口不共享请求状态。宽屏留白滚动沿用分支已有TeakScrollContainer，内部列表继续持有原滚动状态。

## issue #36：共用搜索界面

HyperOS 4保留BottomSearchFrame及主壳八条转场时间线；内部直接调用与顶部入口相同的MainSearchLayer/SearchOverlay和MainHomeBarContent/HomeTopBar。记录、联想、结果卡片、用户页签及筛选完全共用；两个入口独立创建同一SearchStateHolder，底部专用控制器/展示卡片/测试副本已删除。工厂从AppContainer经主壳注入。关闭、提交、返回搜索首页仍作用于各自输入会话。

验证：checkLayering、现有SearchStateHolderTest及assembleDebug通过（/tmp/bdfun-issue36-shared.log，1m39s）；人工检查清单：两种入口的热词/历史/输入/提交/视频与UP主切换/筛选/清除/取消，HyperOS 4展开和返回动画，关闭后再打开不残留结果。未执行真实账号写操作。
