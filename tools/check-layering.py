#!/usr/bin/env python3
"""检查 Kotlin 源码依赖边界（含完全限定名）；Gradle 模块图另行验收。"""
import argparse
from pathlib import Path
import re


def code_only(source):
    """屏蔽注释与字面文本，但保留字符串模板表达式中的真实代码和行号。"""
    chars = list(source)
    size = len(source)

    def blank(start, end):
        for n in range(start, min(end, size)):
            if chars[n] != '\n': chars[n] = ' '

    def string(i, delimiter):
        start = i
        raw = delimiter == '\"\"\"'
        i += len(delimiter)
        while i < size:
            if not raw and source[i] == '\\':
                i += 2
            elif source.startswith(delimiter, i):
                i += len(delimiter)
                blank(start, i)
                return i
            elif delimiter != "'" and source.startswith('${', i):
                blank(start, i + 2)
                i = code(i + 2, template=True)
                start = i
            else:
                i += 1
        blank(start, i)
        return i

    def code(i, template=False):
        braces = 0
        while i < size:
            start = i
            if source.startswith('//', i):
                end = source.find('\n', i)
                i = size if end < 0 else end
                blank(start, i)
            elif source.startswith('/*', i):
                depth = 1
                i += 2
                while i < size and depth:
                    if source.startswith('/*', i): depth += 1; i += 2
                    elif source.startswith('*/', i): depth -= 1; i += 2
                    else: i += 1
                blank(start, i)
            elif source.startswith('\"\"\"', i):
                i = string(i, '\"\"\"')
            elif source[i] in ('"', "'"):
                i = string(i, source[i])
            elif template and source[i] == '}':
                if braces == 0:
                    blank(i, i + 1)
                    return i + 1
                braces -= 1
                i += 1
            else:
                if template and source[i] == '{': braces += 1
                i += 1
        return i

    code(0)
    return ''.join(chars)


PLATFORM = r'\b(?:android|androidx|okhttp3|org\.json|coil|coil3)\.'
IMPLEMENTATION = r'\bcom\.aritxonly\.bdfun\.(?:data|infra)\.'
PRESENTATION_PLATFORM = r'\b(?:okhttp3|org\.json)\.'
# 旧展示的诊断钩子仍待注入；仅限这两个类型，不能借此放行整个 data 包。
DIAGNOSTICS = re.compile(r'\bcom\.aritxonly\.bdfun\.data\.(?:LogCollector|LiveCounters)\b')
APP_PACKAGE = Path('app/src/main/kotlin/com/aritxonly/bdfun')


def violations(root):
    errors = []
    def check(path, pattern, message, diagnostics=False):
        code = code_only(path.read_text(encoding='utf-8'))
        if diagnostics: code = DIAGNOSTICS.sub(lambda m: ' ' * len(m[0]), code)
        for line, text in enumerate(code.splitlines(), 1):
            if re.search(pattern, text): errors.append(f'{path}:{line}: {message}：{text.strip()}')
    for layer in ('model', 'domain', 'common', 'application', 'navigation'):
        for path in (root / 'core' / layer).rglob('*.kt'):
            check(path, PLATFORM + '|' + IMPLEMENTATION, '核心层依赖平台或实现')
    for feature_root in (root / 'feature', root / APP_PACKAGE / 'feature'):
        for path in feature_root.rglob('*.kt'):
            parts = path.relative_to(feature_root).parts
            feature = parts[0]
            code = code_only(path.read_text(encoding='utf-8'))
            for line, text in enumerate(code.splitlines(), 1):
                for match in re.finditer(r'\bcom\.aritxonly\.bdfun\.feature\.(\w+)', text):
                    if match[1] != feature:
                        errors.append(f'{path}:{line}: feature {feature} 引用了 {match[1]}')
            if 'application' in parts:
                check(path, PLATFORM + '|' + IMPLEMENTATION + r'|@Composable\b', '应用层引用平台或实现')
            if 'presentation' in parts:
                check(path, PRESENTATION_PLATFORM + '|' + IMPLEMENTATION + r'|\bapi\.\w+\s*\(', '表现层直接访问基础设施')
    for path in (root / APP_PACKAGE / 'presentation').rglob('*.kt'):
        check(path, PRESENTATION_PLATFORM + '|' + IMPLEMENTATION + r'|\bapi\.\w+\s*\(',
              '表现层直接访问基础设施', diagnostics=True)
    for path in (root / APP_PACKAGE / 'presentation/space').rglob('*.kt'):
        check(path, r'\bAppGraph\.', '空间展示回查进程依赖，应由Activity显式传入')
    # 旧播放器目录逐步迁移；已移出的原生实现不得重新进入展示。
    for path in (root / APP_PACKAGE / 'ui/player').rglob('*.kt'):
        check(path, r'\b(?:androidx\.media3|com\.aritxonly\.bdfun\.infra)\.'
              r'|\bcom\.aritxonly\.bdfun\.data\.BiliApi\b', '播放器展示引用原生或网络实现')
    video_sources = list((root / APP_PACKAGE / 'ui/player').rglob('*.kt'))
    video_sources += list((root / APP_PACKAGE / 'ui/video').rglob('*.kt'))
    player_root = root / APP_PACKAGE / 'ui/VideoPlayer.kt'
    if player_root.exists(): video_sources.append(player_root)
    for path in video_sources:
        check(path, r'\bAppGraph\.settings\b|\bDanmakuSettings\.'
              r'|\bcom\.aritxonly\.bdfun\.ui\.DanmakuSettings\b', '视频展示回查全局设置')
    explicit_page_sources = video_sources + list((root / APP_PACKAGE / 'presentation/comment').rglob('*.kt'))
    video_root = root / APP_PACKAGE / 'ui/VideoPlayer.kt'
    if video_root.exists(): explicit_page_sources.append(video_root)
    for path in explicit_page_sources:
        check(path, r'\bAppGraph\.|\bPlaybackProgressStore\b|\bVideoPlayQueue\b|\bBackgroundPlayback\.'
              r'|\bcom\.aritxonly\.bdfun\.data\.LogCollector\b', '视频/评论展示须使用显式页面依赖')
    for path in (root / APP_PACKAGE).glob('*Activity.kt'):
        check(path, r'\bcom\.aritxonly\.bdfun\.(?:infra\.bili\.|data\.BiliApi\b)'
              r'|\bAppGraph\.api\b', 'Activity外壳直接访问网络实现')
    return errors


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    errors = violations(args.root)
    for error in errors: print(error)
    raise SystemExit(bool(errors))
