/* 一级入口只引用二级route；不能复制设置合同或包含root_footer。 */
(function(root){
'use strict';
const clone=x=>JSON.parse(JSON.stringify(x)),icons=['Background','Play','Messages','Home','ContactsCircle','Info','Settings'];
function validate(candidate,categories){
 if(!Array.isArray(candidate)||!candidate.length||candidate.length>100)throw Error('一级分组数量不正确');
 const expected=new Set(categories.filter(c=>c.layout!=='root_footer').map(c=>c.route)),seen=new Set(),ids=new Set();
 candidate.forEach(group=>{
  if(typeof group.id!=='string'||!(/^[a-z][a-z0-9_]*$/).test(group.id)||ids.has(group.id))throw Error('一级分组ID为空、非法或重复');ids.add(group.id);
  if(typeof group.title!=='string'||!group.title.trim()||!Array.isArray(group.entries)||!group.entries.length)throw Error('一级分组名称或入口列表不正确');
  group.entries.forEach(entry=>{
   if(!expected.has(entry.route)||seen.has(entry.route))throw Error('一级入口未知、重复或包含退出登录：'+entry.route);seen.add(entry.route);
   if(!icons.includes(entry.icon))throw Error('请选择已支持的MIUIX图标');
   if(typeof entry.color!=='string'||!/^#[0-9a-fA-F]{6}$/.test(entry.color))throw Error('图标底色必须为RGB颜色');
  });
 });
 if(seen.size!==expected.size)throw Error('一级索引缺少二级页面');return clone(candidate);
}
function reconcile(candidate,categories){
 const routes=new Set(categories.filter(c=>c.layout!=='root_footer').map(c=>c.route)),seen=new Set();
 const groups=clone(candidate).map(g=>({...g,entries:g.entries.filter(e=>routes.has(e.route)&&!seen.has(e.route)&&seen.add(e.route))})).filter(g=>g.entries.length);
 categories.filter(c=>routes.has(c.route)&&!seen.has(c.route)).forEach(c=>{
  if(!groups.length)groups.push({id:'general',title:'通用',entries:[]});
  groups[groups.length-1].entries.push({route:c.route,icon:'Settings',color:'#778795'});
 });
 return groups;
}
root.SettingsSeperation={validate,reconcile,icons};
})(globalThis);
