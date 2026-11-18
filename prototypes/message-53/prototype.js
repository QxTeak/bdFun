const $ = selector => document.querySelector(selector);
const pages = ['消息中心', '回复与@', '收到喜欢', '系统通知', '新增粉丝', '联系人',
  '私信会话', '聊天设置', '消息设置', '通知设置子页', '黑名单', '消息屏蔽词', '点赞详情', '二级私信列表'];
const parents = {'聊天设置':'私信会话', '点赞详情':'收到喜欢', '通知设置子页':'回复与@',
  '消息屏蔽词':'消息设置', '黑名单':'消息设置'};
const people = ['木木的放映室', '山间来信', '映画研究所', '小岛同学', '哔哩哔哩漫画'];
const snippets = ['这个镜头的光线真的很漂亮。', '[图片]', '[视频] 把日落装进镜头里', '这条消息已撤回', '你关注的作品更新啦'];
const feeds = ['回复与@', '收到喜欢', '系统通知', '新增粉丝'];
let stack = ['消息中心'];
let profile = 0;
let tab = 0;
let contactsTab = 0;
let query = '';
let followedOnly = false;
let dark = false;
let emotes = false;
let pendingImage = false;
let inbox = people.map((_, id) => id);
let blocked = [3];
let keywords = ['推广', '加群'];
let unread = [3, 2, 0, 1, 0];
let followBack = new Set();
const preferences = people.map((_, i) => ({pin:i === 0, mute:false, block:i === 3, push:true}));
const deleted = new Set();
const mutedNotices = new Set();
const sent = people.map(() => []);
let withdrawn = new Set();
let settingsTitle = '回复我的';
const noticePreferences = {
  '回复我的': {enabled:true, receiver:0},
  '@我': {enabled:true, receiver:0},
  '收到喜欢': {enabled:true, receiver:0},
};
const modal = $('#modal');

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, ch => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[ch]));
}
const page = () => stack.at(-1);
const state = () => $('#state').value;
const button = (label, action, cls = '') => `<button class="${cls}" onclick="${action}">${label}</button>`;
const avatar = id => `<span class="avatar" aria-hidden="true">${people[id][0]}</span>`;
const status = text => `<span class="sub">${text}</span>`;
const group = items => `<div class="group">${items}</div>`;
const row = (label, action) => button(`<span>${label}</span>›`, action, 'setting link full');
function toast(text) {
  $('.toast')?.remove();
  const message = document.createElement('div');
  message.className = 'toast';
  message.textContent = text;
  document.body.append(message);
  setTimeout(() => message.remove(), 1900);
}
function go(destination) {
  if (destination !== page()) stack.push(destination);
  render(true);
}
function back() {
  if (stack.length > 1) stack.pop();
  render(true);
}
function jump(destination) {
  stack = ['消息中心'];
  if (parents[destination]) stack.push(parents[destination]);
  if (destination !== '消息中心') stack.push(destination);
  render(true);
}
function openChat(id) {
  if (profile !== id) {
    $('#draft').value = '';
    pendingImage = false;
    emotes = false;
  }
  profile = id;
  unread[id] = 0;
  go('私信会话');
}
function retry() {
  $('#state').value = '正常';
  render();
}
function menu(title, actions, description = '') {
  $('#modalTitle').textContent = title;
  $('#modalBody').innerHTML = description ? `<p>${escapeHtml(description)}</p>` : '';
  for (const [label, action] of actions) {
    const item = document.createElement('button');
    item.textContent = label;
    item.onclick = () => { modal.close(); action(); };
    $('#modalBody').append(item);
  }
  modal.showModal();
}
function markAll() {
  menu('一键已读', [['确认标为已读', () => { unread.fill(0); render(); }]], '是否清除全部新消息提醒？');
}
function openInboxMenu() {
  menu('消息', [['刷新消息', retry], ['消息设置', () => go('消息设置')]]);
}
function sessionMenu(id) {
  const pref = preferences[id];
  menu(people[id], [
    [pref.pin ? '取消置顶' : '置顶会话', () => { pref.pin = !pref.pin; render(); }],
    [pref.mute ? '关闭免打扰' : '消息免打扰', () => { pref.mute = !pref.mute; render(); }],
    ['标为已读', () => { unread[id] = 0; render(); }],
    ['删除会话', () => menu('删除会话', [['确认删除会话', () => {
      inbox = inbox.filter(item => item !== id); render();
    }]], '删除会话后，可通过对方的个人空间重新发起私信。')],
  ]);
}
function filter(value) { followedOnly = value; render(); }
function inboxView() {
  const ids = inbox.filter(id => !followedOnly || id < 3)
    .sort((a, b) => Number(preferences[b].pin) - Number(preferences[a].pin));
  return ids.map(id => `<button class="row ${stack.includes('私信会话') && id === profile ? 'selected' : ''}"
    onclick="openChat(${id})" oncontextmenu="event.preventDefault();sessionMenu(${id})">
    ${avatar(id)}<span class="info"><span class="name">${people[id]} ${preferences[id].pin ? status('置顶') : ''}</span>
    <span class="preview">${snippets[id]}</span></span><span class="meta">${['刚刚','12:32','昨天','昨天','周一'][id]}
    ${unread[id] ? `<b class="badge">${unread[id]}</b>` : ''}</span></button>`).join('');
}
function feedMenu(key, like, anchor) {
  const actions = [['删除通知', () => menu('删除通知？', [['删除', () => { deleted.add(key); render(); }]],
    like ? '有新的点赞时，这条通知会重新出现在列表。' : '确认删除这条通知？')]];
  if (like) actions.unshift([mutedNotices.has(key) ? '接收通知' : '不再通知', () => {
    if (mutedNotices.delete(key)) return render();
    menu('不再通知', [['确认不再通知', () => { mutedNotices.add(key); render(); }]], '仍可在列表中查看。');
  }]);
  const dropdown = $('#cardMenu');
  dropdown.replaceChildren();
  for (const [label, action] of actions) {
    const item = document.createElement('button');
    item.textContent = label === '删除通知' ? '删除' : label;
    item.onclick = () => { dropdown.hidePopover(); action(); };
    dropdown.append(item);
  }
  const bounds = anchor.getBoundingClientRect();
  dropdown.showPopover();
  dropdown.style.left = Math.max(8, bounds.right - dropdown.offsetWidth) + 'px';
  dropdown.style.top = Math.min(bounds.bottom, innerHeight - dropdown.offsetHeight - 8) + 'px';
}
function cardMenuAttrs(key, like = false) {
  return `data-feed-key="${key}" data-like="${like}" tabindex="0"`;
}
function replyRow(id, title, body, quote, subject) {
  const key = `reply-${tab}-${id}`;
  if (deleted.has(key)) return '';
  return `<article class="feed" ${cardMenuAttrs(key)}>${avatar(id)}<div class="feed-body">
    <div class="feed-head"><b>${title}</b></div>
    <div class="feed-click" role="link" tabindex="0" onclick="openSource(${id})" onkeydown="if(event.key==='Enter')openSource(${id})">
    <p>${body}</p>${quote ? `<div class="quote">${quote}</div>` : ''}
    ${subject ? `<div class="quote">${subject}</div>` : ''}${status('今天 09:41')}</div></div></article>`;
}
function openSource(id) {
  toast(['打开视频评论定位','打开动态评论','动态已删除或暂不可用'][id]);
}
function selectTab(value) { tab = value; render(true); }
function replyTabs() {
  const tabs = ['回复我的', '@我'].map((label, i) =>
    button(label, `selectTab(${i})`, i === tab ? 'active' : '')).join('');
  return `<div class="page-tools">${tabs}</div>`;
}
function replyView() {
  return replyTabs() + group(tab === 0
    ? replyRow(0, '木木 回复了我的评论', '这个镜头在海边拍的吗？', '我：下午四点的光线最好看。', '根评论：记录一下这条海岸线。')
      + replyRow(2, '映画研究所 回复了我的动态', '期待下一次更新！', '', '')
    : replyRow(1, '山间来信 在评论中 @了我', '@我 你上次找的机位在这里。', '', '原评论：海边观景台怎么走？'));
}
function likeView() {
  const sections = ['最新', '累计'].map((title, i) => {
    const key = 'like-' + i;
    if (deleted.has(key)) return '';
    return `<div class="sectionlabel">${title}</div>` + group(`<article class="feed" ${cardMenuAttrs(key, true)}>${avatar(i)}<div class="feed-body">
      <div class="feed-head"><b>${i === 0 ? '木木等 24 人赞了我的评论' : '山间来信赞了我的视频'}</b>
      </div>
      <div role="link" class="feed-click" tabindex="0" onclick="${i === 0 ? "go('点赞详情')" : 'openSource(0)'}"
      onkeydown="if(event.key==='Enter')this.click()"><p>${i === 0 ? '下午四点的光线最好看。' : '把日落装进镜头里'}</p>
      ${status('今天 08:16' + (mutedNotices.has(key) ? ' · 已关闭通知' : ''))}</div>
      </div></article>`);
  });
  return sections.join('');
}
function systemView() {
  return group(['稿件审核通过', '账号安全提醒'].map((title, i) => {
    const key = 'system-' + i;
    return deleted.has(key) ? '' : `<article class="feed" ${cardMenuAttrs(key)}><div class="feed-body"><div class="feed-head">
      <b>${title}</b></div>
      <p>${i === 0 ? '你的稿件「把日落装进镜头里」已通过审核。' : '请定期检查你的登录设备。'}</p>
      <p>${button('查看详情 ↗', "toast('打开通知内链接')", 'link')}</p>${status('哔哩哔哩 · 今天')}</div></article>`;
  }).join(''));
}
function personRow(id, action, extra = '', subtitle = '') {
  return `<div class="setting">${avatar(id)}<button class="person" onclick="${action}"><b>${people[id]}</b>${subtitle ? '<br>'+status(subtitle) : ''}</button>${extra}</div>`;
}
function switchContact(value) { contactsTab = value; query = ''; render(true); }
function searchContact(event) { event.preventDefault(); query = $('#contactQuery').value.trim(); render(); }
function contactsView() {
  const filters = `<div class="page-tools">${['我的关注','我的粉丝'].map((label,i) => button(label,`switchContact(${i})`,contactsTab === i ? 'active' : '')).join('')}</div>`;
  const search = contactsTab === 0 ? `<form class="page-tools" onsubmit="searchContact(event)">
     <input class="search" id="contactQuery" placeholder="搜索关注的人" value="${escapeHtml(query)}"><button class="link">搜索</button></form>` : '';
  const ids = contactsTab === 0 ? [0,1,2].filter(id => people[id].includes(query)) : [3,1];
  return filters + search + (ids.length ? group(ids.map(id => personRow(id, `openChat(${id})`)).join('')) : empty('暂无联系人'));
}
function follow(id) {
  if (!followBack.delete(id)) followBack.add(id);
  render();
}
function toggle(key) {
  if (state() === '设置保存失败') return toast('保存失败，请重试');
  const pref = preferences[profile];
  const commit = () => { pref[key] = !pref[key]; render(); };
  if (key === 'block' && !pref.block) return menu('确认设置', [['确认', commit]], '拉黑将解除关注和合集订阅，禁止该用户与你互动或查看你的空间。');
  if (key === 'push' && pref.push) return menu('确认设置', [['确认', commit]], '关闭后不再收到该账号的图文与稿件推送，通知类消息不受影响。');
  commit();
}
function switchRow(label, value, action) {
  return `<div class="setting"><span>${label}</span><button role="switch" aria-label="${label}" aria-checked="${value}"
    class="switch ${value ? 'on' : ''}" onclick="${action}"></button></div>`;
}
function chatSettings() {
  const pref = preferences[profile];
  const pin = state() === '置顶读取失败 1217'
    ? '<div class="setting"><span>置顶聊天<br><span class="sub">状态未知 · 1217 (1217)</span></span><button class="link" onclick="retry()">重试</button></div>'
    : switchRow('置顶聊天', pref.pin, "toggle('pin')");
  return group(personRow(profile, "toast('打开个人空间')")) + group(pin
    + switchRow('消息免打扰', pref.mute, "toggle('mute')")
    + switchRow('加入黑名单', pref.block, "toggle('block')")
    + (profile === 4 ? switchRow('内容推送', pref.push, "toggle('push')") : ''));
}
function openNoticeSettings(title = page() === '收到喜欢' ? '收到喜欢' : tab === 1 ? '@我' : '回复我的') {
  settingsTitle = title;
  go('通知设置子页');
}
function messageSettings() {
  const notices = Object.keys(noticePreferences).map(label => row(label, `openNoticeSettings('${label}')`)).join('');
  return group(notices + row('黑名单', "go('黑名单')") + row('消息屏蔽词', "go('消息屏蔽词')"));
}
function setNotice(key, value) {
  noticePreferences[settingsTitle][key] = value;
  render();
}
function subSettings() {
  const pref = noticePreferences[settingsTitle];
  return group(switchRow('消息提醒', pref.enabled, `setNotice('enabled',${!pref.enabled})`)
    + ['所有人', '我关注的人'].map((label, i) => row(`${i === pref.receiver ? '✓ ' : ''}${label}`,
      `setNotice('receiver',${i})`)).join(''));
}
function addWord(event) {
  event.preventDefault();
  const value = $('#word').value.trim();
  if (!value || keywords.includes(value) || keywords.length >= 20) return toast('请输入有效且未添加的屏蔽词');
  keywords.push(value); render();
}
function keywordView() {
  return `<div class="sectionlabel">${keywords.length} / 20</div>
    <form class="page-tools" onsubmit="addWord(event)"><input class="search" id="word" placeholder="输入消息屏蔽词"><button class="link">添加</button></form>`
    + group(keywords.map((word,i) => `<div class="setting"><span>${escapeHtml(word)}</span>${button('删除', `deleteWord(${i})`, 'link')}</div>`).join(''));
}
function deleteWord(i) {
  menu('删除屏蔽词', [['确认删除', () => { keywords.splice(i,1); render(); }]], keywords[i]);
}
function removeBlock(id) { blocked = blocked.filter(value => id !== value); render(); }
function toggleEmotes() { emotes = !emotes; render(); }
function addEmote(text) { $('#draft').value += text; $('#draft').focus(); }
function attach() {
  if (state() === '图片上传失败') return toast('图片上传失败，草稿已保留');
  pendingImage = true;
  render();
}
function messageMenu(id, mine) {
  if (mine) menu('消息操作', [['撤回', () => { withdrawn.add(profile+':'+id); render(); }]]);
  else menu('消息操作', [['举报', reportMenu]]);
}
function reportMenu() {
  const choices = ['色情低俗','政治敏感','违法有害','广告骚扰','人身攻击','诈骗','其他问题'];
  menu('举报消息', choices.map(reason => [reason, () => {
    if (reason === '其他问题') {
      menu('补充说明', [], '选择“其他问题”时必填');
      $('#modalBody').insertAdjacentHTML('beforeend', '<form onsubmit="event.preventDefault();modal.close();toast(\'已提交举报\')"><input class="search" required placeholder="补充说明"><button>提交</button></form>');
    } else toast('已提交举报');
  }]));
}
function bubble(id, body, mine = false) {
  if (withdrawn.has(profile+':'+id)) return '<div class="time">你撤回了一条消息</div>';
  return `<div class="msg ${mine ? 'mine' : ''}"><div class="bubble ${mine ? 'mine' : ''}" oncontextmenu="event.preventDefault();messageMenu(${id},${mine})">${body}</div></div>`;
}
function chatView() {
  return '<div class="time">今天 09:32</div>'
    + bubble(0,'最近在整理旅行时拍下来的片段，分享给你看看。')
    + bubble(1,'<button class="cover" onclick="openSource(0)">▷</button><b>把日落装进镜头里</b><br>' + status('视频分享 · 06:24'))
    + bubble(2, '这个镜头的光线真的很漂亮。', true)
    + bubble(3,'<button class="cover" onclick="menu(\'图片预览\',[],\'聊天图片示例\')">▧</button>')
    + '<div class="time">对方撤回了一条消息</div>'
    + bubble(4, status('暂不支持的消息类型'))
    + sent[profile].map((message, i) => bubble(i+5, escapeHtml(message), true)).join('');
}
function empty(title, action = '') {
  return `<div class="empty"><h2>${title}</h2>${action ? button(action,'retry()','link') : ''}</div>`;
}
const views = {
  '回复与@': replyView, '收到喜欢': likeView, '系统通知': systemView,
  '私信会话': chatView, '聊天设置': chatSettings, '联系人': contactsView,
  '消息设置': messageSettings, '通知设置子页': subSettings, '消息屏蔽词': keywordView,
  '新增粉丝': () => group([3,1].map(id => personRow(id, "toast('打开个人空间')",
    button(followBack.has(id) ? '已关注' : '+ 回关', `follow(${id})`, followBack.has(id) ? '' : 'primary'), '关注了你 · 今天')).join('')),
  '点赞详情': () => group(row('下午四点的光线最好看。', 'openSource(0)')
    + [0,1,2].map(id => personRow(id, "toast('打开个人空间')", '', '赞了我 · 今天')).join('')),
  '黑名单': () => blocked.length ? group(blocked.map(id => personRow(id, "toast('打开个人空间')",
    button('移除', `removeBlock(${id})`, 'link'))).join('')) : empty('暂无黑名单用户'),
  '二级私信列表': () => inboxView(),
  '消息中心': () => empty('选择一条消息'),
};
function withState(content) {
  switch (state()) {
    case '加载中': return '<div aria-label="加载中">' + '<div class="skeleton"></div>'.repeat(4) + '</div>';
    case '空状态': return empty('暂无消息');
    case '加载失败': return empty('加载失败', '重试');
    case '未登录': return empty('登录后查看消息', '登录');
    case '登录失效': return empty('登录已失效', '重新登录');
    case '刷新失败保留数据': return '<div class="notice">刷新失败 · 正在显示上次内容 ' + button('重试','retry()') + '</div>' + content;
    case '加载更多失败': return content + '<div class="footer">' + button('加载更多失败 · 重试','retry()') + '</div>';
    default: return content;
  }
}
function renderComposer() {
  const show = page() === '私信会话' && !['加载中','加载失败','未登录','登录失效'].includes(state());
  $('#composer').hidden = !show;
  const picker = emotes ? ['[微笑]', '[打call]', '[点赞]'].map(x => button(x,`addEmote('${x}')`)).join('') : '';
  const image = pendingImage ? '待发送图片 ' + button('移除','pendingImage=false;render()') : '';
  $('#extras').hidden = !show || !(picker || image);
  $('#extras').innerHTML = image + picker;
}
function render(resetScroll = false) {
  if ($('#cardMenu').matches(':popover-open')) $('#cardMenu').hidePopover();
  const destination = page();
  $('#device').className = ['device', $('#size').value, dark ? 'dark' : '',
    destination === '消息中心' ? 'nav-empty' : 'open'].join(' ');
  $('#case').value = destination;
  $('#count').textContent = '';
  $('#all').classList.toggle('active', !followedOnly);
  $('#follow').classList.toggle('active', followedOnly);
  $('#shortcuts').innerHTML = feeds.slice(0,3).map((label,i) => button(
    `<span class="glyph">${['☏','♡','▤'][i]}</span>${label}`, `tab=0;go('${label}')`, 'shortcut')).join('');
  $('#sessions').innerHTML = destination === '消息中心' ? withState(inboxView()) : inboxView();
  $('#title').textContent = destination === '私信会话' ? people[profile] : destination === '通知设置子页' ? settingsTitle : destination;
  const gear = $('#gear');
  gear.hidden = !['私信会话', '回复与@', '收到喜欢'].includes(destination);
  gear.onclick = destination === '私信会话' ? () => go('聊天设置') : () => openNoticeSettings();
  const content = views[destination]();
  $('#content').innerHTML = destination === '消息中心' ? content : withState(content);
  if (resetScroll) $('#content').scrollTop = destination === '私信会话' ? $('#content').scrollHeight : 0;
  renderComposer();
}
$('#case').innerHTML = pages.map(label => `<option>${label}</option>`).join('');
$('#case').onchange = event => { tab = 0; jump(event.target.value); };
$('#size').onchange = () => render();
$('#theme').onclick = () => { dark = !dark; render(); };
$('#state').onchange = () => {
  if (state().includes('置顶') || state().includes('设置保存')) jump('聊天设置');
  else if (/图片上传|文字发送/.test(state())) jump('私信会话');
  else render();
};
$('#composer').onsubmit = event => {
  event.preventDefault();
  const input = $('#draft');
  const message = input.value.trim();
  if (pendingImage) { sent[profile].push('[图片]'); pendingImage = false; }
  if (message && state() === '文字发送失败') { render(); return toast('发送失败，草稿已保留'); }
  if (message) sent[profile].push(message);
  input.value = '';
  render(true);
};
render();

