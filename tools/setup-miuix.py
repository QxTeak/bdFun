#!/usr/bin/env python3
"""准备版本锁定的 Miuix 源码；默认下载，也可复制已验证的本机快照。"""
import argparse
from pathlib import Path
import shutil
import tarfile
import tempfile
import urllib.request

COMMIT = '45645854cd46c32275c5666e3aafa9980d8f104e'
ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / '.local' / ('miuix-' + COMMIT)
OVERRIDES = ROOT / 'tools' / 'miuix-patches'
URL = f'https://codeload.github.com/compose-miuix-ui/miuix/tar.gz/{COMMIT}'

# Pinned-commit overrides. NavDisplay.kt adds `predictiveBackPreview` so the app can
# reuse the same transition with and without finger tracking. Keep this list small and
# refresh the file when bumping COMMIT.
OVERRIDE_FILES = {
    'miuix-nav/src/commonMain/kotlin/top/yukonga/miuix/kmp/nav/core/NavDisplay.kt': 'NavDisplay.kt',
}


def apply_overrides(target: Path) -> None:
    for relative, name in OVERRIDE_FILES.items():
        source = OVERRIDES / name
        destination = target / relative
        if not source.is_file() or not destination.is_file():
            raise SystemExit(f'override missing: {name} -> {relative}')
        shutil.copyfile(source, destination)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, help='已验证的本机源码目录')
    args = parser.parse_args()
    if TARGET.exists():
        raise SystemExit(f'目标已存在，未覆盖：{TARGET}')
    with tempfile.TemporaryDirectory() as directory:
        if args.source:
            source = args.source.resolve()
        else:
            archive = Path(directory) / 'source.tar.gz'
            urllib.request.urlretrieve(URL, archive)
            with tarfile.open(archive) as bundle:
                bundle.extractall(directory, filter='data')
            source = Path(directory) / ('miuix-' + COMMIT)
        for name in ('settings.gradle.kts', 'miuix-glass/build.gradle.kts', 'LICENSE'):
            if not (source / name).is_file():
                raise SystemExit(f'快照缺少必要文件：{name}')
        shutil.copytree(source, TARGET, ignore=shutil.ignore_patterns('build', '.gradle', '.kotlin', '.git'))
        apply_overrides(TARGET)
    print(f'已准备 {TARGET}；来源 {args.source or URL}')


if __name__ == '__main__':
    main()
