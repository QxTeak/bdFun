# Third-Party Notices

bdFun 包含或参考了以下第三方项目的代码、材质和参数。分发时请保留本文件，并遵守各项目许可证。

## Hyper-PiliPlus

- 仓库：https://github.com/AritxOnly/Hyper-PiliPlus
- 上游 fork：https://github.com/Mutx163/Hyper-PiliPlus
- 许可证：GNU General Public License v3.0（GPL-3.0）
- 使用方式：**直接复用 / 移植**，不是仅参考。包括首页悬浮胶囊底栏、选中块指示器动画、玻璃材质与 recipe、玻璃返回键背景、视频页 Tab 配套、材质层级/折射/阴影等。
- 文件级清单：`TeakUI/docs/PROVENANCE.md`

注意：GPL-3.0 具有传染性。包含这些直接移植代码的 bdFun 如果对外分发，通常需要以 GPL-3.0 兼容方式提供完整对应源码，并保留许可证和版权声明。不能只写“感谢”而按闭源方式发布。

## HyperIsland

- 仓库：https://github.com/1812z/HyperIsland
- 许可证：MIT
- 使用方式：**仅参数/观感参考**，未复制源码。参考了顶栏渐进式模糊的部分参数和分层方式；`TeakProgressiveTopBar` 实际来自 Hyper-PiliPlus，不是 HyperIsland。
- 如果后续复制了 HyperIsland 的代码，必须保留其 MIT 版权与许可声明。

## Miuix

- 仓库：https://github.com/compose-miuix-ui/miuix
- 许可证：Apache License 2.0
- 使用方式：第三方依赖，未复制源码。提供 `miuix-ui`、`miuix-blur`、`miuix-icons`、`MiuixTheme`、`LayerBackdrop`、`textureBlur`、`progressiveTextureBlur` 等。
- 分发时按 Apache-2.0 保留 NOTICE / 许可证信息（如上游提供）。

## BiliPai / bytedance DanmakuRenderEngine

- 仓库：https://github.com/jay3-yy/BiliPai
- 上游：https://github.com/bytedance/DanmakuRenderEngine（commit `577d3f4e170283a5bdb198af3ca924b8c32d1ab4`）
- 许可证：Apache License 2.0
- 使用方式：**完整移植** `danmaku-engine` 模块（66 个源文件）到本地 `danmaku-engine/`，保留上游版权头和 `LICENSE` / `UPSTREAM.md`。
- 本地仅做了两类小改动：`TextData` 增加 `isSelf` 字段、`TextDrawItem` 增加自发送弹幕的粉色圆角底；其余轨道、渲染、解析代码保持上游。
- 分发时保留该模块的 `LICENSE` 与版权声明。

## 其他主要依赖

| 项目 | 地址 | 许可证 |
| --- | --- | --- |
| PiliPlus | https://github.com/bggRGjQaUbCoE/PiliPlus | GPL-3.0 |
| bilibili-API-collect | https://github.com/SocialSisterYi/bilibili-API-collect | 见仓库 |
| AndroidX / Jetpack Compose | https://github.com/androidx/androidx | Apache-2.0 |
| Media3 / ExoPlayer | https://github.com/androidx/media | Apache-2.0 |
| OkHttp | https://github.com/square/okhttp | Apache-2.0 |
| Coil | https://github.com/coil-kt/coil | Apache-2.0 |
| Kotlin / kotlinx.coroutines | https://github.com/JetBrains/kotlin | Apache-2.0 |
| ZXing | https://github.com/zxing/zxing | Apache-2.0 |

## 当前仓库

bdFun 根目录已包含 GPL-3.0 的 `LICENSE`。由于直接包含 GPL-3.0 的 Hyper-PiliPlus 代码，bdFun 按 GPL-3.0 分发；对外分发修改版时需要保留本文件、`TeakUI/docs/PROVENANCE.md`，并按 GPL-3.0 提供完整对应源码。具体法律问题请以各项目 LICENSE 原文为准。

详细逐文件来源：`TeakUI/docs/PROVENANCE.md`

## Deadliner glass and layout sync (2026-11-10)

- Deadliner: https://github.com/AritxOnly/Deadliner — MIT, Copyright (c) 2024 Aritx Zhou. License: `TeakUI/third_party/deadliner/LICENSE`.
- MIUIX glass dependency closure: PR #423, commit `ec4c96963abf206ab2f2b0253a5efca71ba28043`, Apache-2.0. License and upstream hashes: `TeakUI/third_party/miuix-glass/`. Sources: `TeakUI/src/main/kotlin/top/yukonga/miuix/kmp/glass/`.
- Scope, source hashes and explicit bdFun material overrides: `docs/DEADLINER_GLASS_SYNC.md`.
