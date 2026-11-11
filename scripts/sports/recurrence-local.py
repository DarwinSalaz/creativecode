#!/usr/bin/env python3
"""Local-only integration: isolated fictitious court; never accepts a remote URL."""
import concurrent.futures, datetime as dt, json, pathlib, urllib.request, urllib.error, urllib.parse, uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
cfg=json.loads((ROOT/'.local/sports-local.json').read_text())
BASE='http://127.0.0.1:5002/api/sports/v1'
tenant='11111111-1111-4111-8111-111111111111'
def request(path,method='GET',body=None,token=None):
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})},method=method)
    try:
        with urllib.request.urlopen(req,timeout=60) as r:
            data=r.read();return r.status,json.loads(data) if data else None
    except urllib.error.HTTPError as e:return e.code,json.loads(e.read())
def ok(path,method='GET',body=None,token=None):
    code,res=request(path,method,body,token);assert code==200,(path,code,res);return res
def login(name):return ok('/auth/login','POST',{'tenantId':tenant,'username':name,'password':cfg['loginPassword']})['token']
admin=login('admin');worker=login('empleado1')
court=str(uuid.uuid4());client=str(uuid.uuid4());teacher=str(uuid.uuid4())
ok('/courts','POST',{'id':court,'name':'Series prueba local '+court[:6]},admin)
ok('/clients','POST',{'id':client,'name':'Cliente serie local'},admin)
ok('/teachers','POST',{'id':teacher,'name':'Profesor serie local'},admin)
start=(dt.date.today()+dt.timedelta(days=14));start+=dt.timedelta(days=(1-start.weekday())%7)
rule={'id':str(uuid.uuid4()),'courtId':court,'clientId':client,'guestName':'Nombre anterior','weekday':2,'localTime':'21:00','durationMinutes':60,'fromDate':str(start),'untilDate':None,'outsideHoursConfirmed':True}
try:
    created=ok('/recurrence-rules','POST',rule,worker)
    assert 13<=len(created)<=14 and all(b['guestName']=='Cliente serie local' for b in created)
    assert ok('/recurrence-rules','POST',rule,worker)==[]
    first=created[0]
    def window(t):
        a=dt.datetime.fromisoformat(t.replace('Z','+00:00'));b=a+dt.timedelta(hours=1)
        return '/reservations?'+urllib.parse.urlencode({'from':a.isoformat().replace('+00:00','Z'),'to':b.isoformat().replace('+00:00','Z'),'courtId':court})
    distant=dt.datetime.fromisoformat(first['start'].replace('Z','+00:00'))+dt.timedelta(weeks=520)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        views=list(pool.map(lambda _:ok(window(distant.isoformat()),token=worker),range(4)))
    assert all(len(v)==1 for v in views) and len({v[0]['id'] for v in views})==1
    ok('/reservations/'+first['id']+'/cancel','POST',{'version':first['version']},worker)
    replacement={'courtId':court,'guestName':'Reemplazo local','start':first['start'],'end':first['end'],'outsideHoursConfirmed':True}
    ok('/reservations','POST',replacement,worker)
    rows=ok(window(first['start']),token=worker)
    assert len(rows)==2 and sum(b['status']=='CANCELLED_CLIENT' for b in rows)==1
    second=created[1];shift=dt.datetime.fromisoformat(second['start'].replace('Z','+00:00'))+dt.timedelta(hours=1)
    ok('/reservations/'+second['id'],'PATCH',{'courtId':court,'guestName':'Movida','start':shift.isoformat(),'end':(shift+dt.timedelta(hours=1)).isoformat(),'kind':'FIXED','version':second['version'],'outsideHoursConfirmed':True},worker)
    assert ok(window(second['start']),token=worker)==[]
    # Finite teacher series, preserving class semantics and its inclusive final day.
    lesson={**rule,'id':str(uuid.uuid4()),'clientId':None,'teacherId':teacher,'kind':'CLASS','guestName':'Profesor serie local','localTime':'18:00','durationMinutes':120,'untilDate':str(start+dt.timedelta(days=28))}
    lessons=ok('/recurrence-rules','POST',lesson,admin)
    assert len(lessons)==5 and all(b['teacherId']==teacher and b['kind']=='CLASS' for b in lessons)
    rules=ok('/recurrence-rules',token=admin);r=next(r for r in rules if r['id']==rule['id'])
    stop={'fromDate':str(start+dt.timedelta(days=14)),'version':r['version']}
    assert request('/recurrence-rules/'+r['id']+'/stop','POST',stop,worker)[0]==403
    ok('/recurrence-rules/'+r['id']+'/stop','POST',stop,admin)
    assert all(b['status']=='CANCELLED_CLIENT' for b in ok(window(distant.isoformat()),token=worker))
    assert request('/recurrence-rules/'+r['id']+'/stop','POST',stop,admin)[0]==409
    print('PASS: weekly series, unbounded future, concurrent idempotent generation, cancellation/replacement, moved-date exception, finite teacher classes, stop permissions and versions. Local database only.')
finally:
    # Stop all test rules so later unrelated local tests never generate their future bookings.
    for r in ok('/recurrence-rules',token=admin):
        if r['courtId']==court:ok('/recurrence-rules/'+r['id']+'/stop','POST',{'fromDate':str(dt.date.today()),'version':r['version']},admin)
    ok('/courts','POST',{'id':court,'name':'Series prueba local '+court[:6],'active':False},admin)
    ok('/auth/logout','POST',{},worker);ok('/auth/logout','POST',{},admin)
