# Message center and home recommendation actions

Reference: Hyper-PiliPlus commit `0d08dd25bdeaa9a55e5a0e333154b681fd8048fe`.
The existing bdFun / Deadliner glass recipes, navigation chrome and message card appearance remain in use.

## Implemented behavior

- Home video cards open a long-press sheet with watch later, current-video feedback, creator feedback and the actual video category. Web feedback preserves the recommendation tracking ID. App feedback uses the server-provided `three_point_v2` dislike reason IDs and the existing signed access key.
- Only successful server feedback removes a video/creator from the visible feed. Category feedback uses the App reason when provided. When the recommendation source supplies no category reason, the sheet explicitly describes a **current-browsing-session local filter**, rather than claiming a server preference was updated. Subsequent pages resolve missing category metadata only while that filter is active, with bounded concurrency. Exclusions are not a durable blacklist.
- Message center keeps its shortcuts/cards and adds system notifications. Sessions and feeds use real lazy rows and server cursors. Filtering followed users no longer silently substitutes all users. Refresh failures retain content, retries retain the failed request mode, and an empty filtered page can still advance.
- Session long press supports pin/unpin, mute/unmute, read acknowledgement and removal. Mutations update local state after service success. A full read operation enumerates sessions, acknowledges each, opens notification feeds, then checks server unread counts before reporting success.
- Reply, mention and like feeds use `id` plus their corresponding time cursor; likes retain the latest/total buckets. Notification deletion uses the service's type enum, independent of the screen tab index. Like notifications can be muted per item.
- System notices use `message.bilibili.com/x/sys-msg/query_notify_list`, cursor acknowledgement and deletion. HTML content is rendered as text with HTTP(S) links, without a WebView. Dynamic notifications follow their supplied dynamic/opus link; video notifications retain comment/seek targeting.
- Private chats carry acknowledgement positions, page backwards by message sequence, refresh new messages only while resumed and preserve drafts on failure. Images are sent as image messages, not textual URLs. If an image sends but its caption fails, retry keeps only the unsent text. Withdrawn records render a withdrawal placeholder.
- Dynamic navigation uses MIUIX Album. Cache, history and watch-later actions use Download, Recent and SearchDevice; player watch-later/cache shortcuts use the same icons.

## MSG-01 · 消息行为对齐（2026-11-13）

目标是对齐固定参考版本的消息行为，页面保留 bdFun / Teak 风格。主工作区以 origin/main 为基线，不包含独立的视频列表 PR #39。

| 入口 | 接入的行为 |
| --- | --- |
| 消息中心 | gRPC SessionMain、复合 offsets 分页、服务端 outside/three-dot 菜单、回复/@/点赞/系统入口 |
| 次级收件箱 | SessionSecondary，按 fold 类型进入未关注人、陌生人、消息垃圾箱、群聊、客服或 AI 收件箱 |
| 会话操作 | 服务端身份快照置顶/取消置顶、免打扰、已读确认、单条删除、确认清空当前列表 |
| 回复与 @ | 分别使用 reply/at 游标，正文、目标/根评论引用、头像空间和原内容跳转、通知删除、独立设置类型17/18 |
| 收到的赞 | 最新/累计分桶、多头像、摘要和缩略图、聚合人员详情（pn/last_mid）、原内容跳转、通知开关及确认说明、设置类型19 |
| 消息设置 | Get/SetImSettings 的 switch/select/redirect/window-select、子设置页、黑名单和消息屏蔽词 |
| 消息屏蔽词 | 服务端列表/字符上限、增删、错误保留草稿、成功后刷新 |
| 通讯录 | 关注/粉丝分页、关注搜索、个人空间入口；黑名单支持移除 |
| 单聊设置 | 用户资料、置顶、免打扰、内容推送、拉黑/解除拉黑；关闭推送和拉黑需确认 |
| 私信 | gRPC 发送/历史消息、序列分页与已读、文本/图片/自定义表情、分享/通知卡片、撤回、举报；图片使用 biz=im 的 upload_bfs |

