const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const context = vm.createContext({});
for (const file of ['catalog.js', 'schema.js']) {
    let source = fs.readFileSync(path.join(root, 'docs/settings-organizer', file), 'utf8');
    if (file === 'catalog.js') source = source.replace('window.SETTINGS_CATALOG', 'globalThis.SETTINGS_CATALOG');
    vm.runInContext(source, context);
}
const { clone, validate, flatten } = context.SettingsSchema;
const baseline = context.SETTINGS_CATALOG;
const app = JSON.parse(fs.readFileSync(path.join(root, 'app/src/main/assets/settings-index.json'), 'utf8'));
assert.equal(JSON.stringify(baseline), JSON.stringify(app), '编排器必须使用实际 App 索引');
const changed = clone(baseline);
const entry = changed[0].groups[0].settings.shift();
changed[1].groups[0].settings.push(entry);
changed[1].name = '自定义播放分类';
entry.title = '自定义标题';
validate(changed, baseline);
for (const mutate of [
    value => value[0].groups[0].settings.pop(),
    value => value[0].groups[0].settings.push(clone(value[0].groups[0].settings[0])),
    value => value[0].groups[0].settings[0].type = 'action',
    value => value[0].groups[0].settings[0].default = 999,
    value => value[0].groups[0].settings[0].visible_when = 'debug',
    value => value[0].route = value[1].route,
    value => value[0].icon = 'custom-icon',
]) {
    const invalid = clone(baseline);
    mutate(invalid);
    assert.throws(() => validate(invalid, baseline));
}
const reordered = clone(baseline);
const original = reordered[0].groups[0].settings[0];
reordered[0].groups[0].settings[0] = Object.fromEntries(Object.entries(original).reverse());
validate(reordered, baseline);
const snapshot = JSON.stringify(baseline);
const missing = clone(baseline);
missing[0].groups[0].settings.pop();
assert.throws(() => validate(missing, baseline));
assert.equal(JSON.stringify(baseline), snapshot, '失败校验不能更改输入基准');
console.log(`PASS: ${flatten(baseline).length} components, full catalog sync, reorganization, contract guards, property order and failed import isolation`);
