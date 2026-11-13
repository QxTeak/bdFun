# PR CI 与签名测试包（P5-04 后续）

`.github/workflows/architecture.yml` 在每次 PR 创建、更新及重新打开时运行，也支持手动运行；main/codex 分支推送保留原检查。

检查包含架构依赖规则、Android Lint (`:app:lintDebug`)、Debug/Release 编译、反向依赖测试、core 策略与视频回归测试。这里的 linter 是 Android Lint，不新增 ktlint/detekt。报告、R8 mapping 与 Release 候选包作为 Actions artifacts 保留。同一 PR 的新提交会取消旧运行。

## GitHub 设置

1. 在实际接收 PR 的仓库（如 QxTeak/bdFun）启用 Settings → Actions → General；允许本工作流引用的 actions 与 gradle/actions。
2. Settings → Environments → New environment，新建 **pr-signing**。在 Environment secrets 添加：

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | bdfun-release.jks 文件的 Base64 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 签名密钥别名（本项目为 `bdfun`） |
| `KEY_PASSWORD` | 密钥密码 |

macOS 可用 `base64 -i /绝对路径/test-release.jks | pbcopy` 复制 Base64，不要提交密钥或把密码打印到日志。建议使用固定的测试专用签名；如果要覆盖正式安装，必须使用与正式包相同的证书。当前包名仍为 `com.aritxonly.bdfun`，versionCode 沿用项目配置，安装时注意覆盖及数据影响。

3. Environment 的部署分支规则需要允许 PR merge refs (`refs/pull/*/merge`)；最简单是 No restriction。可加 Required reviewers，由维护者审核后放行签名；希望同仓库 PR 全自动则不设置 reviewer。任何能修改同仓库工作流的人都应视为可信，独立签名 job 不能代替仓库写权限控制。
4. 把工作流提交并推送到 PR 后，从 PR → Checks → Android CI → Details 打开运行，下载 `signed-release-pr-编号-SHA`，解压取得 APK 与 SHA256SUMS。也可以点签名任务 Summary 的下载链接。需要登录 GitHub；保留 14 天。这是 PR 检查附件，不会创建 GitHub Release，也不会向 PR 发评论。
5. Settings → Rules → Rulesets（或 Branch protection）为 main 添加必过的 `check` 检查；先成功运行一次再从列表选择。不要把 `signed-release` 设成所有 PR 必过，因为 fork PR 会跳过它。

同仓库 PR 检查成功后，独立的 `pr-signing` job 下载该次运行的 APK，用 apksigner 替换候选包的临时签名并验证，再上传固定密钥签名的包。签名任务不检出 PR、不执行其 Gradle/脚本；密钥只在签名步骤注入，结束后清理。缺少 secrets 会明确失败，不会将临时签名包冒充固定签名测试包。

fork 和 Dependabot PR 仍运行 CI、生成 Release 候选包，但跳过固定密钥签名。GitHub 默认不会把 secrets 传给 fork 的 pull_request 工作流。若需签名，维护者审核后将提交放到仓库内分支并发起 PR。不要改用 pull_request_target 执行外部 PR 代码来获取 secrets。

## PR #1 合并适配

PR #1 的手动构建和标签预发布流程已合入，并补上固定 Miuix 准备步骤。冲突中的旧 UI/业务实现保持 main 版本，避免恢复已迁移代码。手动发布沿用标签/版本校验，签名使用与 PR 一致的 Secret 名称和 V2；本地 keystore.properties 兼容保留。
手动发布需在仓库 Settings → Secrets and variables → Actions 中配置同名四个 Secrets；仅配置 pr-signing Environment 不会自动提供给手动发布任务。
手动工作流进入默认分支后可用 Run workflow；发布标签也必须包含适配后的构建配置。

GitHub 依据：
- [fork 的工作流与 secrets 限制](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows)
- [Environment 审核与分支规则](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)

## 本次签名参数

使用 `bdfun-release.jks`、alias `bdfun`、包名 `com.aritxonly.bdfun`。签名任务仅启用 APK Signature Scheme V2，并关闭 V1/V3/V4。
预期证书 SHA-256：`a34f7803b15c3ebba2e6c4c7bc9646ae14bffb15abad4fe58aaf2ca46566e5db`。
密码只保存到 GitHub Secrets，不写入文档。

如果使用 GitHub CLI，可在本机终端执行以下命令（密码通过交互提示输入）：

```bash
base64 -i /Users/aritxonly/Downloads/bdfun-release.jks | gh secret set KEYSTORE_BASE64 --repo QxTeak/bdFun --env pr-signing
gh secret set KEYSTORE_PASSWORD --repo QxTeak/bdFun --env pr-signing
gh secret set KEY_ALIAS --repo QxTeak/bdFun --env pr-signing --body bdfun
gh secret set KEY_PASSWORD --repo QxTeak/bdFun --env pr-signing
```

必须先创建 Environment，且当前 GitHub 账号需要该仓库的管理权限。如果 CI 实际跑在 fork，请把命令中的仓库替换为实际仓库。不要同时在命令行参数中填写密码。
