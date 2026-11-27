# Hyper-PiliPlus 动态流对齐

参考 [AritxOnly/Hyper-PiliPlus](https://github.com/AritxOnly/Hyper-PiliPlus/tree/0d08dd25bdeaa9a55e5a0e333154b681fd8048fe)，核对日期 2026-11-11。按其服务端接口契约和动态页行为实现 Compose 版本，保留 bdFun / Deadliner 的主题、玻璃材质、刷新和自适应外壳。

## 数据与交互

- `lib/http/dynamics.dart`：分类与单 UP 统一使用 `/x/polymer/web-dynamic/v1/feed/all`。分类传 `type`，UP 传 `host_mid`，两者互斥；分页传服务端 `offset`，不再固定 `page=1`，不再用空间接口替代 UP 关注流。
- `lib/common/constants.dart`：动态 feed 的 `features` 与参考版本一致。
- `lib/pages/dynamics/controller.dart`、`lib/models/dynamics/up.dart`：首次加载/刷新请求 `/portal?up_list_more=1&web_location=333.1365`，UP 列表分页请求 `/uplist`，传 `offset/platform/web_location`。保留接口排序、更新标记和直播列表，不再从已加载动态中推断作者或按本地已读记录重排。
- `lib/models/common/dynamic/dynamics_type.dart`：全部、投稿、番剧、专栏、UP。分类/UP 分别缓存列表和游标；选择头像进入对应 UP，再点当前头像刷新，刷新作用于当前分类/UP。“我”入口使用当前账号。
- `lib/pages/dynamics/widgets/live_rcmd_panel.dart`、`lib/models/dynamics/result.dart`：渲染 `major.live_rcmd` 和 `subscription_new.live_rcmd` 的封面、标题、直播状态、分区与观看提示，保留服务端 `module_tag.text`。bdFun 尚无内置直播播放器，点击打开对应 B 站直播间链接。
- 不自行产生、替换或重排推荐内容；显示服务端在当前账号、分类和游标下返回的动态流。

## 状态与界面

- 删除本地每次截断 15 条的队列，按服务端分页展示和去重；重复/空游标终止分页，空内容页最多连续补取 5 页，仍可手动加载更多。
- 请求代次保护防止旧分类、旧 UP 或旧分页覆盖新结果；取消异常向上传递；刷新失败保留原内容，重试保留原请求类型；切账号重建动态状态。
- 动态在 736dp 起使用两列卡片，窄屏单列；横向 UP 头像列表和独立的可折叠“正在直播”区域置于内容顶部。更新提示为头像右上角圆点，名称居中，点击已读不改变行高。保留已有 Deadliner 玻璃分类栏和 APP 材质参数。
- 消息页“回复与@”使用 MIUIX `MiuixIcons.Messages`（与主页消息入口一致）；“新增粉丝”使用 `MiuixIcons.Contacts`。

## 验证

上一版使用 Android 35 模拟器补充验证了消息图标、分类切换、UP 筛选、分页和宽窄屏切换，动态数据来自临时可控 fixture，不代表真实账号个性化推荐验证。随后用户要求停止模拟器测试，后续以真机 APK 验证为主；临时 Debug 入口已移除。

本轮补充：窗口按 edge-to-edge 绘制并允许覆盖所有刘海边缘，系统导航栏不额外添加对比度底色；播放器详情背景覆盖安全区，宽屏顶栏不再把系统侧边 inset 当成整个栏的留白。消息和设置将状态栏留白放进滚动内容；空间页改用原生 LazyColumn 滚动，736dp 起左侧 300–360dp 资料列、右侧内容列，窄屏头图与内容在同一个列表内自然滚动。恢复主页头像，消息入口显示服务端未读数量，0 隐藏、超过 99 显示 99+，页面回到前台时刷新。

构建检查覆盖 Debug、Release 和 25 项回归用例（包括新增的单双列尺寸边界）；本轮实机的 OEM 窗口边缘表现交由用户验收。


## 2026-11-11 真机布局反馈

界面入口收敛为“全部 / 视频 / 番剧”；article/up 协议支持保留，UP 通过作者导航进入。
宽屏采用窄作者侧栏与 280–560dp 自适应瀑布流，窄屏采用横向作者导航与单列流。
切换布局只改变展示，复用同一动态状态、服务端顺序、分页游标和点赞状态。

## 2026-11-27 卡片菜单与举报接口

动态卡片省略号 / 长按的条目与首页视频卡片不同，但外壳、形变动画与危险色完全复用：`TeakMorphMenuHost` + 每卡一个 `TeakMorphMenuState`，长按走页面级点锚点（`TeakMorphPointAnchor`，不画图标副本），宿主挂在主壳玻璃采样树外的 `MainFeedChrome`。条目为：添加至稍后再看、复制、关闭自动播放、分享、取消关注（红）、举报（红）。「关闭自动播放」写既有 `settings.dynamicAutoPlay=false`；稍后再看、取消关注分别复用 `VideoActionsRepository.watchLater/follow`；复制优先正文、其次标题、最后网页链接；分享链接为 `https://t.bilibili.com/{id}`。

举报接口没有照 bdFun 旧代码猜：端点与字段对齐 PiliPlus 固定参考实现 `lib/http/user.dart` 的 `UserHttp.dynamicReport`（`Api.dynamicReport = /x/dynamic/feed/dynamic_report/add`），form 提交 `accused_uid`、`dynamic_id`、`reason_type`、`reason_desc`，`csrf` 放查询串。理由编号取自 `ReportOptions.dynamicReport`，落在 `core:domain/DynamicReportReason`；其中「其他」(`reason_type=0`) 必须带文字说明，本轮不提供，避免提交服务端必拒的空说明。端口在 `FeedPageRepository.reportDynamic`，实现在 `infra:bili`，UI 侧先选理由再提交，成功/失败都走页面 toast。

举报需要登录账号，真实提交结果无法用游客/离线样本代替；PR 需附手动验证：登录 → 打开某条动态的省略号/长按 → 选理由提交 → 观察服务端返回与 toast。

## 2026-11-27 UP 切页推动与头像环出屏方向

UP 之间切换改成和空间页一样的整页推动：`FeedUpPageTransition` 在切换瞬间用 `rememberGraphicsLayer` 录一帧当前页当旧页，旧页滑出、新页从另一侧滑入，拖动时页面跟手（阻尼 + 34% 上限），松手没过阈值回弹。方向按 UP 在头像行里的先后；进入/退出 UP 模式只淡入。所有入口（头像点击、左右滑、返回、全部动态）统一走 `rememberFeedUpSelection`，上一次推动没播完时直接换页、不吞操作。

头像环的方向记忆修了两个根因：①锚点是 root 坐标，判断左右必须换成卡片内坐标再和覆盖层宽度比较，宽屏有作者侧栏时差一整个侧栏；②头像也会因为整张头像卡纵向滚出列表而被回收，最后一次测量是完整可见的，不能按水平位置猜左右——只认真正探出视口的那一侧，探不出时用头像行此刻的滚动方向兜底，拿不到就保持未知。
