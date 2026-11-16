import importlib.util
import os
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('agent', 'tools/release-notes-agent.py')
agent = importlib.util.module_from_spec(spec)
spec.loader.exec_module(agent)

class ReleaseNotesTest(unittest.TestCase):
    def test_manual_notes_never_call_api(self):
        with patch.dict(os.environ, {'RELEASE_NOTES': '- 新增 唱片控件'}, clear=True), patch.object(agent, 'git') as git:
            self.assertEqual('- 新增 唱片控件', agent.generate())
            git.assert_not_called()

    def test_missing_key_fails_explicitly(self):
        with patch.dict(os.environ, {'GENERATE_NOTES': 'true'}, clear=True), patch.object(agent, 'git', side_effect=['vpreview-5.0', '新增 更新检查']):
            with self.assertRaisesRegex(SystemExit, 'DEEPSEEK_API_KEY'):
                agent.generate()

    def test_empty_changes_are_not_invented(self):
        with patch.dict(os.environ, {'GENERATE_NOTES': 'true'}, clear=True), patch.object(agent, 'git', side_effect=['vpreview-5.0', '']):
            with self.assertRaisesRegex(SystemExit, '没有提交'):
                agent.generate()

if __name__ == '__main__':
    unittest.main()
