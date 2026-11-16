#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# 从原始像素估算透明度，去除粉色底色污染，避免生成式重绘改变字形。
# 需要 ImageMagick 7；中间文件仅放在临时目录。
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT
magick docs/icon.png -alpha on \
    -channel A -fx 'clamp((max(g-0.37*r,b-0.61*r)-0.12)/0.49)' \
    -channel R -fx 'a>0.001?clamp((r-(1-a)*0.91)/a):0' \
    -channel G -fx 'a>0.001?clamp((g-(1-a)*0.35)/a):0' \
    -channel B -fx 'a>0.001?clamp((b-(1-a)*0.55)/a):0' \
    +channel "$work_dir/cutout.png"
magick "$work_dir/cutout.png" -trim +repage -resize 682x682 \
    -gravity center -background none -extent 1080x1080 \
    app/src/main/res/drawable-nodpi/ic_launcher_adaptive_foreground.png
