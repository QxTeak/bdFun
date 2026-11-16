#!/usr/bin/env python3
"""刷新已从字符替换为矢量的导出记录，保留 stable ID 以兼容旧预设。"""
from pathlib import Path
import json
import subprocess

ROOT = Path(__file__).resolve().parents[2]

def sync():
    path = ROOT / 'docs/icon-presets/issue-16.json'
    preset = json.loads(path.read_text())
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    files = [p for base in ['app', 'feature'] for p in (ROOT / base).rglob('*.kt')
             if '/src/main/' in str(p) and '/build/' not in str(p)]
    for item in preset['mappings']:
        if not item['currentId'].startswith('current:字符 ') or item['mode'] != 'replace':
            continue
        target = item['replacement']
        access = ('MiuixIcons.' + (target['id'].split(':')[1] if target['variant'] == 'Basic'
                                  else target['name'])) if target['family'] == 'Miuix' else (
            'LucideIcons.' + ''.join(x.capitalize() for x in target['name'].split('-')))
        original_paths = {x['path'] for x in item['locations']}
        locations = []
        for file in files:
            rel = str(file.relative_to(ROOT))
            if original_paths and rel not in original_paths:
                continue
            for line, text in enumerate(file.read_text().splitlines(), 1):
                if access in text and 'AppGlyph(' in text:
                    locations.append(dict(path=rel, line=line, snippet=text.strip(), kind='引用', revision=revision, localOnly=True))
        item['legacyName'] = item.get('legacyName', item['currentName'])
        item['currentName'] = item['semantic'] + ' · ' + access
        item['locations'] = locations
        item['implementation'] = dict(kind='vector', access=access, replacement=target,
                                      revision=revision, locations=locations, replacesSymbol=True)
    preset['implementationRevision'] = revision
    path.write_text(json.dumps(preset, ensure_ascii=False, indent=2) + '\n')
    return preset

if __name__ == '__main__':
    data = sync()
    print('Updated', sum(x.get('implementation', {}).get('replacesSymbol', False) for x in data['mappings']), 'symbol bindings')