const refreshPages = new Set([
  '回复与@', '收到喜欢', '系统通知', '新增粉丝', '联系人', '聊天设置',
  '消息设置', '通知设置子页', '黑名单', '消息屏蔽词', '点赞详情', '二级私信列表',
]);
const refreshThreshold = 64;
const scrollPane = $('#content');
const pullIndicator = $('#pullIndicator');
let pullStart = null;
let refreshing = false;

function canPull() {
  return refreshPages.has(page()) && scrollPane.scrollTop <= 0
    && !['加载中', '未登录', '登录失效'].includes(state()) && !refreshing;
}
function startPull(x, y) {
  pullStart = canPull() ? {x, y, distance:0} : null;
}
function movePull(x, y) {
  if (!pullStart) return false;
  const distance = y - pullStart.y;
  if (distance < -5 || Math.abs(x - pullStart.x) > Math.max(12, distance)) {
    finishPull(false);
    return false;
  }
  pullStart.distance = distance;
  pullIndicator.hidden = distance < 10;
  pullIndicator.textContent = distance >= refreshThreshold ? '松开刷新' : '下拉刷新';
  return distance >= 10;
}
function finishPull(commit) {
  const trigger = commit && pullStart?.distance >= refreshThreshold && canPull();
  pullStart = null;
  if (trigger) refreshPage();
  else if (!refreshing) pullIndicator.hidden = true;
}
function refreshPage() {
  const destination = stack;
  const current = page();
  const scenario = state();
  refreshing = true;
  pullIndicator.hidden = false;
  pullIndicator.textContent = '正在刷新…';
  setTimeout(() => {
    refreshing = false;
    pullIndicator.hidden = true;
    if (destination !== stack || page() !== current || state() !== scenario) return;
    retry();
  }, 450);
}
scrollPane.addEventListener('touchstart', event => {
  if (event.touches.length === 1) startPull(event.touches[0].clientX, event.touches[0].clientY);
}, {passive:true});
scrollPane.addEventListener('touchmove', event => {
  const touch = event.touches[0];
  if (movePull(touch.clientX, touch.clientY) && event.cancelable) event.preventDefault();
}, {passive:false});
scrollPane.addEventListener('touchend', () => finishPull(true));
scrollPane.addEventListener('touchcancel', () => finishPull(false));
scrollPane.addEventListener('pointerdown', event => {
  if (event.pointerType === 'mouse' && event.button === 0 && !event.target.closest('button,input,a')) {
    startPull(event.clientX, event.clientY);
  }
});
window.addEventListener('pointermove', event => {
  if (event.pointerType === 'mouse' && movePull(event.clientX, event.clientY)) event.preventDefault();
});
window.addEventListener('pointerup', event => {
  if (event.pointerType === 'mouse') finishPull(true);
});
scrollPane.addEventListener('keydown', event => {
  if (event.target === scrollPane && event.key.toLowerCase() === 'r' && canPull()) refreshPage();
});


