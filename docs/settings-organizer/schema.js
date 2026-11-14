/* Layout changes may not replace runtime component contracts. No external resources. */
(function(root){
'use strict';
const clone=x=>JSON.parse(JSON.stringify(x));
const stable=x=>JSON.stringify(normalize(x));
function normalize(x){return Array.isArray(x)?x.map(normalize):x&&typeof x==='object'?Object.fromEntries(Object.keys(x).sort().map(k=>[k,normalize(x[k])])):x;}
const flatten=categories=>categories.flatMap(c=>c.groups.flatMap(g=>g.settings));
function contract(setting){const copy=clone(setting);delete copy.title;delete copy.subtitle;return copy;}
function validate(candidate,baseline){
 if(!Array.isArray(candidate)||!candidate.length)throw Error('至少保留一个分类');
 if(candidate.length>100)throw Error('分类数量超过100');
 const routes=new Set(),seen=new Set(),original=new Map(flatten(baseline).map(s=>[s.pref_name,s]));
 const text=(value,label)=>{if(typeof value!=='string'||!value.trim()||value.length>200)throw Error(label+'不能为空或超过200字');};
 candidate.forEach(c=>{text(c.name,'分类名称');text(c.route,'分类路由');if(routes.has(c.route))throw Error('分类路由重复：'+c.route);routes.add(c.route);
  if(!['settings','about'].includes(c.layout||'settings'))throw Error('未知页面布局');
  if(c.icon!=null)throw Error('第一阶段不配置分类图标');
  if(!Array.isArray(c.groups)||c.groups.length>100)throw Error('分组格式不正确');
  c.groups.forEach(g=>{text(g.title,'分组标题');if(!Array.isArray(g.settings))throw Error('设置列表格式不正确');
   g.settings.forEach(s=>{text(s.title,'设置名称');if(s.subtitle!=null&&(typeof s.subtitle!=='string'||s.subtitle.length>1000))throw Error('说明格式不正确');
    const existing=original.get(s.pref_name);if(!existing)throw Error('未知设置：'+s.pref_name);if(seen.has(s.pref_name))throw Error('设置重复：'+s.pref_name);seen.add(s.pref_name);
    if(stable(contract(s))!==stable(contract(existing)))throw Error('不能改变组件类型、存储键、默认值、选项或显隐条件：'+s.pref_name);
   });
  });
 });
 if(seen.size!==original.size)throw Error('索引缺少设置：'+[...original.keys()].filter(k=>!seen.has(k)).join('、'));
 return clone(candidate);
}
root.SettingsSchema={clone,flatten,validate};
})(typeof window==='undefined'?globalThis:window);
