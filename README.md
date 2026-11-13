# bdFun

<p align="center">
  <img src="docs/icon.png" width="260" alt="bdFun">
</p>

<p align="center">
  <a href="https://github.com/QxTeak/bdFun/releases"><img src="https://img.shields.io/github/v/release/QxTeak/bdFun?include_prereleases&label=release&color=FF6699&style=flat-square" alt="release"></a>
  <a href="https://github.com/QxTeak/bdFun/releases"><img src="https://img.shields.io/github/downloads/QxTeak/bdFun/total?label=downloads&color=4CAF50&style=flat-square" alt="downloads"></a>
  <img src="https://img.shields.io/github/license/QxTeak/bdFun?label=license&color=FFA500&style=flat-square" alt="license">
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white&style=flat-square" alt="platform">
  <img src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white&style=flat-square" alt="kotlin">
  <img src="https://img.shields.io/badge/Compose-Jetpack-4285F4?logo=jetpackcompose&logoColor=white&style=flat-square" alt="compose">
  <img src="https://img.shields.io/badge/build-Gradle-02303A?logo=gradle&logoColor=white&style=flat-square" alt="build">
</p>

bdFun 是一个第三方哔哩哔哩 Android 客户端，使用 Kotlin + Jetpack Compose 编写，致力于轻快、流畅、简洁、美观的使用体验。

> 本项目为第三方非官方客户端，与哔哩哔哩（上海宽娱数码科技有限公司）及其关联方无任何隶属、授权或合作关系。项目仅用于学习、研究与技术交流，不提供、不包含、不传播任何破解内容、付费内容绕过机制或会员权益破解功能。

## 界面预览

<p align="center">
  <img src="docs/preview/1.png" height="340" alt="首页">
  <img src="docs/preview/2.png" height="340" alt="动态">
  <img src="docs/preview/3.png" height="340" alt="视频详情">
  <img src="docs/preview/4.png" height="340" alt="个人主页">
  <img src="docs/preview/5.png" height="340" alt="设置">
</p>

采用 HyperOS 4 的设计风格。

## 安装

系统要求：Android 13+

## 反馈

