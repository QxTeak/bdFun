# ICON-16 图标统一实现

来源：用户导出的 `issue-16.json`（基线 main 3566d1c）。本轮只实现图标部分。

## 规则与边界

- 应用78项替换，保留其余素材；所有扩展 Miuix 调用使用 `MiuixIcons.Name` 的默认 Regular（330），包括设置一级入口原先的 Normal。
- Basic.ArrowRight / ArrowUpDown / SearchCleanup 没有字重 API，保持预设选中的库内固定图形；不伪造 Regular 扩展。
- Lucide / Extended 打包28个共享 Android 矢量；24视口，线框1.8，填充92%中心收边。保留调用方场景的尺寸、颜色、按压和回调。
- 重复选择同一图形仅保留一份资源：headphones、picture-in-picture-2、captions、coins-filled 等；Miuix 继续使用库内缓存的同一矢量。删除已替换的菜单绘制帮助文件、旧投币/发送 PNG。
- 预设为明确选型；例如 Cast 与 ScreenMirroring、headphones 与 book-headphones-filled 是不同图形，不能因业务描述相近而强行合并。
- 预设只作为开发契约，不在运行时读取或引入网页。未新增依赖。素材来源和许可见 TeakUI/docs/PROVENANCE.md。

## 验证

- Debug 构建，app / TeakUI / danmaku-engine 单测通过。
- SVG 转换5项检查：继承描边与无填充、evenodd孔洞、圆角、拒绝不支持的变换、预设资源完整覆盖。
- Android独立包名对照页已装机渲染，检查全部28项Lucide与7项Miuix参考的浅/深底图形；临时 Activity 已移除。
- Kotlin PSI尺寸审计：1170文件、12777可调用节点，0违规。
- Debug / Release（含 R8）最终构建通过；首页装机截图已确认加载。完整视频与登录路径仍待手动回归。

## 手动回归清单（不能以图标对照页代替）

- [ ] 登录后的私信 / 评论表情、发送、菜单与选中状态。
- [ ] 视频进入 → 切清晰度 → 切分P → 全屏旋转 → 画中画 → 后台 → 退出 → 恢复进度。
- [ ] 直播 / 空间充电与认证 / 媒体库 / 搜索 / 消息筛选 / 网格选择的交互与无障碍。

## 选型对应

| 当前入口 | 统一目标 |
|---|---|
| BackChevron | miuix:ChevronBackward.Regular |
| MoreGlyph | miuix:More.Regular |
| CloseGlyph | miuix:Close.Regular |
| CheckGlyph | miuix:Ok.Regular |
| ChevronRightGlyph | miuix:Basic.ArrowRight |
| HeadphoneGlyph | lucide:headphones |
| ClockGlyph | lucide:clock |
| PipGlyph | lucide:picture-in-picture-2 |
| CastGlyph | lucide:cast |
| ViewerGlyph | lucide:users |
| 字符 ⋮ | miuix:More.Regular |
| 字符 ＞ | miuix:Basic.ArrowRight |
| 字符 ✔ | miuix:Ok.Regular |
| 字符 ➤ | lucide-extended:send-horizontal-filled |
| 字符 😀 | lucide:face-grinning |
| SearchSuggestGlyph | miuix:Search.Regular |
| FunnelGlyph | miuix:Filter.Regular |
| ResetGlyph | miuix:Reset.Regular |
| ChargeGlyph | lucide-extended:zap-filled |
| LiveCloseGlyph | miuix:Close.Regular |
| LiveCheckGlyph | miuix:Ok.Regular |
| ScrollTopCanvas0 | lucide:arrow-up |
| SpaceSignToggleCanvas0 | miuix:Basic.ArrowUpDown |
| LibrarySearchHeaderCanvas0 | miuix:ChevronBackward.Regular |
| LibrarySearchHeaderCanvas1 | miuix:Search.Regular |
| LibrarySearchHeaderCanvas2 | miuix:Basic.SearchCleanup |
| SpaceWidgetsCanvas0 | miuix:Basic.ArrowRight |
| AllFeedAvatarCanvas0 | miuix:All.Regular |
| LiveGesture.true | lucide:sun |
| LiveGesture.false | lucide:volume-1 |
| Miuix.ChevronBackward | miuix:ChevronBackward.Regular |
| Miuix.Normal.ContactsCircle | miuix:ContactsBook.Regular |
| act_coin | lucide-extended:coins-filled |
| ic_send | lucide:send |
| InlineBackGlyph | miuix:ChevronBackward.Regular |
| InlineHomeGlyph | miuix:Home.Regular |
| InlineHeadphoneGlyph | lucide:headphones |
| InlineCastGlyph | miuix:ScreenMirroring.Regular |
| InlineMoreGlyph | miuix:More.Regular |
| DanmakuSettingsGlyph | miuix:Tune.Regular |
| BackgroundPlayGlyph | lucide:headphones |
| AutoPlayNextGlyph | lucide:circle-fading-play |
| LoopGlyph | lucide:repeat-2 |
| MirrorGlyph | lucide:mirror-rectangular |
| BackgroundListenGlyph | lucide-extended:book-headphones-filled |
| ResizeGlyph | miuix:ExpandMore.Regular |
| ColorAssistGlyph | lucide:circle-pile |
| SpeedGlyph | lucide:gauge |
| TimerGlyph | lucide:timer |
| SubtitleGlyph | lucide:captions |
| DanmakuSettingGlyph | miuix:Tune.Regular |
| DislikeGlyph | lucide:face-slightly-frowning |
| WatchLaterGlyph | lucide:clock-fading |
| CacheGlyphSmall | miuix:Download.Regular |
| PipGlyphSmall | lucide:picture-in-picture-2 |
| CastGlyphSmall | miuix:ScreenMirroring.Regular |
| LockGlyph.true | miuix:Lock.Regular |
| LockGlyph.false | miuix:Unlock.Regular |
| Menu.Reply | miuix:Reply.Regular |
| Menu.Copy | miuix:Copy.Regular |
| Menu.Report | miuix:Report.Regular |
| Menu.Block | miuix:Blocklist.Regular |
| Menu.Delete | miuix:Delete.Regular |
| Menu.Coin | lucide-extended:coins-filled |
| Menu.Star | lucide:star |
| Danmaku.Fixed | lucide:captions |
| Danmaku.Scroll | lucide:arrow-left-right |
| Danmaku.Color | lucide:palette |
| Danmaku.Advanced | lucide:graph-star |
| Danmaku.Count | lucide:lamp-ceiling |
| MarkRead | miuix:Community.Regular |
| ThreeDots | miuix:More.Regular |
| Comment | miuix:Reply.Regular |
| 字符 › | miuix:Basic.ArrowRight |
| 字符 ⌄ | miuix:Basic.ArrowUpDown |
| 字符 ✓ | miuix:Ok.Regular |
| 字符 ✓ | miuix:Ok.Regular |
| 字符 ✕ | miuix:Close.Regular |

