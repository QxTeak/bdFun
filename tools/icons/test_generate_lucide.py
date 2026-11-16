import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
from generate_lucide import ANDROID, ROOT, STROKE, drawable, shape_path

A = '{' + ANDROID + '}'


class LucideConversionTest(unittest.TestCase):
    def test_inherited_none_fill_does_not_turn_outline_into_solid(self):
        icon = {'id': 'lucide:test', 'width':24, 'height':24, 'variant':'Outline', 'body': '<g fill="none" stroke="currentColor" '
                'stroke-linecap="round"><circle cx="12" cy="12" r="10"/></g>'}
        path = ET.fromstring(drawable(icon)).find('path')
        self.assertNotIn(A + 'fillColor', path.attrib)
        self.assertEqual(path.get(A + 'strokeWidth'), str(STROKE))
        self.assertEqual(path.get(A + 'strokeLineCap'), 'round')
        self.assertIn('a10.0,10.0', path.get(A + 'pathData'))

    def test_filled_evenodd_and_optical_inset_survive(self):
        icon = {'id': 'lucide-extended:test-filled', 'width':24, 'height':24, 'variant':'Filled',
                'body': '<path fill="currentColor" fill-rule="evenodd" d="M0 0h24v24Z"/>'}
        root = ET.fromstring(drawable(icon))
        path = root.find('group/path')
        self.assertEqual(path.get(A + 'fillType'), 'evenOdd')
        self.assertEqual(root.find('group').get(A + 'scaleX'), '0.92')
        self.assertNotIn(A + 'strokeWidth', path.attrib)

    def test_rectangle_keeps_rounded_corners(self):
        path = shape_path(ET.fromstring('<rect x="2" y="3" width="20" height="16" rx="2"/>'))
        self.assertEqual(path.count('A2.0,2.0'), 4)
        self.assertTrue(path.endswith('Z'))

    def test_unsupported_transform_fails_instead_of_corrupting_icon(self):
        with self.assertRaises(ValueError):
            drawable({'id': 'lucide:test', 'width':24, 'height':24, 'variant':'Outline', 'body': '<g transform="rotate(90)"><path d="M0 0"/></g>'})

    def test_selected_resources_cover_preset_exactly(self):
        preset = json.loads((ROOT / 'docs/icon-presets/issue-16.json').read_text())
        selected = json.loads((ROOT / 'tools/icons/lucide-selected.json').read_text())['icons']
        expected = {m['replacement']['id'] for m in preset['mappings']
                    if m['mode'] == 'replace' and m['replacement']['family'] != 'Miuix'}
        self.assertEqual({i['id'] for i in selected}, expected)
        for icon in selected:
            path = ROOT / 'TeakUI/src/main/res/drawable' / ('hybrid_' + icon['name'].replace('-', '_') + '.xml')
            self.assertEqual(path.read_text(), drawable(icon))


if __name__ == '__main__':
    unittest.main()
