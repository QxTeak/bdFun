# 手动运行构建与发布（GitHub Actions）

- **Android 手动构建**：仅在 Actions 页面点击 **Run workflow** 时运行，构建 Debug 和 Release，检查 Release 的代码压缩构建，并直接上传 Debug APK（保留 14 天），下载时无需解压 ZIP。普通构建不需要 Secrets。
- **Android 手动发布**：仅在 Actions 页面点击 **Run workflow** 并填写已有版本标签时运行。流程检出该标签的代码，构建正式签名的 Release APK，校验签名，再自动创建 GitHub Release、生成更新说明并上传 APK、`SHA256SUMS.txt` 和混淆映射 `mapping.txt`。所有自动创建的版本统一标记为预发布，管理员验证后可编辑 Release，取消预发布标记并保存，手动转为正式版。
- 推送代码、创建 PR 或推送标签均不会自动运行这两个工作流。
- 标签必须是 `v` 加上 `app/build.gradle.kts` 中的 `versionName`，例如版本名为 `preview-5.0` 时对应 `vpreview-5.0`；不匹配会停止发布。同一个标签重跑时只更新附件，保留管理员设置的正式版或预发布状态。

## 首次配置签名

在 GitHub 仓库 **Settings → Secrets and variables → Actions → New repository secret** 添加：

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | 签名 keystore 文件的 Base64 编码 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 签名密钥别名 |
| `KEY_PASSWORD` | 签名密钥密码 |

如果已有发行版本，应使用原来的签名密钥，以便用户覆盖升级。没有密钥时，可使用 JDK 自带的工具创建，按提示设置密码并妥善备份：

```bash
keytool -genkeypair -v -keystore release.jks -storetype JKS -alias bdfun -keyalg RSA -keysize 4096 -validity 10000
```

在 PowerShell 中将文件编码并复制到剪贴板，粘贴为 `KEYSTORE_BASE64`：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ./release.jks).Path)) | Set-Clipboard
```

Linux 可使用 `base64 -w 0 release.jks`；macOS 可使用 `base64 -i release.jks | tr -d '\n'`。不要将密钥或密码提交到仓库。Release 工作流缺少任一签名 Secret 时会失败，不会发布临时调试签名的 APK。

## 手动构建

构建环境使用 JDK 21、Android SDK Platform 37 和 Build Tools 37.0.0。SDK 平台包在官方仓库中的实际标识为 `platforms;android-37.0`，安装命令如下；项目的 `compileSdk = 37` 无需因此修改：

```bash
sdkmanager 'platforms;android-37.0' 'build-tools;37.0.0'
```

将工作流提交到仓库默认分支后，进入 **Actions → Android 手动构建 → Run workflow**，选择需要构建的分支并确认运行。完成后在该次运行的 Artifacts 中点击 `bdFun-debug-提交哈希.apk`，即可直接下载 APK 文件。

工作流使用 `actions/upload-artifact@v7` 的 `archive: false` 上传原文件。发布流程也将 APK、校验文件和混淆映射分别上传；既可在 Actions 的 Artifacts 中逐个下载，也可在发布成功后从 Releases 页面下载。

## 手动发布新版本

1. 修改 `app/build.gradle.kts` 中的 `versionName`，并递增 `versionCode`，提交代码。
2. 将工作流提交到仓库默认分支，并将版本修改推送到 GitHub，再推送对应标签。以下命令以在 `main` 分支发布为例：

```bash
git push origin main
git tag vpreview-5.0
git push origin vpreview-5.0
```

3. 进入 **Actions → Android 手动发布 → Run workflow**，在分支选择器中选择包含最新工作流的默认分支，在版本标签输入框中填写 `vpreview-5.0`，然后确认运行。实际构建的代码来自输入的标签；标签必须已经存在。

以上为版本标签格式的示例；后续请替换成新版本标签。仅推送标签不会启动发布。构建完成后可在仓库的 Releases 页面下载。发布使用 GitHub 自动提供的 `GITHUB_TOKEN`，不需要额外的个人令牌。

本地 `assembleRelease` 未设置签名环境变量时优先读取原有 keystore.properties，缺少该文件才回退调试签名。若要本地正式签名，设置 `ANDROID_KEYSTORE_PATH`（绝对路径）以及同名的三个密码/别名环境变量。使用正式密钥前已安装的调试签名版本可能需要先卸载；卸载会清除应用数据。

PR 自动检查与签名测试包请参阅 [CI.md](CI.md)。手动发布使用仓库 Secrets，PR 签名使用 pr-signing Environment Secrets；两处使用相同的四个名称。云端签名优先于本地 keystore.properties，使用 V2。
