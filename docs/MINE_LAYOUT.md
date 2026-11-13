# MY-01 我的页布局与预览大卡

采用个人空间头图与叠层头像；不足600dp时头图从页面/状态栏顶部通栏铺开，不套卡片、不留外边距。资料卡左右内缩14dp，上压头图30dp，76dp头像再上压24dp。600dp起按原型使用左45%名片/右55%功能双栏。原四个快捷入口保留，补齐常用功能区标题。窄屏设置入口位于采样树外的TopAppBar右上角，复用AdaptiveMiuixTopAppBar/TeakGlassIconButton；宽屏入口为个人资料卡下方的Miuix Card+ArrowPreference，顶栏仅保留滚动渐进模糊、不显示设置按钮。头图绘制/解码/Referer复用空间页现有组件与工具。

设置 → 我的页新增「展开历史记录」「展开我的收藏」「展开稍后再看」，默认关闭，立即生效且持久保存。页面内不提供展开/收起操作。该设备各账号共用布局偏好，内容请求按账号隔离。

历史/待看各预览两条视频，收藏预览两个收藏夹；点击收藏夹进入现有收藏页，查看全部进入对应完整页面。历史正数进度按原HistoryItem秒单位续播，已完成的负值不传续播位置。加载/空/错误重试均有展示；重新进入前台刷新，关闭或切号拒收旧响应。

分层：MineLayoutPreferences为领域存储端口，AndroidMineLayoutPreferences保存独立mine_layout文件，core/application/MineLayoutStateHolder提供共享不可变StateFlow。MinePreviewStateHolder复用LibraryRepository，不在UI请求API。头图从已对齐的空间响应读取，失败使用原资料/默认头图。

验证：新增布局偏好重建恢复、按需加载、失败重试、两项截断、账号替换、关闭配置后的迟到响应、取消解锁、续播位置等JVM测试。运行app全部单测、core/application单测、assembleDebug及checkLayering。

登录账号手动验收待办：个人头图/头像/等级/会员与统计；三个开关分别启停以及重启恢复；真实历史续播；收藏完整页；待看播放；空数据与断网重试；横竖屏及大字体；登录/退出/换号后不残留旧内容。

## 原生崩溃修复

2026-11-13设备日志在RenderThread报SIGSEGV，512层堆栈重复RenderNode::prepareTreeImpl与SkiaDisplayList::prepareListAndChildren，并标明stack overflow。设置按钮及原空间玻璃胶囊位于它们读取的主壳layerBackdrop树内，构成绘制依赖环。

设置顶栏改由MainScaffold的MainMineChrome在采样树外绘制，保留原柔光配方；空间胶囊改为现有Miuix普通Button。body不再调用MineTopAppBar、不使用主壳玻璃采样。验收要检查进入我的页、横竖屏、反复切换tab、打开设置及返回；普通JVM测试不能证明厂商原生渲染路径通过。

## 沉浸与渐进模糊

窄屏头图绘制到状态栏背后，在我的页顶栏存在时关闭系统状态栏对比色蒙层，离开我的页后恢复原值。主壳持有并保存列表滚动位置，复用TrackTeakPageScroll将实际位置投影为TeakPageScrollState.materialProgress；仅在采样树之外用ProgressiveTopBarMaterial包裹TopAppBar，初始透明，滚动后渐进模糊。

宽屏顶栏保留滚动渐进模糊但取消全页右上设置按钮，45%左栏在个人资料卡下方放置原生Miuix Card+ArrowPreference，进入现有设置二级页；普通偏好行不依赖背景采样，保留55%右栏。宽屏游客同样保留设置卡。

## 视口与表面色修正

我的页宽屏列表移除外层statusBarsPadding，将等量状态栏高度加到contentPadding中；媒体库共用pager同样铺满窗口，收藏/最近播放/稍后再看的网格将等量避让加入contentPadding。首屏位置保持，滚动内容可以进入顶部安全区和渐进模糊采样区。

宽屏设置卡去除summary，继续使用Miuix ArrowPreference，Card显式选择surface/onSurface以匹配资料卡。平板底栏取消navigationBarsPadding，侧边与底部固定20dp，同一个常量提供给主壳遮挡高度；手机底栏保留原Insets行为。

## 刷新稳定性与缩略图

宽屏头图保留上角圆角、底角直角，与资料容器衔接。刷新保留上次成功内容，首次请求使用与最终内容等高的骨架；失败重试、空状态及取消都不收缩区块。关闭开关和账号失效仍移除旧内容，并保留代数守卫。

每个视频最大180dp宽、16:9封面，标题预留两行、元信息一行。内容槽高度随窗口和字体缩放计算，网络请求/返回条数不会改变容器高度。新增JVM测试覆盖刷新期间、失败、取消及账号替换的旧数据行为。

## 五条最近视频与四个收藏夹

最近播放与稍后再看各展示按时间倒序的最近5条，沿用180dp最大宽度的紧凑封面并使用LazyRow横向滚动，区块高度不随条数或请求状态改变。收藏保持4行纵向布局，备注「共 n 视频」。封面通过已按mtime排序的收藏夹第一页第一条视频取得；空夹显示占位，单个封面请求失败保留旧封面，初次无缓存则留空。标题、数量与顺序保留收藏夹列表的原值，四个封面并行补全并受账号/页面/代数与取消保护。

预览上限由旧需求的2改为视频5/收藏4，原上限测试据新需求调整；新增JVM测试覆盖时间排序、四夹上限、首视频封面/数量、单封面失败与子请求取消。
