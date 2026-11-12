#!/usr/bin/env python3
"""Investment Java17 smoke on the fixed temporary branch. Never targets production."""
import datetime,hashlib,importlib.util,json,os,pathlib,subprocess,sys,urllib.request,urllib.error,uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('release',ROOT/'scripts/sports/release-finance.py');release=importlib.util.module_from_spec(spec);spec.loader.exec_module(release)
HOST='ep-muddy-cloud-b7fkob2e.c-13.us-east-1.aws.neon.tech'
assert HOST!=release.PROD and HOST=='ep-muddy-cloud-b7fkob2e.c-13.us-east-1.aws.neon.tech'
OUT=release.OUT
JAR=pathlib.Path('/private/tmp/creativecode-investments-package/libs/creativecode-1.0.jar')
BASE='http://127.0.0.1:5004'
if '--run' in sys.argv:
 cfg=json.loads((OUT/'production-credentials.json').read_text());values=release.credentials()
 env={k:v for k,v in os.environ.items() if not k.startswith(('DB_','SPORTS_','SPRING_','LOGGING_','DEBUG','TRACE','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'))}
 url='jdbc:postgresql://'+HOST+'/neondb?sslmode=require'
 env.update(SPRING_PROFILES_ACTIVE='sports',DB_URL=url,DB_USER=values['DB_USER'],DB_PASSWORD=values['DB_PASSWORD'],SPORTS_DB_URL=url,SPORTS_DB_USER=cfg['role'],SPORTS_DB_PASSWORD=cfg['password'],SPORTS_ALLOWED_ORIGINS=BASE)
 java='/opt/homebrew/opt/openjdk@17/bin/java'
 os.execve(java,[java,'-Xmx384m','-jar',str(JAR),'--server.address=127.0.0.1','--server.port=5004','--spring.jpa.hibernate.ddl-auto=none','--debug=false','--trace=false','--logging.level.root=WARN','--logging.level.com.portafolio=WARN','--logging.level.org.springframework=WARN'],env)

def q(v):return "'"+str(v).replace("'","''")+"'"
def uid():return str(uuid.uuid4())
def request(path,body=None,token=None,expected=200):
 req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with urllib.request.urlopen(req,timeout=30) as response:
   status=response.status;raw=response.read();content=response.headers.get('Content-Type','')
 except urllib.error.HTTPError as error:status=error.code;raw=error.read();content='application/json'
 assert status==expected,(path,status)
 return json.loads(raw) if raw and 'json' in content else raw

# A fictitious tenant is seeded only in the fixed rehearsal branch, never in production.
password=uuid.uuid4().hex
crypto=OUT/'spring-security-crypto.jar';helper=OUT/'HashPassword.java'
r=subprocess.run(['/opt/homebrew/opt/openjdk@17/bin/java','-cp',str(crypto),str(helper)],input=password+'\n',text=True,capture_output=True,check=True)
hashed=r.stdout.strip();assert hashed.startswith('$2')
tenant,admin,employee,other=uid(),uid(),uid(),uid()
settings=json.dumps({'name':'Ensayo financiero temporal','timezone':'America/Bogota','currency':'COP','toleranceMinutes':15,'waterMinutes':30,'warningMinutes':5})
release.sql('INSERT INTO sports.tenants(id,slug,settings) VALUES('+q(tenant)+','+q(tenant)+','+q(settings)+'::jsonb)',HOST)
for id,name,role in [(admin,'admin','ADMIN'),(employee,'empleado','EMPLOYEE'),(other,'otroadmin','ADMIN')]:
 release.sql('INSERT INTO sports.users(tenant_id,id,username,name,role,password_hash) VALUES('+','.join(map(q,[tenant,id,name,name,role,hashed]))+')',HOST)
a=request('/api/sports/v1/auth/login',{'tenantId':tenant,'username':'admin','password':password})['token']
e=request('/api/sports/v1/auth/login',{'tenantId':tenant,'username':'empleado','password':password})['token']
prefix='/api/sports/v1/investments'
view=request(prefix,token=a)
finance=request('/api/sports/v1/finance',token=a)
value={'id':uid(),'day':view['today'],'category':'EQUIPMENT','description':'Ensayo aislado de inversiones','amount':500000,'contributions':[{'adminId':admin,'amount':200000},{'adminId':other,'amount':300000}]}
request(prefix,token=e,expected=403);request(prefix,value,e,expected=403)
first=request(prefix,value,a);assert first==request(prefix,value,a)
request(prefix,dict(value,description='Distinto'),a,expected=409)
view=request(prefix,token=a);assert view['total']==500000 and len(view['records'])==1
assert view['byAdministrator'][0]=={'adminId':other,'amount':300000}
assert finance==request('/api/sports/v1/finance',token=a)
request('/api/portfolio/status');request('/api/portfolio/products',{})
request('/api/sports/v1/auth/logout',{},a);request('/api/sports/v1/auth/logout',{},e)
record={'passed':True,'host':HOST,'branch':'br-delicate-water-b79pl7d9','sha256':hashlib.sha256((ROOT/'sports/migrations/V006__administrator_investments.sql').read_bytes()).hexdigest(),'jarSha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'at':datetime.datetime.now(datetime.timezone.utc).isoformat()}
release.save(OUT/'investments-rehearsal-smoke.json',record)
print('PASS: Java17 candidate on isolated Neon branch; investments, shared totals, idempotency, admin-only permissions, unchanged cash and sales read endpoints.')
