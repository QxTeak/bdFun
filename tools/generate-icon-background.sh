#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# 从原启动图标未被标志遮挡的位置采样；不把原有圆角带入自适应图层。
source_icon=app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
samples=''
for point in '30 15' '96 10' '162 15' '12 60' '180 60' '10 96' '181 96' '12 130' '180 130' '30 177' '96 181' '162 177'; do
    read -r x y <<< "$point"
    color=$(magick "$source_icon" -format "%[pixel:p{$x,$y}]" info:)
    # 原 192px 图标映射到 108dp 图层中央的 72dp 可视区域。
    samples+=" $((180 + x * 720 / 192)),$((180 + y * 720 / 192)) $color"
done
magick -size 1080x1080 xc:'#FC5998' -alpha off \
    -sparse-color Shepards "$samples" \
    app/src/main/res/drawable-nodpi/ic_launcher_adaptive_background.png
