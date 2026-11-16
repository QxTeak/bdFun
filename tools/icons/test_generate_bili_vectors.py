"""原生图标生成器的资源一致性与有孔轮廓保护。"""
import importlib.util
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('generator', Path(__file__).with_name('generate_bili_vectors.py'))
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)

class NativeVectorsTest(unittest.TestCase):
    def test_all_original_resources_are_vectors_and_match_fixed_paths(self):
        icons = json.loads((ROOT / 'tools/icons/bili-vectors.json').read_text())['icons']
        self.assertEqual(len(icons), 21)
        for icon in icons:
            with self.subTest(icon=icon['name']):
                xml = generator.render(icon)
                self.assertEqual(xml, (ROOT / 'app/src/main/res/drawable' / (icon['name'] + '.xml')).read_text())
                self.assertFalse((ROOT / 'app/src/main/res/drawable-nodpi' / (icon['name'] + '.png')).exists())
                root = ET.fromstring(xml)
                paths = list(root.iter('path'))
                self.assertEqual(len(paths), len(icon['paths']))
                for path in paths:
                    self.assertEqual(path.get('{http://schemas.android.com/apk/res/android}fillType'), 'nonZero')
                self.assertGreater(icon['silhouetteIoU'], .90)

    def test_unsupported_transform_is_rejected(self):
        with self.assertRaises(ValueError):
            generator.render(dict(width=24, height=24, paths=[dict(d='M0 0Z', transform='scale(2)')]))

    def test_danmaku_enabled_keeps_colored_mark(self):
        icons = json.loads((ROOT / 'tools/icons/bili-vectors.json').read_text())['icons']
        icon = next(x for x in icons if x['name'] == 'fs_danmaku_on')
        self.assertIn('#FFFF6699', generator.render(icon))

if __name__ == '__main__':
    unittest.main()
