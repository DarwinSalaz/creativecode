#!/usr/bin/env python3
"""Joint Java17 smoke on the fixed temporary branch. Never targets production."""
import datetime,hashlib,importlib.util,json,os,pathlib,subprocess,sys,urllib.request,urllib.error,uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('release',ROOT/'scripts/sports/release-finance.py');release=importlib.util.module_from_spec(spec);spec.loader.exec_module(release)
HOST=release.STAGE
assert HOST!=release.PROD and HOST=='ep-flat-star-b7qm5x8s.c-13.us-east-1.aws.neon.tech'
OUT=release.OUT
JAR=pathlib.Path('/private/tmp/creativecode-sports-package/libs/creativecode-1.0.jar')
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
tenant,admin,employee=uid(),uid(),uid()
settings=json.dumps({'name':'Ensayo financiero temporal','timezone':'America/Bogota','currency':'COP','toleranceMinutes':15,'waterMinutes':30,'warningMinutes':5})
release.sql('INSERT INTO sports.tenants(id,slug,settings) VALUES('+q(tenant)+','+q(tenant)+','+q(settings)+'::jsonb)',HOST)
for id,name,role in [(admin,'admin','ADMIN'),(employee,'empleado','EMPLOYEE')]:
 release.sql('INSERT INTO sports.users(tenant_id,id,username,name,role,password_hash) VALUES('+','.join(map(q,[tenant,id,name,name,role,hashed]))+')',HOST)
a=request('/api/sports/v1/auth/login',{'tenantId':tenant,'username':'admin','password':password})['token']
e=request('/api/sports/v1/auth/login',{'tenantId':tenant,'username':'empleado','password':password})['token']
prefix='/api/sports/v1/finance'
view=request(prefix,token=a);today=view['today'];assert view['cashBase']==150000
bank=uid();request(prefix+'/accounts',{'id':bank,'name':'Cuenta de ensayo','ownerId':admin},a)
day={'id':uid(),'day':today,'cash':500000,'transfers':[{'accountId':bank,'amount':100000}]}
request(prefix+'/days',day,e);request(prefix+'/days',day,e)
cash=next(b['account']['id'] for b in request(prefix,token=e)['accounts'] if b['account']['kind']=='CASH')
request(prefix+'/expenses',{'id':uid(),'day':today,'accountId':cash,'amount':50000,'note':'Insumos ficticios'},e)
handover=uid();request(prefix+'/handovers',{'id':handover,'receiverId':admin},e)
request(prefix+'/handovers/'+handover+'/accept',{'id':uid(),'received':440000,'note':'Faltante ficticio de diez mil'},a)
report=request(prefix+'/report?month='+today[:7],token=a)
assert (report['incomeCash'],report['incomeTransfer'],report['expenses'],report['shortages'],report['result'])==(500000,100000,50000,10000,540000)
assert next(b['balance'] for b in report['balances'] if b['account']['id']==cash)==0
request('/api/portfolio/status');html=request('/sports/index.html');assert b'<sports-app' in html
request(prefix,expected=401)
record={'passed':True,'host':HOST,'branch':release.BRANCH,'sha256':hashlib.sha256((ROOT/'sports/migrations/V005__daily_finance.sql').read_bytes()).hexdigest(),'jarSha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'at':datetime.datetime.now(datetime.timezone.utc).isoformat()}
release.save(OUT/'finance-rehearsal-smoke.json',record)
print('PASS: Java17 joint JAR on isolated Neon branch; sales health, sports UI/auth, cash and bank income, duplicate retry, expense, handover, shortage, balances and monthly report.')
