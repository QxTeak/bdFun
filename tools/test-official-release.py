import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('official_release', Path(__file__).with_name('build-official-release.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class SignatureGuardTest(unittest.TestCase):
    def report(self, schemes=('v2',), cert=None):
        return '\n'.join([f'Verified using {s} scheme (APK Signature Scheme): true' for s in schemes] +
                         ['Signer #1 certificate SHA-256 digest: ' + (cert or release.FINGERPRINT)])

    def test_official_v2(self):
        release.validate_signature(self.report())

    def test_sdk37_signer_format(self):
        release.validate_signature(self.report().replace('Signer #1 certificate', 'V2 Signer: certificate'))

    def test_wrong_certificate(self):
        with self.assertRaises(ValueError):
            release.validate_signature(self.report(cert='0' * 64))

    def test_missing_or_additional_schemes(self):
        for schemes in [(), ('v1',), ('v2', 'v3'), ('v2', 'v3.1'), ('v2', 'v4')]:
            with self.subTest(schemes=schemes), self.assertRaises(ValueError):
                release.validate_signature(self.report(schemes))

    def test_multiple_signers(self):
        with self.assertRaises(ValueError):
            release.validate_signature(self.report() + '\nSigner #2 certificate SHA-256 digest: ' + release.FINGERPRINT)


if __name__ == '__main__':
    unittest.main()
