# Issue 16：现有图标审阅清单

基线：2026-11-16，origin/main `3566d1c`。当前阶段仅盘点，尚未替换图标。

## 审阅范围

扫描 app、feature、TeakUI 的 main Kotlin 与 app drawable-nodpi，按资源和绘制实现去重，保留同义图标的不同实现与状态。图册共 160 条 / 8 页；包含附属素材，不能将此数字理解为 160 个独立操作图标。

图册通过临时、独立包名的 Android Compose 页面渲染；自绘调用原实现或复制原 Canvas，未用 AI 重画。深色底与放大仅用于审阅，不代表最终尺寸或颜色。图形嵌在复杂组件中的少数装饰、进度滑块、外部加载的头像/表情和动画状态不属于静态操作图标，后续替换须回到实际页面复核。

本机图册和完整引用清单位于 `.local/artifacts/issue-16-icons/`，不纳入源码。

## 来源统计

- 自绘：58
- 字符：10
- 嵌入式自绘：8
- Miuix：29
- PNG：32
- 附属素材：23

## 统一准备

- 按 issue 16 引入 Deadliner 的 Hybrid MIUIX；先建立语义、来源和尺寸映射。
- B 站语义：点赞/投币/收藏/弹幕/评论等，逐项确认保留或使用官方素材。
- 优先审阅重复实现：返回、更多、关闭、勾选、搜索、耳机、投屏、画中画。
- 当前清单中的附属素材不默认替换；PNG 中存在仅定义或打包、无直接调用的资源。
- 此次没有修改播放器、TeakUI 或业务逻辑。

## 图册索引

