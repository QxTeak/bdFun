#!/usr/bin/env python3
# 只生成协议字段表，不复制上游 UI；来源和固定版本见消息对齐文档。
from pathlib import Path
import re,ast,json
import argparse
args = argparse.ArgumentParser(description="Extract the pinned PiliPlus IM protobuf descriptors")
args.add_argument("source", type=Path, help="Hyper-PiliPlus repository root at 0d08dd25")
root = args.parse_args().source / "lib/grpc/bilibili"
schema={}
for path in root.rglob('*.pbjson.dart'):
 dart=path.with_name(path.name.replace('.pbjson.dart','.pb.dart'))
 if not dart.exists():continue
 match=re.search(r"PackageName\([^)]*'(bilibili[^']+)'",dart.read_text())
 if not match:continue
 pkg=match[1]
 for name,body in re.findall(r'const (\w+)\$json = (\{.*?\n\});',path.read_text(),re.S):
  try:d=ast.literal_eval(re.sub(r'\w+\$json','""',body).replace('true','True').replace('false','False'))
  except:continue
  if '2' not in d or not any('3' in x for x in d['2']):continue
  schema[pkg+'.'+name.replace('_','.')]=d['2']
seeds=['bilibili.app.im.v1.'+n for n in ['SessionUpdateReq','SessionUpdateReply','SessionMainReq','SessionMainReply','SessionSecondaryReq','SessionSecondaryReply','ClearUnreadReq','ClearUnreadReply','PinSessionReq','PinSessionReply','UnPinSessionReq','UnPinSessionReply','DeleteSessionListReq','DeleteSessionListReply','GetImSettingsReq','GetImSettingsReply','SetImSettingsReq','SetImSettingsReply','KeywordBlockingListReq','KeywordBlockingListReply','KeywordBlockingAddReq','KeywordBlockingAddReply','KeywordBlockingDeleteReq','KeywordBlockingDeleteReply']]
seeds += ['bilibili.im.interfaces.v1.'+n for n in ['ReqSessionMsg','RspSessionMsg','ReqSendMsg','RspSendMsg','ReqTotalUnread','RspTotalUnread']]
seeds += ['bilibili.metadata.Metadata','bilibili.metadata.device.Device','bilibili.metadata.fawkes.FawkesReq','bilibili.metadata.network.Network','bilibili.metadata.locale.Locale']
selected={}
def visit(key):
 if key in selected:return
 if key not in schema:
  selected[key]=[];return
 selected[key]=schema[key]
 for f in schema[key]:
  if f['5']==11:visit(f['6'].lstrip('.'))
for key in seeds:visit(key)
p=Path('infra/bili/src/main/resources/im-schema.json');p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(json.dumps(selected,ensure_ascii=False,separators=(',',':'))+'\n')

print('schemas',len(selected),'bytes',p.stat().st_size)
for k in selected:
 if not selected[k]:print('empty',k)
