#!/usr/bin/env python3
"""将已审阅的 Lucide SVG 固定样本转换为 Android 矢量，不在构建时下载素材。"""
from pathlib import Path
import json
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', ANDROID)
STROKE = 1.8


def shape_path(node):
    a = node.attrib
    if node.tag == 'path':
        return a['d']
    if node.tag == 'circle':
        x, y, r = (float(a[k]) for k in ('cx', 'cy', 'r'))
        return f'M{x-r},{y} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0 Z'
    if node.tag == 'rect':
        x, y = float(a.get('x', 0)), float(a.get('y', 0))
        w, h = float(a['width']), float(a['height'])
        rx = min(float(a.get('rx', a.get('ry', 0))), w / 2)
        ry = min(float(a.get('ry', a.get('rx', 0))), h / 2)
        if not rx or not ry:
            return f'M{x},{y} h{w} v{h} h{-w} Z'
        return (f'M{x+rx},{y} H{x+w-rx} A{rx},{ry} 0 0,1 {x+w},{y+ry} '
                f'V{y+h-ry} A{rx},{ry} 0 0,1 {x+w-rx},{y+h} '
                f'H{x+rx} A{rx},{ry} 0 0,1 {x},{y+h-ry} '
                f'V{y+ry} A{rx},{ry} 0 0,1 {x+rx},{y} Z')
    if node.tag == 'line':
        return f'M{a["x1"]},{a["y1"]} L{a["x2"]},{a["y2"]}'
    if node.tag in ('polygon', 'polyline'):
        return 'M' + a['points'] + (' Z' if node.tag == 'polygon' else '')
    raise ValueError(f'不支持的 SVG 形状: {node.tag}')


def drawable(icon):
    root = ET.Element('vector', {f'{{{ANDROID}}}{k}': str(v) for k, v in {
        'width': '24dp', 'height': '24dp', 'viewportWidth': icon['width'],
        'viewportHeight': icon['height'],
    }.items()})
    parent = root
    if icon['variant'] == 'Filled':
        # 实心 Extended 外轮廓接近 22 单位；收至与 Regular 约 20 单位的视觉包络一致。
        parent = ET.SubElement(root, 'group', {f'{{{ANDROID}}}{k}': str(v) for k, v in {
            'pivotX': 12, 'pivotY': 12, 'scaleX': .92, 'scaleY': .92,
        }.items()})

    def visit(node, inherited):
        attrs = inherited | node.attrib
        if 'transform' in attrs:
            raise ValueError('禁止静默丢弃 SVG 变换')
        if node.tag in ('svg', 'g'):
            for child in node:
                visit(child, attrs)
            return
        props = {'pathData': shape_path(node)}
        if attrs.get('fill', 'currentColor') != 'none':
            props['fillColor'] = '#FF000000'
        if attrs.get('stroke', 'none') != 'none':
            props.update(strokeColor='#FF000000', strokeWidth=str(STROKE),
                         strokeLineCap=attrs.get('stroke-linecap', 'butt'),
                         strokeLineJoin=attrs.get('stroke-linejoin', 'miter'))
        props['fillType'] = 'evenOdd' if attrs.get('fill-rule') == 'evenodd' else 'nonZero'
        ET.SubElement(parent, 'path', {f'{{{ANDROID}}}{k}': v for k, v in props.items()})
    visit(ET.fromstring('<svg>' + icon['body'] + '</svg>'), {})
    ET.indent(root)
    return '<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(root, encoding='unicode') + '\n'


def generate():
    icons = json.loads((ROOT / 'tools/icons/lucide-selected.json').read_text())['icons']
    output = ROOT / 'TeakUI/src/main/res/drawable'
    output.mkdir(parents=True, exist_ok=True)
    props = []
    for icon in icons:
        name = 'hybrid_' + icon['name'].replace('-', '_')
        (output / (name + '.xml')).write_text(drawable(icon))
        prop = ''.join(p.capitalize() for p in icon['name'].split('-'))
        props.append(f'    val {prop} = R.drawable.{name}')
    (ROOT / 'TeakUI/src/main/kotlin/com/aritxonly/teak/common/icons/LucideIcons.kt').write_text(
        'package com.aritxonly.teak.common.icons\n\nimport com.aritxonly.teak.common.R\n\n'
        '/** 固定的已审阅素材；由 tools/icons/generate_lucide.py 生成，调用方共用同一资源。 */\n'
        'object LucideIcons {\n' + '\n'.join(props) + '\n}\n')


if __name__ == '__main__':
    generate()
