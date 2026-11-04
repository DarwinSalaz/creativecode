#!/usr/bin/env python3
"""Real HTTP/SQL tests, hard-wired to the isolated local environment."""
import concurrent.futures, datetime, getpass, json, os, pathlib, secrets, subprocess, urllib.request, urllib.error, uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
config=json.loads((ROOT/'.local/sports-local.json').read_text())
BASE='http://127.0.0.1:5002/api/sports/v1'
tenant='11111111-1111-4111-8111-111111111111'
court='33333333-3333-4333-8333-333333333333'
def request(path,method='GET',body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=25) as r:
            data=r.read();return r.status,json.loads(data) if data else None
    except urllib.error.HTTPError as e:
        data=e.read();return e.code,json.loads(data) if data else None
status,login=request('/auth/login','POST',{'tenantId':tenant,'username':'admin','password':config['loginPassword']})
assert status==200,(status,login)
admin=login['token']
assert request('/me',token=admin)[1]['role']=='ADMIN'
assert request('/courts')[0]==401
status,login=request('/auth/login','POST',{'tenantId':tenant,'username':'empleado1','password':config['loginPassword']})
assert status==200,(status,login)
worker=login['token']
assert request('/courts','POST',{'name':'Forbidden'},worker)[0]==403
start=datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=40,seconds=secrets.randbelow(86400*100))
start=start.replace(second=0,microsecond=0)
body={'courtId':court,'guestName':'Prueba concurrencia local','start':start.isoformat(),'end':(start+datetime.timedelta(hours=1)).isoformat(),'outsideHoursConfirmed':True,'status':'CONFIRMED'}
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    results=list(pool.map(lambda _:request('/reservations','POST',body,admin),range(6)))
codes=[r[0] for r in results]
assert codes.count(200)==1 and codes.count(409)==5,codes
booking=next(r[1] for r in results if r[0]==200)
status,running=request('/reservations/'+booking['id']+'/start','POST',{'version':0,'exceptionConfirmed':True,'deviceId':'local-test'},worker)
assert status==200,(status,running)
status,again=request('/reservations/'+booking['id']+'/start','POST',{'version':0,'exceptionConfirmed':True},worker)
assert status==200 and again['version']==running['version']
status,done=request('/reservations/'+booking['id']+'/finish','POST',{'version':running['version']},worker)
assert status==200,(status,done)
status,again=request('/reservations/'+booking['id']+'/finish','POST',{'version':running['version']},worker)
assert status==200 and done['version']==again['version']
assert request('/reservations/'+str(uuid.uuid4())+'/cancel','POST',{'version':0},admin)[0]==404
# Prove runtime roles cannot cross the two schemas (no real sales data exists locally).
command=['psql','-X','-h',str(ROOT/'.local/sports-postgres-socket'),'-p','55432','-U',getpass.getuser(),'-d','sports_local','-At','-v','ON_ERROR_STOP=1']
env={k:v for k,v in os.environ.items() if not k.startswith('PG')}
sql="SELECT has_schema_privilege('sports_app','public','USAGE'), has_schema_privilege('sales_local_app','sports','USAGE'), has_table_privilege('sports_app','sports.audit_events','UPDATE'), has_table_privilege('sports_app','sports.reservations','DELETE');"
r=subprocess.run(command,input=sql,capture_output=True,text=True,check=True,env=env)
assert r.stdout.strip()=='f|f|f|f',r.stdout
print('PASS: real local login; employee permissions; 6 concurrent reservations = 1 success + 5 conflicts; idempotent start/finish; restricted schema privileges. No remote DB used.')
