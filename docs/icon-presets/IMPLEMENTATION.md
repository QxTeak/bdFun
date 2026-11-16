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
