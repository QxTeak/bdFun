# 消息中心自适应改版

按 `output/design/message-prototype/index.html` 确认的交互实现，消息中心继续是首页入口打开的二级页面。

## 组件与页面结构

- 消息首页展示「回复与@」「收到喜欢」「系统通知」三个入口；回复与@角标合并两类未读数，子页保留分类切换。系统通知使用 `MiuixIcons.Promotions`。
- 页面沿用 `TeakActivityScaffold`、`TeakLargeTitle`、`TeakTopGlassButton`、`TeakPagerTabs` 和 `TeakActionSheet`，不新增主导航入口。
- 可用宽度小于 840dp 时使用单栏，390dp 竖屏和 480dp 阔折叠外屏均逐级进入子页。840dp 起使用 340dp 消息列表和剩余宽度的详情区域。
- 私信、回复与@、喜欢、系统通知、联系人及设置在同一宿主内打开；外部独立 Activity 入口继续可用。视频、动态、个人空间仍走已有显式路由。
- `MessageCenterStateHolder` 持有单一导航栈，宿主用现有路由编码保存恢复。切换尺寸不重建页面实例；被覆盖页面保留列表和草稿，生命周期降到 STARTED 以停止私信轮询，返回后恢复。
- 联系人行可进入私信；黑名单条目保留原空间入口和移除动作。关注搜索、服务端菜单、置顶、免打扰和删除确认均保留。

## 消息封面

回复、@、点赞共用 `MessageCardHeader`，封面在卡片头部右上角，正文和原评论引用在其下方使用剩余完整宽度。

`MessagePresentation.originalVideoCover` 与原有 `subjectCover` 分开，避免将评论/动态附图误作视频封面。补查复用已有视频详情接口和进程缓存，按视频去重，最多四路并发；只使用明确的 BV、视频业务类型或原生视频链接，不能把动态 subject_id 当作 aid。封面补查失败仍展示文字，不阻断整页。不存在视频来源时可展示接口原内容图片，不伪造视频封面。

## 验证

已通过：新增消息导航/筛选/未读数 6 项和封面来源 4 项 JVM 测试，以及 app 单测、infra:bili 单测、弹幕测试、Debug 构建、分层/模块依赖及 Kotlin PSI 尺寸检查（0 违规）。

模拟器游客环境已检查：普通竖屏（约411dp）、480×780dp 阔折叠外屏、1024×768dp 宽屏；进入回复子页仍是同一个 MessageActivity，展开后显示列表与详情双栏，系统返回可回到消息中心。深色截图保存在 `output/design/message-prototype/implemented-*.png`。浅色、登录后的真实内容和私信收发仍需按下列清单验收。

人工回归清单：

- 390dp、480dp、1024dp 下进入消息中心，检查返回、顶栏和无主底栏。
- 进入回复、@、喜欢和系统通知；切换已有分类、下拉刷新、分页失败重试。
- 登录后核对真实原视频封面和评论跳转；确认动态等非视频来源不会错配封面。
- 私信输入草稿 → 聊天设置 → 返回 → 展开/收起或旋转 → 确认草稿和列表位置。
- 图片选择、表情、发送失败重试、未读回执、置顶、免打扰和删除确认。
- 联系人关注搜索、粉丝、黑名单及消息设置子项。
- 浅色/深色、系统返回、输入法弹出与收起。

游客页面检查不代表登录后的真实私信收发及封面数据验收。

## 分段、返回与读取错误修复

- 分段沿用原 `TeakPagerTabs` 的 plain 胶囊和 96dp 单项宽度。此前传入的 `position` 始终是 0，覆盖了 selected 对指示器和文字的控制；移除该固定位置，直接由选中项驱动组件自己的动画。
- 互动标题设置主题基础文字色，避免聚合通知中没有独立 SpanStyle 的「等 x 人」回退到固定黑色。
- 嵌入宿主的 MessageScreen 不注册独立 BackHandler，由消息中心统一弹出导航栈；独立 Activity 入口保留原返回行为，隐藏首页不能处理子页的系统返回。
- 聊天设置读取补齐固定上游的 build/mobi_app/csrf/csrf_token；gRPC Device 补齐同一上游的 brand/model。私信初次加载保留错误原因，设置标识失败阶段；gRPC 优先解析 Status/Any 错误详情并解码百分号编码，不将所有错误压成一个数字。
- 1217 的实际服务端含义不能从数字推断。这些请求修正及错误解析有离线测试，真实登录账号是否恢复仍需实机确认。
- 游客设备回归使用 `-Ppkg=com.aritxonly.bdfun.msgregression` 隔离构建；入口：`adb shell am instrument -w -e suite message-navigation com.aritxonly.bdfun.msgregression.test/com.aritxonly.bdfun.PrivateChatLaunchRegression`。系统返回、顶栏返回均通过，宿主保持打开；回复/@截图已检查指示器分别位于对应选项，保存在 `output/design/message-prototype/implemented-*-selected.png`。真实账号的1217是否恢复仍未验证。
