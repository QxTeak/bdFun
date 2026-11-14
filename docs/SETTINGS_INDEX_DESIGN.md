# SET-01 / SET-02 · 索引设置与管理者编排器

## 运行时结构

App 的唯一布局来源是 `app/src/main/assets/settings-index.json`。本轮以原生设置页完整清单重新建立索引，替换原型的36项示例；61个组件描述覆盖原全部设置、操作、开发者行，另含关于身份区与PR52的三个可选推荐组件。page_switches仍展开原来的六个页面开关。没有旧设置页兜底入口，原四个手写卡片文件已删除。

竖屏为分类列表与二级页；横屏且宽度至少600dp使用左侧分类、右侧内容。导航和搜索由页面作用域SettingsBrowserStateHolder持有，旋转不另建选择状态。返回先关闭搜索再回分类。搜索匹配名称/说明/分组/分类/键，排除当前不满足条件的设置，结果跳转并滚动定位。页面外壳、返回键、TopAppBar和大标题/滚动/材质联动继续使用原来的TeakActivityScaffold与TeakLargeTitle/TrackTeakPageScroll，不另画顶栏；二级页只在原工具栏增加搜索按钮。一级底部柔光胶囊展开、分裂关闭按钮并跟随IME抬升；动画复用主页搜索参数。分类内从右上搜索。

普通设置仍使用现有Teak/Miuix行：dropdown仍是玻璃dropdown（包括原面板宽度、开闭行为），字号仍用原SliderPreference和吸附/松手写入；主题/色彩的260ms延迟、动画反向开关、CDN动态选项、壁纸系统选择、开发者头像、退出登录的颜色与登录/debug显隐保持。第一阶段不展示分类图标；返回/搜索/关闭仅使用Miuix Icons。

## JSON 合同

顶层SettingsCategory数组：name / route / groups，layout可为settings（默认）或about；icon预留但本阶段不使用。Group为title / settings。

Setting保留pref_name / type / title / subtitle / default；type现在表示组件：switch、dropdown、cdn_dropdown、font_scale、page_switches、mine_switch、action、developer、version、app_identity、extension。旧boolean/select类型仍可解码。dropdown补充options:[{title,value}]，使用真实存储值而非数组下标。浮点组件带value_type:decimal，保证JSON序列化1.0→1仍按Float读取。inverted保留减弱动效的反向展示。

storage为settings（原偏好文件）、mine_layout（独立我的页存储）或none（操作/只读/扩展组件）。只有settings键由IndexedSettingsStateHolder经过原分组控制器写入；动作标识不是数据库键，不按动作id创建偏好。mine_switch继续使用MineLayoutStateHolder。导入只读布局，不重置用户偏好。default供显式重置与合同校验，现有仓储默认值兼容保留。

visible_when可为debug、logged_in、布尔偏好键，或extension:<组件标识>。推荐开关/清除/错误行通过可选Composable端口注入，PR55可独立编译；与PR52集成后接原优化器，仍保持默认关闭与原动作语义。没有从展示层构造数据实现。

索引验证拒绝重复键/路由、未知类型、非法默认值、混合枚举值类型与无效显隐条件。JSON解码在infra/storage，模型在core/model，验证与搜索在core/domain，状态/导航在feature/settings/application，Android装配在SettingsStore和路由壳。

## 管理者工具

浏览器直接打开`docs/settings-organizer/index.html`，无构建步骤、无服务端、无外部资源。支持：

- 分类/分组/设置拖动、上移下移、跨分类或分组移动；添加/删除空分类与空分组。
- 分类名称/路由、分组标题、设置标题/说明编辑；完整组件目录可检索。
- 实时横竖屏与深浅色预览，dropdown/开关/字号可操作，预览不执行外部动作；登录/debug可切换。
- 完整JSON导入导出、本地草稿、100步撤销/重做及键盘快捷键。

导入必须包含基准全部组件且每项仅出现一次；类型、存储键、默认值、选项、显隐和值类型合同不可更改。非法导入不替换当前布局。导出替换App资产并重新构建生效。每次更新App索引运行`python3 tools/sync-settings-catalog.py`同步离线目录；`node tools/test-settings-organizer.cjs`验证同步与合同。原型旧路径转向完整索引预览，避免两份索引漂移。

## 关于页

layout=about使用Deadliner实际关于页的身份区、三层浮动光晕与320dp滚动淡出，卡片只有背景从0.5到1变化，文字对比度不变。完整来源、适配差异及MIT许可见`SETTINGS_DEADLINER_PROVENANCE.md`。不用虚假的KMP标签，bdFun应用图标和开发者头像属于原产品身份素材。

## 验证与发布

纯JVM测试覆盖旧键写入、旧值兼容、非法更新、页面返回/搜索/显隐以及JSON完整索引/浮点往返。Node测试覆盖编排器合同、目录同步与失败隔离；Chrome实测重命名、撤销、跨组移动、拖动排序、非法导入、导出和实时预览搜索。模拟器检查分类、dropdown、全量多行、搜索/IME、横屏与关于页。

继续更新PR55。release使用专门集成分支合入PR52和PR54，不把它们的业务提交带入设置PR。完整播放器、登录后的真实账号服务及所有设备/字体组合的长期回归仍由装机测试完成。
