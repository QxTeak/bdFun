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
    value => value.find(c => c.layout === 'root_footer').layout = 'settings',
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
for (const file of ['seperation-catalog.js', 'seperation.js']) {
    vm.runInContext(fs.readFileSync(path.join(root, 'docs/settings-organizer', file), 'utf8'), context);
}
const seperation = context.SETTINGS_SEPERATION;
const rootIndex = JSON.parse(fs.readFileSync(path.join(root, 'app/src/main/assets/settings-seperation.json'), 'utf8'));
assert.equal(JSON.stringify(seperation), JSON.stringify(rootIndex), '一级索引必须与App同步');
context.SettingsSeperation.validate(seperation, baseline);
for (const mutate of [
    value => value[0].entries.pop(),
    value => value[0].entries.push(clone(value[1].entries[0])),
    value => value[0].entries[0].route = 'settings/account',
    value => value[0].entries[0].icon = 'Unknown',
    value => value[0].entries[0].color = 'red',
    value => value[0].id = value[1].id,
]) {
    const invalid = clone(seperation); mutate(invalid);
    assert.throws(() => context.SettingsSeperation.validate(invalid, baseline));
}
const grouped = clone(seperation);
grouped[1].entries.push(grouped[0].entries.pop());grouped.shift();
context.SettingsSeperation.validate(grouped, baseline);
const extended = clone(baseline);extended.push({name:'新页面',route:'settings/new',groups:[]});
context.SettingsSeperation.validate(context.SettingsSeperation.reconcile(seperation, extended), extended);
for (const group of seperation) for (const entry of group.entries) {
    const svg=fs.readFileSync(path.join(root,'docs/settings-organizer/assets/miuix',entry.icon+'.svg'),'utf8');
    assert(svg.includes('<path d="'));assert(svg.includes('Apache-2.0'));
}
console.log('PASS: seperation catalog sync, regrouping, route coverage, footer exclusion, Miuix icon/color guards and SVG assets');
