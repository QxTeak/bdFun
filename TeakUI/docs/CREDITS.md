# 来源与致谢

本文件记录 bdFun 中与第三方代码、视觉、材质和动画参数的来源关系。
逐文件的精确分类见 [PROVENANCE.md](PROVENANCE.md)。

这里只是工程来源记录，不替代上游项目的 LICENSE；各上游项目的许可与版权归其作者所有。

## HyperPiliPlus

HyperPiliPlus 不只是“参考”，其中有一部分是直接移植，也有一部分是保留功能但经过 Teak 重构：

**直接移植 / 基本同源：**

- 玻璃材质参数、折射 shader、玻璃阴影、按压反馈
- 材质层级 / 调参模型
- 底栏选中块弹簧动画 `FloatingTabMotion`
- 顶栏材质 `ProgressiveTopBarMaterial`

**同源但重构：**

- 玻璃表面 / 材质入口 / 底栏 / 底栏阴影

**使用其材质或视觉体系，但组件本身新写：**

- 玻璃返回键的按钮组合
- `TeakPagerTabs` 的玻璃底座与指示器观感

**直接来自 HyperPiliPlus 的视觉体系：**

- 首页悬浮胶囊底栏
- 选中块指示器动画
- 玻璃 recipe / 返回键玻璃背景
- 视频页 tab 的玻璃和指示器配套
- 相关设计风格和动画参数

## HyperIsland

**没有复制 HyperIsland 的源码。** 只参考渐进式模糊的部分参数/观感：

以下为历史参数。现行顶栏参数与 Deadliner 的对照见[同步记录](../../docs/DEADLINER_GLASS_SYNC.md#2026-11-28-顶栏渐进模糊)。

- `curve = 1.25`
- `blurRadius = 16`
- `tint = 0.66`
- 顶栏渐进模糊的分层方式

具体参数由项目自己调整，个别值（例如 `startFraction`）并不相同。  
`chrome/TeakProgressiveTopBar.kt` 来自 HyperPiliPlus，不是 HyperIsland。

## Miuix

Miuix 主要作为第三方依赖，提供：

- `LayerBackdrop` / `rememberLayerBackdrop`
- `textureBlur`
- `progressiveTextureBlur`
- `ProgressiveBlur`
- `MiuixTheme` / `ThemeController`

底层落到 Android RuntimeShader / RenderEffect。

## Teak 自己

- 页面外壳、大标题/小标题、顶部 overlay
- 设置页规范
- `TeakPagerTabs` 的组件代码
- `TeakTheme` 主题适配层
- 位置、字号、折叠进度、页面级状态等规范

## Deadliner Dialog / Sheet（MODAL-01）

- Dialog 宿主、窗口采样、按钮与表单角色、Sheet 尺寸和布局迁移自 Deadliner，MIT，Copyright (c) 2024 Aritx Zhou。
- `modal/sheet/MiuixSheetContentLayout.kt` 和 `ScaffoldSheetOverlay.kt` 包含经 Deadliner 调整的 MIUIX Sheet 源码，保留原 Apache-2.0 标头；许可证见 `../third_party/miuix/LICENSE`。
- 完整输入版本、SHA-256、包名适配和 bdFun 材质参数保留说明见仓库 `docs/deadliner-modal-migration.md`。
