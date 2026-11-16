# Release 发布与自动更新

Actions → Android 手动发布 → 选择待发布分支 → 填写 `version`。
不必修改源码版本或预建 tag；流程构建所选提交，成功后创建 `v版本号` 标签和 Release。
已经存在的标签拒绝覆盖，请递增版本重新发布。

版本规则：`preview-y.z` 或 `x.y.z`；y/z 为 0..99，x 为 0..999，禁止前导零。
versionCode = x×10000+y×100+z，preview 的 x=0。例如 preview-5.0=500，1.1.0=10100。
本地构建可用 `bash ./gradlew :app:assembleRelease -PreleaseVersion=1.1.0`。
BUILD_STAMP 仍记录构建时刻，debug 仍增加 -debug 后缀。

`notes` 支持 Markdown 列表并优先使用。留空时必须启用 `generate_notes`，由 DeepSeek
Agent 读取上一标签至 HEAD 的提交，生成列表并自审一轮。首版使用全部提交。
生成失败阻止发布，可填写手动日志重新运行。发布后应检查 Release 日志准确性。

Secrets：KEYSTORE_BASE64、KEYSTORE_PASSWORD、KEY_ALIAS、KEY_PASSWORD；
自动日志另需 DEEPSEEK_API_KEY，可用变量 DEEPSEEK_MODEL 覆盖默认 deepseek-chat。
API Key 仅供 Actions 使用，不编译进 APK。四项签名配置缺一即停止，禁止 CI 退回 debug 签名。
工具链沿用 JDK21 / SDK37 / Build Tools37.0.0 与固定 Miuix 快照。
构建运行 domain/infra-bili/app JVM 测试及 R8 release，校验签名并附 APK、SHA256SUMS 与 mapping。
preview 自动标记 prerelease，正式版本发布为 stable。推送和 PR 不自动发布。

客户端：设置 → 关于 → 点击顶部图标/名称/版本身份区，进入页面自动检查。release 冷启动每日检查一次，debug 不自动弹更新。
preview 安装检查 preview 与正式版；正式安装只检查正式版；按 versionCode 比较，
更新比较忽略无效标签、draft 与 debug APK（旧标签仍在历史列表展示）；多 APK 时使用发布页面避免选错架构。
无更新时显示当前版本日志，底部并列 primary / 中性柔光玻璃按钮及渐进遮罩；更新日志页展示各版本日志。
有更新时底部为单条 primary 下载按钮，历史入口位于品牌头部之后。
更新页内嵌设置导航，横屏仅占右侧详情栏；独立 Activity 供启动更新提示使用。首次下载申请通知权限，
系统 DownloadManager 在通知栏显示后台下载进度，进程退出仍继续；完成后点击系统通知安装。
每个版本保留发布页入口作为浏览器兜底。
系统首次安装授权及 debug/release 不同包名遵循 Android 自身行为。

手动验证：最新/可更新/网络失败/无附件；长日志滚动；下载失败重试；退后台完成下载；
点击通知安装并核对签名覆盖升级；preview 到 stable；横竖屏与深色模式。

GitHub mapper 固定样本取自 2026-11-16 的公开 releases API 返回，包含实际 preview-5.0 当前日志、debug 附件过滤与旧标签。

接口依据：[GitHub Releases REST](https://docs.github.com/en/rest/releases/releases)、
[DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)。

关于页外部的更新日志索引入口已移除，统一从检查更新页右下角“更新日志”进入。检查按钮在禁用态使用当前主题 primary 的淡色，不使用库默认蓝色。

关于页版本、快照、编译时间归入同一“版本信息”设置组，讨论等动作另组。更新日志采用原ChangelogScreen列表布局，自动加载仓储发布数据，展示加载、空与失败重试状态。

快照内容与编译时间均由 Gradle 配置阶段自动生成，快照显示 Git commit，存在本地改动时带 -dirty 后缀。