## 合入最新 main 后的补充

- 已合入 origin/main `ecd7582`（PR #67，preview-6.0），合并提交 `4070e8c9`。
- 10类字符替换的导出映射保留原 currentId，同时添加 legacyName 与 implementation，更新为矢量访问名、当前源码行号；旧预设仍按 stable ID 导入。本地尚未推送的源码位置明确标为本地实现，不生成无效的 GitHub 提交链接。
- AppGlyph 与底栏共享 Painter 使用双倍分辨率矢量缓存再缩小。底栏现有 AnimatedContent 与 graphicsLayer 有非整数缩放，先前会继续缩放当前尺寸的像素缓存；此改动提高缓存分辨率，保持图形、字重、布局和动画参数不变。仅处理 VectorPainter，不把保留的 PNG 伪装成矢量。
- 提升采样不会恢复原 PNG 已丢失的边缘细节，也不能替代用户设备上的对照验收；真机锯齿反馈仍需复核。

## 原生位图与清单漏项补齐

- 「我的」页 `MineRow` 统一使用独立箭头，`全部 ＞`、`查看全部 ＞` 拆为文案和 `MiuixIcons.Basic.ArrowRight`，不改变点击区域与回调。
- 详情互动、评论赞踩、全屏控件、视频卡片计数共 21 张 PNG 改为同名 VectorDrawable。保留原轮廓、留白及全屏弹幕开启的粉色标记；资源 ID 与已有预设 ID 不变。固定轮廓存于 `tools/icons/bili-vectors.json`，使用 `python3 tools/icons/generate_bili_vectors.py` 重建资源，无运行时依赖。曲线来自原 alpha 轮廓的样条拟合，非原设计矢量源；原始文件 SHA-256 与阈值轮廓重合率随数据保存。
- `PixelIcon` 不再通过 BitmapFactory 强制解码；`ResourceGlyph`、评论行和卡片计数接入矢量双倍缓存入口。原有尺寸、着色、按压与回调保持。
- 清单新增 7 项：合集标题、回顶、回底、定位当前集、展开箭头，以及详情独有的播放量和弹幕数。卡片上的计数图标保留原项并明确标为「视频卡片」，不再与详情 Canvas 混为同一图形。新增项默认保留、待审阅，不替用户选择图标。
- 妙搭目录为 167 项，候选库为 5848 项；新增「App 原生」库，提供实际矢量预览。完整旧版 160 项预设可导入，新增 7 项自动补为保留/待审阅；重复 ID、未知项、任意截断清单仍拒绝且不覆盖当前选择。
- 验证：生成器测试、分层/尺寸检查、debug 编译、app/TeakUI/弹幕测试及网页交互回归通过。发布包使用同一官方证书、V2、preview-6.0/code600。
- 本机无已连接 Android 设备，未声称真机锯齿验收通过。装机需检查：我的页所有箭头；详情互动静止/按压/选中；评论赞踩切换；横屏互动与弹幕开启/关闭/设置；合集回顶/回底/定位/展开；卡片与详情计数图标，并检查浅色/深色与系统缩放。