欢迎通过 Issue 反馈问题、提出建议，也可以加入我们的 [Telegram 群组](https://t.me/+tQrDYf-hb702ZjM1)。

## 构建

环境：

- JDK 21
- Android SDK Platform 37 / Build Tools 37.0.0
- Gradle Wrapper（仓库自带）

```bash
# Linux / macOS
./gradlew :app:assembleDebug

# Windows
gradlew.bat :app:assembleDebug
```

> Note: this project builds against a local Miuix source snapshot.
> Place it at `../miuix-src/miuix-45645854cd46c32275c5666e3aafa9980d8f104e`
> or set the `MIUIX_SOURCE` environment variable to the Miuix repo path.
> The app uses AGP 9.5.1, Kotlin 2.4.21 and Compose 1.12.2 to match this snapshot; composite builds cannot mix AGP versions.

Release：

```bash
./gradlew :app:assembleRelease
```

如果本机没有配置 Android SDK，请在项目根目录创建 `local.properties`：

```properties
sdk.dir=/path/to/Android/Sdk
```

## 项目结构

```text
app/              主应用（包名 com.aritxonly.bdfun）
TeakUI/           Compose 页面效果组件库
danmaku-engine/   基于 BiliPai / bytedance DanmakuRenderEngine 的弹幕渲染模块
```

TeakUI 是为方便开发而抽出的 Compose UI 组件库：把页面骨架、玻璃材质、顶部组件和动效统一收敛成一套组件，开发时直接调用，避免每个页面重复实现。它包含基于 Hyper-PiliPlus 的 HyperOS 4 风格 Tab 栏、顶部玻璃组件动画，以及基于 Miuix 的统一页面能力，包括渐进式模糊、大/小标题折叠动画等。

`danmaku-engine` 是 4.0 引入的独立弹幕渲染模块，负责弹幕解析、轨道计算和绘制，保留上游 Apache-2.0 版权与许可说明。

更多页面规范见 `TeakUI/docs/PAGE_CHROME_SPEC.md`，第三方来源清单见 `TeakUI/docs/PROVENANCE.md`。

## 致谢

bdFun 使用了 / 参考了以下项目，感谢作者和社区的工作：

- [Hyper-PiliPlus](https://github.com/AritxOnly/Hyper-PiliPlus)（[@AritxOnly](https://github.com/AritxOnly)，fork [@Mutx163](https://github.com/Mutx163)）：玻璃材质、折射、阴影、底栏弹簧、ProgressiveTopBar 等
- [HyperIsland](https://github.com/1812z/HyperIsland)（[@1812z](https://github.com/1812z)）：渐进模糊参数与观感参考
- [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus)（[@bggRGjQaUbCoE](https://github.com/bggRGjQaUbCoE)）：功能与交互参考
- [Miuix](https://github.com/compose-miuix-ui/miuix)（[@compose-miuix-ui](https://github.com/compose-miuix-ui)）：Compose UI / 玻璃 blur 依赖
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（[@Kyant0](https://github.com/Kyant0)，Apache-2.0）：柔光玻璃组件使用了其中部分渲染管线能力，包括折射着色器的圆形透镜轮廓与圆角矩形 SDF 思路
- [Shapes](https://github.com/Kyant0/Shapes)（[@Kyant0](https://github.com/Kyant0)，Apache-2.0）：使用 `Capsule()` 胶囊形状，用于玻璃表面、导航栏、Tab 指示器等
- [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)（[@SocialSisterYi](https://github.com/SocialSisterYi)）：哔哩哔哩接口资料
- [BiliPai](https://github.com/jay3-yy/BiliPai) / [bytedance DanmakuRenderEngine](https://github.com/bytedance/DanmakuRenderEngine)：弹幕渲染模块、弹幕空降助手、弹幕计数实现移植，水滴动画
- [FodFluidRipple](https://github.com/Leaf-lsgtky)（[@Leaf-lsgtky](https://github.com/Leaf-lsgtky)）：水波纹动画
- [Guiyuan](https://github.com/CHS-Haple/Guiyuan)（[@CHS-Haple](https://github.com/CHS-Haple)）：开始页移植
- HyperOS 4：一镜到底动效
- AndroidX / Jetpack Compose、Media3 / ExoPlayer、OkHttp、Coil、Kotlin / kotlinx.coroutines、ZXing

感谢所有上游项目作者与贡献者。

## 许可证

本项目以 **GNU General Public License v3.0** 发布，完整文本见 [LICENSE](LICENSE)。

Copyright (C) 2026 bdFun contributors

你可以自由使用、修改、再分发，但需要：

1. 保留 `LICENSE`、`THIRD_PARTY_NOTICES.md`、`TeakUI/docs/PROVENANCE.md`；
2. 对外分发修改版时，按 GPL-3.0 提供对应完整源码；
3. 不额外添加与 GPL-3.0 冲突的限制。

第三方依赖与许可说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 免责声明

- 本项目按“现状”提供，不对稳定性、可用性、安全性作任何保证。
- 所有视频、音频、图片、弹幕等内容版权归哔哩哔哩及相应权利人所有，本项目不主张任何内容权利。
- 因官方接口变更、策略调整或不可抗力导致的功能失效或损失，开发者不承担责任。
- 使用本项目即表示你已阅读并同意完整的使用须知；如不同意，请停止使用并删除本项目。

### 固定 Miuix 快照

`miuix-glass` 来自 `45645854cd46c32275c5666e3aafa9980d8f104e` 快照，不能假定 Maven 上的 `0.9.4` 包含它。
先运行 `python3 tools/setup-miuix.py`（也可用 `--source /path/to/snapshot` 复制本机快照）。
源码存入被 Git 忽略的 `.local/`，Gradle 自动使用；仍可通过 `MIUIX_SOURCE` 或 `-PmiuixSource=` 覆盖。
快照采用 Apache-2.0 许可证，脚本保留其 LICENSE；不把临时路径写进版本库。

核心边界验证：`python3 tools/check-layering.py`，或运行 Gradle `checkLayering`。
