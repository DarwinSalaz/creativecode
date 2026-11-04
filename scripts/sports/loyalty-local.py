#!/usr/bin/env python3
"""Explicit localhost-only loyalty integration. Creates fictitious local contacts and matches."""
import concurrent.futures, datetime, json, pathlib, secrets, urllib.request, urllib.error, uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
cfg=json.loads((ROOT/'.local/sports-local.json').read_text())
BASE='http://127.0.0.1:5002/api/sports/v1'
tenant='11111111-1111-4111-8111-111111111111'
courts=['33333333-3333-4333-8333-333333333333','44444444-4444-4444-8444-444444444444']
def request(path,method='GET',body=None,token=None):
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})},method=method)
    try:
        with urllib.request.urlopen(req,timeout=25) as r:
            data=r.read();return r.status,json.loads(data) if data else None
    except urllib.error.HTTPError as e:return e.code,json.loads(e.read())
def login(name):
    code,result=request('/auth/login','POST',{'tenantId':tenant,'username':name,'password':cfg['loginPassword']});assert code==200,(code,result);return result['token']
admin=login('admin');worker=login('empleado1')
client=str(uuid.uuid4())
assert request('/clients','POST',{'id':client,'name':'Promoción · prueba local'},admin)[0]==200
policy={'policy':{'requiredMatches':5,'rewardPercent':50,'maxGapDays':21,'active':True},'version':0}
assert request('/clients/'+client+'/loyalty','PUT',policy,worker)[0]==403
assert request('/clients/'+client+'/loyalty','PUT',policy,admin)[0]==200
assert request('/clients/'+str(uuid.uuid4())+'/loyalty','PUT',policy,admin)[0]==404
base=datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=400+secrets.randbelow(10000))
base=base.replace(hour=18,minute=0,second=0,microsecond=0)
def create(n,court=0):
    start=base+datetime.timedelta(days=n)
    code,b=request('/reservations','POST',{'courtId':courts[court],'clientId':client,'guestName':'Prueba local','start':start.isoformat(),'end':(start+datetime.timedelta(hours=1)).isoformat(),'outsideHoursConfirmed':True,'status':'CONFIRMED'},admin)
    assert code==200,(code,b);return b
def action(b,op,token=worker):
    code,result=request('/reservations/'+b['id']+'/'+op,'POST',{'version':b['version'],'exceptionConfirmed':True},token)
    assert code==200,(code,result);return result
def state():return next(x for x in request('/loyalty',token=admin)[1] if x['clientId']==client)['state']
for n in range(5):
    running=action(create(n),'start');done=action(running,'finish');again=action(running,'finish')
    assert done['version']==again['version'] and done['rewardPercent']==0
assert state()['rewardReady']
action(create(6),'cancel');assert state()['rewardReady']
# Distinct court locks but same client: exactly one transaction redeems the reward.
running=[action(create(7,0),'start'),action(create(7,1),'start')]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(lambda b:action(b,'finish'),running))
assert sorted(x['rewardPercent'] for x in results)==[0,50],results
assert state()['played']==1 and not state()['rewardReady']
code,history=request('/clients/'+client+'/loyalty/history',token=admin)
assert code==200 and len([e for e in history if e['type']=='PLAYED'])==6
assert len([e for e in history if e['type']=='REDEEMED'])==1
s=state();until=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=30)).isoformat()
assert request('/clients/'+client+'/loyalty/exception','POST',{'until':until,'reason':'Lluvia en prueba local','version':s['version']},admin)[0]==200
assert request('/clients/'+client+'/loyalty/exception','POST',{'until':until,'reason':'Reintento obsoleto','version':s['version']},admin)[0]==409
print('PASS: local loyalty configuration, roles, five matches, finish retries, cancellation preservation, concurrent redemption on two courts, ledger and versioned rain exception.')
