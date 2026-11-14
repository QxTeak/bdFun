# bdFun 设置编排器

直接用浏览器打开 `index.html`，也可以在此目录运行 `python3 -m http.server 8793` 后访问本机页面。所有脚本、预览和素材都在目录内，不需要联网。

左栏选择或拖动分类、分组与设置；中栏编辑名称、说明和所属分组；右栏实时预览。也可使用上移/下移按钮完成排序。支持本地草稿、撤销/重做、完整索引导入导出。

预览仅用于检查组织结构，不会修改手机偏好或执行退出登录、打开外链等真实动作。手机界面使用现有 Miuix/Teak 组件：select 仍为 dropdown，字号仍为滑条，返回键和 TopAppBar 沿用原壳。

导出的 `settings-index.json` 替换仓库 `app/src/main/assets/settings-index.json` 后重新构建生效。再运行 `python3 tools/sync-settings-catalog.py` 同步此工具的目录数据。请保留完整组件清单和原组件/存储/默认值/选项/显隐合同；第一阶段不配置分类图标。推荐扩展在合入 PR52 的构建中生效。
