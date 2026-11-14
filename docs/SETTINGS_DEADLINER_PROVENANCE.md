# 关于页设计来源

参考本机 Deadliner Android `3b561656e1ddca43220a39763f12aaa0c67fdaba` 的 `app/src/main/java/com/aritxonly/deadliner/ui/settings/AboutSettings.kt`。

迁入完整 AboutFloatingBackground 绘制与 toVividGlowColor 色彩辅助函数，保留 7500/5600/6400ms 动画、三层光晕比例/强度及320dp滚动淡出；身份区保留252dp、96dp图标、24dp圆角和名称/版本的层级。卡片透明度仍由0.5随滚动增加至1。bdFun已有TeakUI提供主题/玻璃/顶栏，不重复迁入Deadliner整个Material Scaffold或业务路由。应用名/图标/版本与行操作替换成bdFun自己的数据，不展示虚假的KMP标签；遵从bdFun减弱动效设置。普通设置控件仍复用原Miuix实现。

## 上游许可

MIT License

Copyright (c) 2024 Aritx Zhou

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
