#!/usr/bin/env python3
"""构建并验证与 GitHub Actions 相同证书、包名和 V2 方案的正式 APK。"""
import argparse
import getpass
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
FINGERPRINT = 'a34f7803b15c3ebba2e6c4c7bc9646ae14bffb15abad4fe58aaf2ca46566e5db'
PACKAGE = 'com.aritxonly.bdfun'


def run(args, env, **kwargs):
    return subprocess.run(args, env=env, check=True, **kwargs)


def validate_signature(report):
    schemes = re.findall(r'^Verified using (v\S+) scheme .*: true$', report, re.MULTILINE)
    if schemes != ['v2']:
        raise ValueError('APK 必须只使用 V2 签名')
    fingerprints = re.findall(r'^(?:Signer #\d+|V\d+(?:\.\d+)? Signer:) certificate SHA-256 digest: (\w+)$', report, re.MULTILINE)
    if fingerprints != [FINGERPRINT]:
        raise ValueError('APK 必须只有一个官方签名证书')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--keystore', type=Path, default=Path(os.environ.get(
        'ANDROID_KEYSTORE_PATH', str(Path.home() / 'Downloads/bdfun-release.jks'))))
    parser.add_argument('--alias', default=os.environ.get('KEY_ALIAS', 'bdfun'))
    parser.add_argument('--version', help='可选：preview-y.z 或 x.y.z，与 CI 的 releaseVersion 相同')
    parser.add_argument('--output', type=Path, default=ROOT / '.local/artifacts/official-release')
    args = parser.parse_args()
    keystore = args.keystore.expanduser().resolve(strict=True)
    if keystore.is_relative_to(ROOT):
        parser.error('keystore 必须放在仓库外')
    env = os.environ.copy()
    env.setdefault('JAVA_HOME', '/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home')
    env.setdefault('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk'))
    env['ANDROID_KEYSTORE_PATH'] = str(keystore)
    env['KEY_ALIAS'] = args.alias
    env['KEYSTORE_PASSWORD'] = env.get('KEYSTORE_PASSWORD') or getpass.getpass('Keystore 密码（不回显）: ')
    env['KEY_PASSWORD'] = env.get('KEY_PASSWORD') or getpass.getpass('Key 密码（回车使用相同密码）: ') or env['KEYSTORE_PASSWORD']
    keytool = Path(env['JAVA_HOME']) / 'bin/keytool'
    cert = run([str(keytool), '-exportcert', '-keystore', str(keystore), '-alias', args.alias,
                '-storepass:env', 'KEYSTORE_PASSWORD'], env, capture_output=True).stdout
    if hashlib.sha256(cert).hexdigest() != FINGERPRINT:
        parser.error('证书 SHA-256 不符合官方签名，已停止构建')
    sdk_tools = Path(env['ANDROID_HOME']) / 'build-tools/37.0.0'
    for tool in ['apksigner', 'aapt2']:
        if not (sdk_tools / tool).is_file():
            parser.error(f'缺少 Android SDK build-tools 37.0.0: {tool}')
    command = ['bash', str(ROOT / 'gradlew'), ':app:assembleRelease', '-Ppkg=' + PACKAGE]
    local_build = ROOT / '.local-build'
    if (local_build / 'plugin.init.gradle').is_file() and (local_build / 'output.init.gradle').is_file():
        env['MIUIX_SOURCE'] = str(local_build / 'miuix')
        command.extend(['-I', str(local_build / 'plugin.init.gradle'), '-I',
                        str(local_build / 'output.init.gradle'), '--offline'])
        apk_dir = local_build / 'output/_app/outputs/apk/release'
    else:
        apk_dir = ROOT / 'app/build/outputs/apk/release'
    if args.version:
        command.append('-PreleaseVersion=' + args.version)
    run(command, env, cwd=ROOT)
    metadata = json.loads((apk_dir / 'output-metadata.json').read_text())
    if metadata['applicationId'] != PACKAGE or len(metadata['elements']) != 1:
        parser.error('构建输出包名或 APK 数量不符合预期')
    source = apk_dir / metadata['elements'][0]['outputFile']
    verify = run([str(sdk_tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(source)],
                 env, capture_output=True, text=True).stdout
    validate_signature(verify)
    package_info = run([str(sdk_tools / 'aapt2'), 'dump', 'badging', str(source)],
                       env, capture_output=True, text=True).stdout
    if not package_info.startswith("package: name='" + PACKAGE + "'"):
        parser.error('APK 实际包名不符合预期')
    output = args.output.expanduser().resolve()
    output.mkdir(parents=True, exist_ok=True)
    target = output / 'bdFun-official-release.apk'
    shutil.copy2(source, target)
    (output / 'signature-verification.txt').write_text(verify)
    (output / 'SHA256SUMS.txt').write_text(hashlib.sha256(target.read_bytes()).hexdigest() + '  ' + target.name + '\n')
    print(f'官方 release 已验证：{target}\n包名：{PACKAGE}\n签名：V2\n证书 SHA-256：{FINGERPRINT}')


if __name__ == '__main__':
    try:
        main()
    except (subprocess.CalledProcessError, ValueError):
        # 不输出子进程命令或环境，以免诊断信息包含凭据。
        sys.exit('构建或签名验证失败；未交付 APK。请检查上方 Gradle 输出及本地凭据。')
