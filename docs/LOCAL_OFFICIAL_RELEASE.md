# 本地官方签名 Release

运行：

```bash
python3 tools/build-official-release.py
```

默认读取仓库外的 `~/Downloads/bdfun-release.jks`，alias 为 `bdfun`。密码通过不回显提示输入；key 密码回车表示使用相同密码。也可由调用环境提供 `ANDROID_KEYSTORE_PATH`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`，与现有 GitHub Actions 的 Gradle 签名入口一致。脚本不保存密码、不创建仓库内签名配置，不自动上传 GitHub Secrets。

可选参数：

```bash
python3 tools/build-official-release.py --keystore /仓库外/bdfun-release.jks --version preview-6.0
```

不传 `--version` 时沿用工程默认版本（当前为 `preview-6.0` / `versionCode 600`）。脚本固定包名 `com.aritxonly.bdfun`；构建前导出公开证书检查官方 SHA-256，构建后用 `apksigner` 检查 V2、证书指纹，以及 APK 实际包名，拒绝其他签名方案。官方证书指纹：

```text
a34f7803b15c3ebba2e6c4c7bc9646ae14bffb15abad4fe58aaf2ca46566e5db
```

本机有 `.local-build` 离线适配时自动使用；其他环境走标准 Gradle。需要 JDK 21 和 Android SDK build-tools 37.0.0。输出默认位于忽略目录 `.local/artifacts/official-release/`，包含 APK、签名验证文本及 APK SHA-256。可用 `--output` 修改目录；只有全部验证成功才复制交付 APK。

keystore、密码和 Base64 密钥不要提交。现有 GitHub 工作流已经读取 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`，本脚本不修改远端 Secrets 或发布 GitHub Release。
