#!/usr/bin/env python3
"""Finance integration on the dedicated LOCAL cluster only; creates a disposable test tenant."""
import concurrent.futures, datetime as dt, getpass, hashlib, json, os, pathlib, subprocess, urllib.request, urllib.error, uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
LOCAL=ROOT/'.local'
env={k:v for k,v in os.environ.items() if not k.startswith('PG')}
def sql(statement,app=False,ok=True):
    localenv=env.copy()
    if app:localenv['PGPASSWORD']=cfg['sportsPassword']
    result=subprocess.run(['psql','-X','-h', '127.0.0.1' if app else str(LOCAL/'sports-postgres-socket'),'-p','55432','-U','sports_app' if app else getpass.getuser(),'-d','sports_local','-At','-v','ON_ERROR_STOP=1'],input=statement,text=True,capture_output=True,env=localenv)
    if ok and result.returncode:raise AssertionError('Local finance SQL failed (details withheld to protect credentials)')
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
assert request('/finance')[0]==401
view=checked('/finance',token=a);assert view['cashBase']==150000
now=dt.date.fromisoformat(view['today']);prev=now.replace(day=1)-dt.timedelta(days=1);month=prev.strftime('%Y-%m');current=now.strftime('%Y-%m')
bank=uid();checked('/finance/accounts',{'id':bank,'name':'Cuenta negocio','ownerId':None},a)
checked('/finance/accounts',{'id':uid(),'name':'No autorizada'},e,403)
def closing(day,cash,transfers=[]):return {'id':uid(),'day':day.isoformat(),'cash':cash,'transfers':transfers}
first=closing(prev-dt.timedelta(days=1),300000,[{'accountId':bank,'amount':100000}]);checked('/finance/days',first,e);checked('/finance/days',first,e)
checked('/finance/days',dict(first,cash=1),e,409)
checked('/finance/days',closing(prev,200000),e)
checked('/finance/days',closing(prev,1),a,409)
view=checked('/finance',token=e);cash=next(b['account']['id'] for b in view['accounts'] if b['account']['kind']=='CASH')
checked('/finance/expenses',{'id':uid(),'day':prev.isoformat(),'accountId':cash,'amount':50000,'note':'Compra insumos'},e)
# The business account may be used by employees; another holder may not.
checked('/finance/expenses',{'id':uid(),'day':prev.isoformat(),'accountId':bank,'amount':20000,'note':'Transporte'},e)
report=checked('/finance/report?month='+month,token=a);assert (report['incomeCash'],report['incomeTransfer'],report['expenses'],report['result'])==(500000,100000,70000,530000)
checked('/finance/months',{'id':uid(),'month':month,'note':'Revisión de prueba'},a)
snapshot=checked('/finance/report?month='+month,token=a);assert snapshot['closed']
checked('/finance/days',closing(prev-dt.timedelta(days=4),2),e,409)
# Financial custody crosses months without reopening the old monthly report.
h=uid();checked('/finance/handovers',{'id':h,'receiverId':admin},e)
view=checked('/finance',token=e);assert view['handovers'][0]['expected']==450000
checked('/finance/expenses',{'id':uid(),'day':now.isoformat(),'accountId':cash,'amount':1,'note':'Bloqueado durante entrega'},e,409)
checked('/finance/handovers/'+h+'/accept',{'id':uid(),'received':450000},o,403)
checked('/finance/handovers/'+h+'/accept',{'id':uid(),'received':440000},a,400)
accept={'id':uid(),'received':440000,'note':'Faltante contado: diez mil'}
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results=list(pool.map(lambda _:request('/finance/handovers/'+h+'/accept','POST',accept,a),range(2)))
assert [x[0] for x in results]==[200,200],results
assert snapshot==checked('/finance/report?month='+month,token=a)
report=checked('/finance/report?month='+current,token=a);assert report['shortages']==10000 and report['incomeCash']==0
view=checked('/finance',token=a);adminCash=next(b['account']['id'] for b in view['accounts'] if b['account'].get('ownerId')==admin)
assert next(b['balance'] for b in view['accounts'] if b['account']['id']==cash)==0
checked('/finance/expenses',{'id':uid(),'day':now.isoformat(),'accountId':adminCash,'amount':1,'note':'No es mío'},e,403)
checked('/finance/expenses',{'id':uid(),'day':now.isoformat(),'accountId':adminCash,'amount':50000,'note':'Insumos admin'},a)
checked('/finance/adjustments',{'id':uid(),'day':now.isoformat(),'accountId':adminCash,'amount':-5000,'note':'Corrección revisada','relatesToMonth':month},a)
# Concurrent daily closures: exactly one committed ledger and daily row.
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results=list(pool.map(lambda _:request('/finance/days','POST',closing(now,100000),e),range(2)))
assert sorted(x[0] for x in results)==[200,409],results
assert len(checked('/finance/report?month='+current,token=a)['days'])==1
# Cross-tenant IDs never resolve to accounts or receipts.
checked('/finance/days',closing(now+dt.timedelta(days=1),1),e,400)
checked('/finance/expenses',{'id':uid(),'day':now.isoformat(),'accountId':uid(),'amount':1,'note':'Cuenta ajena'},a,404)
# Statement traces an accepted delivery back to previous-month daily closings.
statement=checked('/finance/accounts/'+adminCash+'/statement?month='+current,token=a)
receipt=next(m for m in statement['movements'] if m['entry']['kind']=='HANDOVER_IN')
assert [d['day'] for d in receipt['sourceDays']]==[(prev-dt.timedelta(days=1)).isoformat(),prev.isoformat()]
assert receipt['handover']['senderId']==employee and receipt['handover']['received']==440000
assert (statement['opening'],statement['incoming'],statement['outgoing'],statement['closing'])==(0,440000,55000,385000)
assert statement['movements'][0]['balanceAfter']==statement['closing']
assert checked('/finance/accounts/'+cash+'/statement?month='+current,token=e)['opening']==450000
assert snapshot==checked('/finance/report?month='+month,token=a)
# An admin may optionally transfer money received from others without own daily closings.
assert not checked('/finance',token=a)['pendingDays']
transfer={'id':uid(),'receiverId':other}
checked('/finance/handovers',transfer,a);checked('/finance/handovers',transfer,a)
checked('/finance/handovers/'+transfer['id']+'/accept',{'id':uid(),'received':385000,'note':'Traslado entre administradores'},o)
assert checked('/finance/accounts/'+adminCash+'/statement?month='+current,token=a)['closing']==0
checked('/finance/accounts/'+uid()+'/statement?month='+current,token=a,status=404)
# Actual database grants and triggers prevent history rewrites; sales role has no finance access.
sql('UPDATE sports.finance_entries SET amount=0 WHERE tenant_id='+q(tenant),app=True,ok=False)
sql('BEGIN; UPDATE sports.finance_days SET cash=0 WHERE tenant_id='+q(tenant)+'; ROLLBACK;',ok=False)
assert sql("SELECT has_table_privilege('sales_local_app','sports.finance_entries','SELECT')")=='f'
assert sql("SELECT has_table_privilege('sales_local_app','sports.finance_entries','INSERT')")=='f'
assert before==legacy_snapshot(),'Legacy schema or rows changed'
# Browser test credentials remain local and private, never printed.
fixture=LOCAL/'sports-finance-test.json'
fixture.write_text(json.dumps({'tenant':tenant,'employee':employee,'admin':admin,'employeeToken':e,'adminToken':a,'bank':bank,'cash':cash,'adminCash':adminCash,'today':now.isoformat(),'previousMonth':month,'month':current}));fixture.chmod(0o600)
print('PASS: finance PostgreSQL/API, base exclusion, bank custody, cumulative delivery, mandatory shortage note, concurrency/idempotency, expenses, monthly snapshots, adjustments, immutable ledger and unchanged local legacy schema/data.')