| 页 | 格 | 名称 | 类型 | 来源示例 |
|---|---|---|---|---|
| 1 | 1 | BackChevron | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 2 | MoreGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 3 | CloseGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 4 | CheckGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 5 | ChevronRightGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 6 | HeadphoneGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 7 | ClockGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 8 | PipGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 9 | CastGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDrawing.kt |
| 1 | 10 | ViewerGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenStatus.kt |
| 1 | 11 | 字符 ⋮ | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/ui/MessageListComponents.kt |
| 1 | 12 | 字符 ＞ | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/mine/MineRecent.kt |
| 1 | 13 | 字符 ✔ | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/space/SpaceProfileDetails.kt |
| 1 | 14 | 字符 ➤ | 字符 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/FullscreenDanmakuComposer.kt |
| 1 | 15 | 字符 😀 | 字符 | 仅打包素材 / 无直接引用 |
| 1 | 16 | SearchSuggestGlyph | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/ui/search/SearchLandingComponents.kt |
| 1 | 17 | FunnelGlyph | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/ui/search/SearchResultsHeader.kt |
| 1 | 18 | ResetGlyph | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/danmaku/DanmakuPanelReset.kt |
| 1 | 19 | ChargeGlyph | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/space/SpaceProfileDetails.kt |
| 1 | 20 | LiveCloseGlyph | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/live/LivePlaybackSettings.kt |
| 2 | 1 | LiveCheckGlyph | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/live/LivePlaybackSettings.kt |
| 2 | 2 | ScrollTopCanvas0 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/ui/ScrollTop.kt |
| 2 | 3 | SpaceSignToggleCanvas0 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/space/SpaceSignToggle.kt |
| 2 | 4 | LibrarySearchHeaderCanvas0 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/library/LibrarySearchHeader.kt |
| 2 | 5 | LibrarySearchHeaderCanvas1 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/library/LibrarySearchHeader.kt |
| 2 | 6 | LibrarySearchHeaderCanvas2 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/library/LibrarySearchHeader.kt |
| 2 | 7 | SpaceWidgetsCanvas0 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/space/SpaceWidgets.kt |
| 2 | 8 | AllFeedAvatarCanvas0 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/feed/AllFeedAvatar.kt |
| 2 | 9 | SpaceProfileAvatarCanvas0 | 嵌入式自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/space/SpaceProfileAvatar.kt |
| 2 | 10 | LiveGesture.true | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/live/LiveGestureTip.kt |
| 2 | 11 | LiveGesture.false | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/live/LiveGestureTip.kt |
| 2 | 12 | Miuix.Album | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/shell/MainDestinations.kt |
| 2 | 13 | Miuix.Basic.ArrowRight | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/pgc/BangumiRankPage.kt |
| 2 | 14 | Miuix.ChevronBackward | Miuix | TeakUI/src/main/kotlin/com/aritxonly/teak/common/chrome/TeakGlassBackButton.kt |
| 2 | 15 | Miuix.Close | Miuix | TeakUI/src/main/kotlin/com/aritxonly/teak/common/modal/TeakActionSheet.kt |
| 2 | 16 | Miuix.Contacts | Miuix | feature/home/src/main/kotlin/com/aritxonly/bdfun/feature/home/presentation/HomeVideoActionSheet.kt |
| 2 | 17 | Miuix.ContactsCircle | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/shell/MainDestinations.kt |
| 2 | 18 | Miuix.Delete | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/message/MessageFeedSheet.kt |
| 2 | 19 | Miuix.Download | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/mine/MineActions.kt |
| 2 | 20 | Miuix.Folder | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/feature/library/presentation/FavoriteFolderSheet.kt |
| 3 | 1 | Miuix.Home | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/shell/MainDestinations.kt |
| 3 | 2 | Miuix.Info | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/onboarding/NoticePage.kt |
| 3 | 3 | Miuix.Messages | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/message/MessageShortcutGroup.kt |
| 3 | 4 | Miuix.More | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowMore.kt |
| 3 | 5 | Miuix.Normal.Background | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/SettingsCategoryIcon.kt |
| 3 | 6 | Miuix.Normal.ContactsCircle | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/SettingsCategoryIcon.kt |
| 3 | 7 | Miuix.Normal.Home | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/SettingsCategoryIcon.kt |
| 3 | 8 | Miuix.Normal.Info | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/SettingsCategoryIcon.kt |
| 3 | 9 | Miuix.Normal.Messages | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/SettingsCategoryIcon.kt |
| 3 | 10 | Miuix.Normal.Play | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/SettingsCategoryIcon.kt |
| 3 | 11 | Miuix.Normal.Settings | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/SettingsCategoryIcon.kt |
| 3 | 12 | Miuix.Playlist | Miuix | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/metadata/VideoCollectionHeader.kt |
| 3 | 13 | Miuix.Promotions | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/message/MessageShortcutGroup.kt |
| 3 | 14 | Miuix.Recent | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/mine/MineActions.kt |
| 3 | 15 | Miuix.Refresh | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/live/LiveChromeControls.kt |
| 3 | 16 | Miuix.Search | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/library/LibraryChrome.kt |
| 3 | 17 | Miuix.SearchDevice | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/mine/MineActions.kt |
| 3 | 18 | Miuix.Settings | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/mine/MineSettingsCard.kt |
| 3 | 19 | Miuix.VolumeOff | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/feed/FeedVideoMetrics.kt |
| 3 | 20 | Miuix.VolumeUp | Miuix | app/src/main/kotlin/com/aritxonly/bdfun/presentation/feed/FeedVideoMetrics.kt |
| 4 | 1 | act_coin | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/VideoMetadataAssembly.kt |
| 4 | 2 | act_dislike | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/VideoMetadataAssembly.kt |
| 4 | 3 | act_like | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/VideoMetadataAssembly.kt |
| 4 | 4 | act_like_outline | PNG | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakIcons.kt |
| 4 | 5 | act_share | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/VideoMetadataAssembly.kt |
| 4 | 6 | act_star | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/VideoMetadataAssembly.kt |
| 4 | 7 | bili_lv1 | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowAuthor.kt |
| 4 | 8 | bili_lv2 | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowAuthor.kt |
| 4 | 9 | bili_lv3 | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowAuthor.kt |
| 4 | 10 | bili_lv4 | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowAuthor.kt |
| 4 | 11 | bili_lv5 | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowAuthor.kt |
| 4 | 12 | bili_lv6 | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowAuthor.kt |
| 4 | 13 | comment_bubble | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/feed/FeedCardFooter.kt |
| 4 | 14 | comment_dislike | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowDislike.kt |
| 4 | 15 | comment_dislike_filled | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentRowDislike.kt |
| 4 | 16 | comment_like | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentPreviewLike.kt |
| 4 | 17 | comment_like_filled | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/row/CommentPreviewLike.kt |
| 4 | 18 | dev_artix | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/IndexedSettingsActions.kt |
| 4 | 19 | dev_teak | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/page/IndexedSettingsActions.kt |
| 4 | 20 | donation_alipay | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/DonationScreen.kt |
| 5 | 1 | donation_qr | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/DonationScreen.kt |
| 5 | 2 | fs_comment | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 3 | fs_danmaku_off | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 4 | fs_danmaku_on | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 5 | fs_danmaku_settings | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 6 | fs_like | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 7 | fs_next | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 8 | fs_play | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 9 | fs_share | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 10 | fs_star | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/FullscreenChromeAssembly.kt |
| 5 | 11 | ic_fullscreen | PNG | app/src/main/kotlin/com/aritxonly/bdfun/playback/InlinePlayerAssembly.kt |
| 5 | 12 | ic_image | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/composer/ComposerIconButton.kt |
| 5 | 13 | ic_launcher_adaptive_background | 附属素材 | 仅打包素材 / 无直接引用 |
| 5 | 14 | ic_launcher_adaptive_foreground | 附属素材 | 仅打包素材 / 无直接引用 |
| 5 | 15 | ic_launcher_foreground | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/onboarding/BdFunMark.kt |
| 5 | 16 | ic_metric_danmaku | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/video/card/MetricGlyph.kt |
| 5 | 17 | ic_metric_play | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/video/card/MetricGlyph.kt |
| 5 | 18 | ic_seek_thumb | PNG | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakIcons.kt |
| 5 | 19 | ic_send | PNG | app/src/main/kotlin/com/aritxonly/bdfun/presentation/comment/composer/ComposerIconButton.kt |
| 5 | 20 | teak_loading_00 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 1 | teak_loading_01 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 2 | teak_loading_02 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 3 | teak_loading_03 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 4 | teak_loading_04 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 5 | teak_loading_05 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 6 | teak_loading_06 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 7 | teak_loading_07 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 8 | teak_loading_08 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 9 | teak_loading_09 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 10 | teak_loading_10 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 11 | teak_loading_11 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 12 | teak_loading_12 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 13 | teak_loading_13 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 14 | teak_loading_14 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 15 | teak_loading_15 | 附属素材 | app/src/main/kotlin/com/aritxonly/bdfun/ui/TeakLoading.kt |
| 6 | 16 | InlineBackGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/inline/PlayerGlyphs.kt |
| 6 | 17 | InlineHomeGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/inline/PlayerGlyphs.kt |
| 6 | 18 | InlineHeadphoneGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/inline/PlayerGlyphs.kt |
| 6 | 19 | InlineCastGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/inline/PlayerGlyphs.kt |
| 6 | 20 | InlineMoreGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/inline/PlayerGlyphs.kt |
| 7 | 1 | DanmakuSettingsGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/inline/PlayerGlyphs.kt |
| 7 | 2 | BackgroundPlayGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/inline/PlayerGlyphs.kt |
| 7 | 3 | AutoPlayNextGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 4 | LoopGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 5 | MirrorGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 6 | BackgroundListenGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 7 | ResizeGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 8 | ColorAssistGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 9 | SpeedGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 10 | TimerGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 11 | SubtitleGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 12 | DanmakuSettingGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 13 | DislikeGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 14 | WatchLaterGlyph | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 15 | CacheGlyphSmall | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 16 | PipGlyphSmall | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 17 | CastGlyphSmall | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/fullscreen/PlayerSettingGlyphs.kt |
| 7 | 18 | LockGlyph.true | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/player/VideoPlayerStatusGlyphs.kt |
| 7 | 19 | LockGlyph.false | 自绘 | feature/video/src/main/kotlin/com/aritxonly/bdfun/feature/video/presentation/player/VideoPlayerStatusGlyphs.kt |
| 7 | 20 | Menu.Reply | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/common/glyph/MenuGlyph.kt |
| 8 | 1 | Menu.Copy | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/common/glyph/MenuGlyph.kt |
| 8 | 2 | Menu.Report | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/common/glyph/MenuGlyph.kt |
| 8 | 3 | Menu.Block | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/common/glyph/MenuGlyph.kt |
| 8 | 4 | Menu.Delete | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/common/glyph/MenuGlyph.kt |
| 8 | 5 | Menu.Coin | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/common/glyph/MenuGlyph.kt |
| 8 | 6 | Menu.Star | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/common/glyph/MenuGlyph.kt |
| 8 | 7 | Danmaku.Fixed | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/danmaku/DanmakuGlyphs.kt |
| 8 | 8 | Danmaku.Scroll | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/danmaku/DanmakuGlyphs.kt |
| 8 | 9 | Danmaku.Color | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/danmaku/DanmakuGlyphs.kt |
| 8 | 10 | Danmaku.Advanced | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/danmaku/DanmakuGlyphs.kt |
| 8 | 11 | Danmaku.Count | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/settings/danmaku/DanmakuGlyphs.kt |
| 8 | 12 | MarkRead | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/message/MessageGlyphs.kt |
| 8 | 13 | ThreeDots | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/message/MessageGlyphs.kt |
| 8 | 14 | Comment | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/feed/FeedCommentGlyph.kt |
| 8 | 15 | Pause | 自绘 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/live/LiveChromeControls.kt |
| 8 | 16 | 字符 › | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/onboarding/NoticePage.kt |
| 8 | 17 | 字符 ⌄ | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/feed/FeedViewerAuthor.kt |
| 8 | 18 | 字符 ✓ | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/video/card/GridCoverSelection.kt |
| 8 | 19 | 字符 ✓ | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/message/MessageSettingsContent.kt |
| 8 | 20 | 字符 ✕ | 字符 | app/src/main/kotlin/com/aritxonly/bdfun/presentation/feed/FeedViewerTopBar.kt |
