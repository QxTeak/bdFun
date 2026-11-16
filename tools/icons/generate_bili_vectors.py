#!/usr/bin/env python3
"""将固定的原生图标轮廓转换成 VectorDrawable，不再在设备端缩放 PNG。"""
import json
import re
from pathlib import Path
from xml.sax.saxutils import quoteattr

ROOT = Path(__file__).resolve().parents[2]


def render(icon):
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
             f'    android:width="24dp" android:height="24dp"',
             f'    android:viewportWidth="{icon["width"]}" android:viewportHeight="{icon["height"]}">']
    for path in icon['paths']:
        match = re.fullmatch(r'translate\((-?[\d.]+)[ ,]+(-?[\d.]+)\)', path['transform'])
        if path['transform'] and not match:
            raise ValueError('Unsupported path transform: ' + path['transform'])
        if match:
            lines.append(f'    <group android:translateX="{match[1]}" android:translateY="{match[2]}">')
        lines.append('        <path android:fillColor=' + quoteattr(path.get('color', icon.get('color', '#FFFFFFFF'))) +
                     ' android:fillType="nonZero" android:pathData=' + quoteattr(path['d']) + ' />')
        if match:
            lines.append('    </group>')
    return '\n'.join(lines + ['</vector>', ''])


def main():
    icons = json.loads((ROOT / 'tools/icons/bili-vectors.json').read_text())['icons']
    destination = ROOT / 'app/src/main/res/drawable'
    destination.mkdir(exist_ok=True)
    for icon in icons:
        (destination / (icon['name'] + '.xml')).write_text(render(icon))
    print('Generated', len(icons), 'native vectors')


if __name__ == '__main__':
    main()
