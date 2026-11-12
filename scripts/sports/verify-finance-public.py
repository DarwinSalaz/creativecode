#!/usr/bin/env python3
"""Verify the deployed artifact and read-only business APIs. No financial test records."""
from decimal import Decimal
import argparse,datetime,hashlib,json,pathlib,re,time,urllib.request,urllib.error,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[2];OUT=ROOT/'.local/sports-release'
BASE='https://creative-code-darwinsalaz.koyeb.app'
p=argparse.ArgumentParser();p.add_argument('--wait',type=int,default=0);p.add_argument('--investments',action='store_true');p.add_argument('--jar',type=pathlib.Path,default=ROOT/'build/libs/creativecode-1.0.jar');a=p.parse_args()
jar=a.jar
with zipfile.ZipFile(jar) as z:
 html=z.read('BOOT-INF/classes/static/sports/index.html')
 main=re.search(rb'src="(main-[^"]+\.js)"',html).group(1).decode()
 expected=hashlib.sha256(z.read('BOOT-INF/classes/static/sports/'+main)).hexdigest()
def request(path,body=None,token=None):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json','Cache-Control':'no-cache',**({'Authorization':'Bearer '+token} if token else {})})
 with urllib.request.urlopen(req,timeout=45) as r:return r.status,r.read()
deadline=time.monotonic()+a.wait
while True:
 try:
  status,public=request('/sports/index.html?release='+expected[:12])
  if main.encode() in public:
   _,bundle=request('/sports/'+main)
   if hashlib.sha256(bundle).hexdigest()==expected:break
 except (urllib.error.URLError,TimeoutError):pass
 if time.monotonic()>=deadline:raise SystemExit('Deployment not verified: public frontend is not the candidate yet.')
 print('Waiting for Koyeb to serve the new artifact…',flush=True);time.sleep(15)
checks={'frontendBundle':main,'frontendSha256':expected}
checks['salesStatus']=request('/api/portfolio/status')[0]
checks['salesProductsRead']=request('/api/portfolio/products',{})[0]
cfg=json.loads((OUT/'production-credentials.json').read_text())
status,login=request('/api/sports/v1/auth/login',{'tenantId':cfg['tenantId'],'username':'admin','password':cfg['adminPassword']})
token=json.loads(login)['token']
try:
 status,body=request('/api/sports/v1/finance',token=token);overview=json.loads(body)
 assert overview['cashBase']>=0 and isinstance(overview['accounts'],list)
 checks['financeOverview']=status
 if overview['accounts']:
  selected=next((b for b in overview['accounts'] if b['account']['kind']=='CASH'),overview['accounts'][0])
  code,raw=request('/api/sports/v1/finance/accounts/'+selected['account']['id']+'/statement?month='+overview['today'][:7],token=token)
  statement=json.loads(raw)
  assert Decimal(str(statement['opening']))+Decimal(str(statement['incoming']))-Decimal(str(statement['outgoing']))==Decimal(str(statement['closing']))
  assert 'movements' in statement and all('sourceDays' in m for m in statement['movements'])
  checks['financeAccountStatement']=code
 checks['financeMonth']=request('/api/sports/v1/finance/report?month='+overview['today'][:7],token=token)[0]
 checks['sportsLoyalty']=request('/api/sports/v1/loyalty',token=token)[0]
 checks['sportsRecurrenceRules']=request('/api/sports/v1/recurrence-rules',token=token)[0]
 checks['sportsCourts']=request('/api/sports/v1/courts',token=token)[0]
 if a.investments:
  code,raw=request('/api/sports/v1/investments',token=token);investments=json.loads(raw)
  assert isinstance(investments['records'],list) and isinstance(investments['administrators'],list)
  assert Decimal(str(investments['total']))==sum((Decimal(str(x['amount'])) for x in investments['byAdministrator']),Decimal(0))
  checks['sportsInvestments']=code
  checks['sportsInvestmentsMonth']=request('/api/sports/v1/investments?month='+overview['today'][:7],token=token)[0]
  try:request('/api/sports/v1/investments')
  except urllib.error.HTTPError as err:assert err.code==401;checks['sportsInvestmentsUnauthenticated']=err.code
  else:raise AssertionError('Investments accepted an anonymous request')
finally:request('/api/sports/v1/auth/logout',{},token)
checks.update(verifiedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),url=BASE+'/sports/index.html',jarSha256=hashlib.sha256(jar.read_bytes()).hexdigest())
file=OUT/'finance-public-verified.json';file.write_text(json.dumps(checks,indent=2));file.chmod(0o600)
print(json.dumps(checks,indent=2))
