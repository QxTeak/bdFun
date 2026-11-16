#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT

# 使用与 APK 相同的背景资源；108dp 中心 72dp 为桌面静态可视区域。
magick app/src/main/res/drawable-nodpi/ic_launcher_adaptive_background.png \
    app/src/main/res/drawable-nodpi/ic_launcher_adaptive_foreground.png \
    -composite -crop 720x720+180+180 +repage -resize 320x320 "$work_dir/art.png"
shapes=(Rounded-rectangle Circle Squircle Teardrop)
paths=(
    'roundrectangle 0,0 319,319 72,72'
    'circle 160,160 160,0'
    'path "M 160,0 C 300,0 320,20 320,160 C 320,300 300,320 160,320 C 20,320 0,300 0,160 C 0,20 20,0 160,0 Z"'
    'path "M 160,0 L 320,0 L 320,160 C 320,248 248,320 160,320 C 72,320 0,248 0,160 C 0,72 72,0 160,0 Z"'
)
for i in 0 1 2 3; do
    magick -size 320x320 xc:none -fill white -draw "${paths[$i]}" "$work_dir/mask.png"
    magick "$work_dir/art.png" "$work_dir/mask.png" -alpha off \
        -compose CopyOpacity -composite -compose Over -background '#202026' -alpha remove \
        -gravity south -splice 0x48 -font Helvetica -pointsize 20 -fill white \
        -annotate +0+12 "${shapes[$i]}" -bordercolor '#202026' -border 20 "$work_dir/$i.png"
done
magick "$work_dir/0.png" "$work_dir/1.png" "$work_dir/2.png" "$work_dir/3.png" \
    +append docs/adaptive-icon-preview.png