let cardPress;
let suppressCardClick = false;
function cancelCardPress() {
  if (cardPress) clearTimeout(cardPress.timer);
  cardPress = null;
}
function openCardMenu(card) {
  feedMenu(card.dataset.feedKey, card.dataset.like === 'true', card);
}
scrollPane.addEventListener('contextmenu', event => {
  const card = event.target.closest('[data-feed-key]');
  if (!card) return;
  event.preventDefault();
  cancelCardPress();
  if (!$('#cardMenu').matches(':popover-open')) openCardMenu(card);
});
scrollPane.addEventListener('pointerdown', event => {
  suppressCardClick = false;
  const card = event.target.closest('[data-feed-key]');
  if (!card || event.button !== 0) return;
  cardPress = {x:event.clientX, y:event.clientY, timer:setTimeout(() => {
    suppressCardClick = true;
    openCardMenu(card);
  }, 500)};
});
window.addEventListener('pointermove', event => {
  if (cardPress && Math.hypot(event.clientX-cardPress.x, event.clientY-cardPress.y) > 9) cancelCardPress();
});
window.addEventListener('pointerup', cancelCardPress);
window.addEventListener('pointercancel', cancelCardPress);
scrollPane.addEventListener('click', event => {
  if (!suppressCardClick) return;
  suppressCardClick = false;
  event.preventDefault();
  event.stopImmediatePropagation();
}, true);
scrollPane.addEventListener('keydown', event => {
  const card = event.target.closest('[data-feed-key]');
  if (card && (event.key === 'ContextMenu' || (event.shiftKey && event.key === 'F10'))) {
    event.preventDefault();
    openCardMenu(card);
  }
});