本轮协议字段表来自同一固定上游版本的 pbjson 描述符，生成工具为 `tools/generate-im-schema.py`，输入是参考仓库根目录。运行时使用现有 OkHttp，无新增依赖。描述符及行为参考来自 GPL-3.0 的 [Hyper-PiliPlus](https://github.com/AritxOnly/Hyper-PiliPlus/tree/0d08dd25bdeaa9a55e5a0e333154b681fd8048fe)，本仓库同为 GPL-3.0。协议解析只存在 infra；core/domain 与 application 不解析 JSON。

账号凭证缺少 access_key 时明确要求重新扫码登录，不悄悄回退到行为不同的 HTTP 收件箱。旧 HTTP 门面仅供兼容调用。服务端游标、会话身份与设置载荷为不透明快照，不写入全局 pending 字段。账号切换/页面关闭丢弃迟到响应，取消必须释放 busy/loading。

### MSG-01：私信 -663 鉴权修正

实机截图返回「鉴权失败，请联系账号组 (-663)」。核对发现扫码签发使用 TV appkey `4409e2ce8ffd12b8`，IM gRPC 却声明 `android_hd`。固定参考版本的 `lib/http/login.dart`、`lib/utils/app_sign.dart`、`lib/common/constants.dart` 与 `lib/utils/accounts/grpc_headers.dart` 则统一使用 HD 客户端与 appkey `dfca71928277209b`。现将二维码生成/轮询改为参考版本的签名及参数，登录 User-Agent 和 app-key 也统一；通过 `HdClientIdentity` 与 IM 共用身份常量，避免再次分叉。

旧 TV access_key 无法靠修改请求头变成新签发的 HD 凭据，安装修复版本后需退出账号并重新扫码登录。结构化 -663 错误保留服务端原文并追加该操作提示，不自动退出、不清空 Cookie、不静默转换凭据。JVM 测试覆盖扫码/轮询身份、签名、禁止带旧 access_key 和负错误码提示；登录后的真实历史消息读取仍需实机复测，离线通过不等于已确认服务端恢复。

固定参考版本本身没有实现的特殊会话/菜单类型不宣称可用。完整的登录账号行为一致性仍需实机验收；离线测试不能证明服务端功能全部可用。

## Source pointers

- Hyper-PiliPlus `lib/http/msg.dart`, `lib/pages/msg_feed_top/*/controller.dart`, `lib/pages/common/common_whisper_controller.dart`, `lib/http/video.dart`, `lib/models/home/rcmd/result.dart`.
- [Web recommendation feedback implementation](https://github.com/magicdawn/bilibili-app-recommend/blob/main/src/components/ModalDislike/api/index.ts) and [web reason mapping](https://github.com/magicdawn/bilibili-app-recommend/blob/main/src/components/ModalDislike/api/pc.md).
- [Session and private-message contract](https://github.com/bilibili-plugins/bilibili-api-collect/blob/master/docs/message/private_msg.md), particularly the inverted pin `op_type` (0 pin / 1 unpin) and sequence pagination.

## Validation

Unit checks cover the reply/mention/like cursor fields, server end/stuck cursors, the delete/pin enums, failed/partial/cancelled acknowledgements, duplicate acknowledgements and video/creator/category exclusion boundaries. Debug and optimized Release builds are checked separately.

Per the user's instructions, no emulator was used and no package was sent to Feishu. Authenticated service behavior and visual/touch behavior remain for physical-device verification: long-press dismissal and failure retries, following-only pagination, notification deletion and read counts, chat image/caption partial failures and history scrolling.

## 实机回归清单

- 消息中心：刷新、连续翻页、次级收件箱、服务端菜单、未读数、置顶/取消、免打扰、单条删除、确认清空。
- 回复/@：分别进入而不误读另一个 feed，引用完整，头像和原内容正确跳转，删除及设置生效。
- 点赞：最新/累计，多人通知进入完整人员列表，连续翻页、个人空间、返回原内容、关闭/恢复通知。
- 设置：开关/单选/子设置页往返，保存失败可重试，屏蔽词上限/新增/删除，黑名单解除。
- 私信：文本、图片、表情、分享/通知卡片，历史分页，返回再进入，自己的消息撤回、对方消息举报（仅用测试账号），推送/拉黑确认与设置失败恢复。
- 暗色/浅色、大字体、横屏；退出登录/重新登录不串账号，弱网刷新/翻页/提交不重复请求或丢草稿。

模拟器只有游客数据，已安装 Debug 并打开消息入口，未执行真实发送、举报、撤回、拉黑或清空；登录后的真实接口与触摸回归交由实机完成。当前所有新增协议样本均为依据上游描述符编写的离线结构样本，不冒充线上采样响应。

## 同机跳转哔哩哔哩登录

对照同一参考版本 `lib/pages/login/view.dart` 的「其他应用打开」：登录页提供「跳转哔哩哔哩登录」，使用 `bilibili://browser?url=` 和完整编码的当前二维码 URL，由官方 App 打开授权页；返回后沿用原二维码 key 轮询，不新增回调 scheme、不更换登录方式、不写入新的全局传参。加载、过期、失败或页面关闭后不可发起跳转，点击执行时重新读取当前链接。未安装/无法打开时提示继续使用二维码登录，不输出带凭证的 URL。实机需验证已安装官方 App 的打开、确认与返回登录，以及二维码刷新后使用新链接。

## 私信进入后退出：初始化顺序回归

`PrivateChatEffects` 读取 `LocalRichTextDependencies`，但此前在 `TeakActivityScaffold` 之前组合；独立 Activity 不继承消息页的 CompositionLocal，默认值会抛出「富文本依赖尚未装配」。已将 effect 绑定移入页面壳的 content，在 `TeakBareTheme` 注入依赖后组合，保留原控制器和 effect key。

新增框架 Instrumentation `PrivateChatLaunchRegression`，通过真实 `Route.PrivateChat` 进入 Activity，并等待首屏组合后确认页面保持打开。只允许游客数据，避免真实账号的已读回执；不新增测试运行库，测试入口仅存在独立 androidTest APK。对照同一测试：修复前 Debug APK 报 `Process crashed`，AndroidRuntime 栈指向 `PrivateChatEffects` 的「富文本依赖尚未装配」；修复后 `OK (1 test)`，Fatal 崩溃数 0。

复现/回归命令（使用只读测试模拟器）：

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.aritxonly.bdfun.debug.test/com.aritxonly.bdfun.PrivateChatLaunchRegression
```

该测试覆盖页面初始化与依赖装配，不代表登录账号的实际收发/撤回/举报已经通过。

### MSG-01：重新登录后 SessionUpdate 仍返回 1217

用户已确认安装鉴权修复版并重新扫码后，聊天设置仍在「置顶设置」阶段返回 1217，不能再归因于旧 TV 凭据。对照固定上游 `grpc_headers.dart`，补齐此前遗漏的 `bili-http-engine: cronet`、locale、trace 与空环境头；请求体仍保持与上游一致，不猜测添加 page_type。Locale/LocaleIds 描述符由同一固定版本重新生成，其余既有协议描述符不变。新增独立 wire 样本验证 `SessionUpdateReq.session_id.private_id.talker_uid` 编码，以及语言区域头测试。缺失头是已确认的实现差异，尚无真实账号响应证明它就是 1217 根因或已恢复，需实机复测。

### MSG-01：gRPC 传输与网页 Cookie 隔离

用户再次确认聊天设置 1217 未恢复，且 HyperPiliPlus 同入口正常。进一步比对固定上游 `lib/utils/accounts/account_manager/account_mgr.dart`：gRPC 分支添加 grpcHeaders 后立即返回，不走 Web Cookie 装载；bdFun 此前复用带 BiliCookieJar 的客户端，实际会附加网页 Cookie。现派生共享连接池/调度器的无 Cookie gRPC 客户端，既不发送 Web Cookie，也不将 gRPC 响应 Cookie 写回 Web 登录。补齐参考压缩声明；未压缩消息继续使用标准 frame flag=0。该差异修正不能代替真实账号的成功响应，1217 根因仍待验证。
