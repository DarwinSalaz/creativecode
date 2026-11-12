#!/usr/bin/env python3
"""Investment integration on the dedicated LOCAL cluster only; creates a disposable test tenant."""
import concurrent.futures, datetime as dt, getpass, hashlib, json, os, pathlib, subprocess, urllib.request, urllib.error, uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
LOCAL=ROOT/'.local'
env={k:v for k,v in os.environ.items() if not k.startswith('PG')}
def sql(statement,app=False,ok=True):
    localenv=env.copy()
    if app:localenv['PGPASSWORD']=cfg['sportsPassword']
    result=subprocess.run(['psql','-X','-h', '127.0.0.1' if app else str(LOCAL/'sports-postgres-socket'),'-p','55432','-U','sports_app' if app else getpass.getuser(),'-d','sports_local','-At','-v','ON_ERROR_STOP=1'],input=statement,text=True,capture_output=True,env=localenv)
    if ok and result.returncode:raise AssertionError('Local investment SQL failed (details withheld to protect credentials)')
    if not ok:assert result.returncode!=0,'Expected DB protection to reject mutation'
    return result.stdout.strip()
def q(value):return "'"+str(value).replace("'","''")+"'"
def uid():return str(uuid.uuid4())
assert pathlib.Path(sql('SHOW data_directory')).resolve()==(LOCAL/'sports-postgres').resolve()
cfg=json.loads((LOCAL/'sports-local.json').read_text())
# Compare metadata and all local legacy rows before/after. No writes outside sports.
def legacy_snapshot():
    tables=sql("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename").splitlines()
    values=[sql("SELECT table_name,column_name,data_type,is_nullable,column_default FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position")]
    for table in tables:
        ident='"'+table.replace('"','""')+'"'
        values.append(sql('SELECT row_to_json(t)::text FROM public.'+ident+' t ORDER BY row_to_json(t)::text'))
    return hashlib.sha256('\n'.join(values).encode()).hexdigest()
before=legacy_snapshot()
tenant=uid();employee=uid();admin=uid();other=uid()
settings=json.dumps({'name':'Prueba de caja aislada','timezone':'America/Bogota','currency':'COP','toleranceMinutes':15,'waterMinutes':30,'warningMinutes':5})
sql('INSERT INTO sports.tenants(id,slug,settings) VALUES('+q(tenant)+','+q(tenant)+','+q(settings)+'::jsonb)')
for id,name,role in [(employee,'empleado','EMPLOYEE'),(admin,'admin','ADMIN'),(other,'otroadmin','ADMIN')]:
    sql('INSERT INTO sports.users(tenant_id,id,username,name,role,password_hash) VALUES('+','.join(map(q,[tenant,id,name,name,role]))+',public.crypt('+q(cfg['loginPassword'])+",public.gen_salt('bf',12)))")
BASE='http://127.0.0.1:5002/api/sports/v1'
def request(path,method='GET',body=None,token=None):
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})},method=method)
    try:
        with urllib.request.urlopen(req,timeout=30) as response:
            data=response.read();return response.status,json.loads(data) if data else None
    except urllib.error.HTTPError as e:
        data=e.read()
        try:return e.code,json.loads(data)
        except ValueError:return e.code,{'message':'non-json error'}
def checked(path,body=None,token=None,status=200):
    code,value=request(path,'GET' if body is None else 'POST',body,token)
    assert code==status,(path,code,value)
    return value
def login(name):return checked('/auth/login',{'tenantId':tenant,'username':name,'password':cfg['loginPassword']})['token']
a=login('admin');e=login('empleado');o=login('otroadmin')
assert request('/investments')[0]==401
checked('/investments',token=e,status=403)
view=checked('/investments',token=a)
now=dt.date.fromisoformat(view['today']);previous=now.replace(day=1)-dt.timedelta(days=1)
# Seed money in this disposable tenant to prove investments cannot affect existing cash.
checked('/finance/days',{'id':uid(),'day':now.isoformat(),'cash':450000},a)
finance=checked('/finance',token=a)
report=checked('/finance/report?month='+now.strftime('%Y-%m'),token=a)
def finance_snapshot():
    tables=sql("SELECT tablename FROM pg_tables WHERE schemaname='sports' AND tablename LIKE 'finance_%' ORDER BY tablename").splitlines()
    return hashlib.sha256('\n'.join(sql('SELECT row_to_json(t)::text FROM sports."'+t+'" t ORDER BY row_to_json(t)::text') for t in tables).encode()).hexdigest()
finance_before=finance_snapshot()
value={'id':uid(),'day':now.isoformat(),'category':'EQUIPMENT','description':'Reflectores cancha 2','amount':500000,'contributions':[{'adminId':admin,'amount':200000},{'adminId':other,'amount':300000}]}
checked('/investments',value,e,403)
checked('/investments',dict(value,amount=500001),a,400)
checked('/investments',dict(value,contributions=[{'adminId':employee,'amount':500000}]),a,400)
checked('/investments',dict(value,contributions=[{'adminId':uid(),'amount':500000}]),a,400)
checked('/investments',dict(value,day=(now+dt.timedelta(days=1)).isoformat()),a,400)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results=list(pool.map(lambda _:request('/investments','POST',value,a),range(2)))
assert [r[0] for r in results]==[200,200],results
assert results[0][1]==results[1][1]
checked('/investments',dict(value,description='Cambio no permitido'),a,409)
checked('/investments',value,o,409)
checked('/investments',dict(value,id=uid(),day=previous.isoformat()),o)
view=checked('/investments?month='+now.strftime('%Y-%m'),token=a)
assert view['total']==1000000 and view['periodTotal']==500000 and len(view['records'])==1
assert view['byAdministrator'][0]=={'adminId':other,'amount':600000}
assert len(checked('/investments',token=a)['records'])==2
assert finance==checked('/finance',token=a)
assert report==checked('/finance/report?month='+now.strftime('%Y-%m'),token=a)
assert finance_before==finance_snapshot(),'An investment changed the financial ledger'
for table in ['investments','investment_contributions']:
    sql('UPDATE sports.'+table+' SET amount=1 WHERE tenant_id='+q(tenant),app=True,ok=False)
    sql('DELETE FROM sports.'+table+' WHERE tenant_id='+q(tenant),app=True,ok=False)
    sql('UPDATE sports.'+table+' SET amount=1 WHERE tenant_id='+q(tenant),ok=False)
assert before==legacy_snapshot(),'Legacy sales data changed'
fixture=LOCAL/'sports-investment-test.json'
fixture.write_text(json.dumps({'tenant':tenant,'admin':admin,'other':other,'employee':employee,'adminToken':a,'employeeToken':e,'day':now.isoformat()}));fixture.chmod(0o600)
print('PASS: local API; admin-only access, exact allocation, concurrent idempotency, historic totals, monthly filtering, immutable records. Existing cash ledger and sales data unchanged.')
