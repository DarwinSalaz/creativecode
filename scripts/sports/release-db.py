#!/usr/bin/env python3
"""Explicit, scoped release migration. Never called by application startup.
Reads existing sales credentials without changing them. Only sports schema and
sports_runtime role may be created. Production invocation requires --production.
"""
import argparse, hashlib, json, os, pathlib, secrets, shlex, subprocess, urllib.parse, uuid, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[2]
OUT=ROOT/'.local/sports-release';OUT.mkdir(mode=0o700,parents=True,exist_ok=True)
PROD='ep-winter-band-b7vwqo6z.c-13.us-east-1.aws.neon.tech'
STAGE='ep-blue-grass-b733t6b2.c-13.us-east-1.aws.neon.tech'
parser=argparse.ArgumentParser();parser.add_argument('--production',action='store_true');args=parser.parse_args()
name='production' if args.production else 'rehearsal';host=PROD if args.production else STAGE
values={}
for line in (ROOT/'.env').read_text().splitlines():
 if '=' in line and not line.lstrip().startswith('#'):
  k,v=line.split('=',1);values[k.strip()]=shlex.split(v)[0] if v else ''
u=urllib.parse.urlparse(values['DB_URL'].removeprefix('jdbc:'))
assert u.hostname.replace('-pooler','')==PROD and u.path=='/neondb','Unexpected sales target'
env={k:v for k,v in os.environ.items() if not k.startswith('PG')}
env.update(PGHOST=host,PGPORT='5432',PGDATABASE='neondb',PGUSER=values['DB_USER'],PGPASSWORD=values['DB_PASSWORD'],PGSSLMODE='require',PGCONNECT_TIMEOUT='15',PGOPTIONS='-c statement_timeout=30000 -c lock_timeout=5000')
def sql(text,runenv=env):
 r=subprocess.run(['psql','-X','-At','-v','ON_ERROR_STOP=1'],input=text,text=True,capture_output=True,env=runenv)
 if r.returncode:raise RuntimeError('Release SQL failed: '+r.stderr.splitlines()[0])
 return r.stdout.strip()
def quote(v):return "'"+str(v).replace("'","''")+"'"
def save(p,data):
 p.write_text(json.dumps(data,indent=2)+'\n');p.chmod(0o600)
# Catalog-only fingerprint: no sales row contents are extracted.
fingerprint="""SELECT md5(string_agg(item,E'\n' ORDER BY item)) FROM (
SELECT row_to_json(t)::text item FROM (SELECT table_name,column_name,data_type,is_nullable,column_default,ordinal_position FROM information_schema.columns WHERE table_schema='public') t
UNION ALL SELECT row_to_json(t)::text FROM (SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public') t
UNION ALL SELECT row_to_json(t)::text FROM (SELECT c.relname,k.conname,pg_get_constraintdef(k.oid) definition FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public') t
UNION ALL SELECT row_to_json(t)::text FROM (SELECT grantee,table_name,privilege_type,is_grantable FROM information_schema.role_table_grants WHERE table_schema='public') t
UNION ALL SELECT nspname||coalesce(nspacl::text,'') FROM pg_namespace WHERE nspname='public'
) s;"""
before=sql(fingerprint)
assert sql("SELECT count(*) FROM pg_tables WHERE schemaname='public'")=='21','Unexpected sales schema'
files=sorted((ROOT/'sports/migrations').glob('V*.sql'))
checks={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
if args.production:
 rehearsal=json.loads((OUT/'rehearsal-verified.json').read_text())
 assert rehearsal['migrations']==checks and rehearsal['salesSchemaBefore']==rehearsal['salesSchemaAfter'],'Rehearsal evidence does not match migrations'

secret=OUT/(name+'-credentials.json')
if secret.exists():cfg=json.loads(secret.read_text())
else:
 cfg={'host':host,'role':'sports_runtime','password':secrets.token_urlsafe(36),'tenantId':str(uuid.uuid4()),'adminId':str(uuid.uuid4()),'adminPassword':secrets.token_urlsafe(24),'migrations':checks}
 save(secret,cfg)
assert cfg['host']==host and cfg['migrations']==checks,'Release configuration or migration checksums changed'
if sql("SELECT count(*) FROM pg_namespace WHERE nspname='sports'")!='0':
 assert (OUT/(name+'-verified.json')).exists(),'Schema already exists without this release record; inspect manually'
 assert sql('SELECT string_agg(version::text,\',\' ORDER BY version) FROM sports.schema_history')=='1,2,3'
 print(name+': migrations already applied; no writes');raise SystemExit()
assert sql("SELECT count(*) FROM pg_roles WHERE rolname='sports_runtime'")=='0','Role already exists; inspect manually'
jar=pathlib.Path('/private/tmp/creativecode-sports-package/libs/creativecode-1.0.jar')
with zipfile.ZipFile(jar) as z:
 entry=next(n for n in z.namelist() if n.startswith('BOOT-INF/lib/spring-security-crypto-') and n.endswith('.jar'))
 crypto=OUT/'spring-security-crypto.jar';crypto.write_bytes(z.read(entry))
helper=OUT/'HashPassword.java';helper.write_text('import java.io.*;import org.springframework.security.crypto.bcrypt.BCrypt;class HashPassword{public static void main(String[] a)throws Exception{System.out.print(BCrypt.hashpw(new BufferedReader(new InputStreamReader(System.in)).readLine(),BCrypt.gensalt(12)));}}')
r=subprocess.run(['/opt/homebrew/opt/openjdk@17/bin/java','-cp',str(crypto),str(helper)],input=cfg['adminPassword']+'\n',text=True,capture_output=True,check=True)
password_hash=r.stdout.strip();assert password_hash.startswith('$2')
# One transaction for all migrations, privileges and initial sports-only account.
body='\n'.join(p.read_text().replace('BEGIN;','').replace('COMMIT;','') for p in files)
settings=json.dumps({'name':'Reservas deportivas','timezone':'America/Bogota','currency':'COP','toleranceMinutes':15,'waterMinutes':30,'warningMinutes':5})
bootstrap=f"""
CREATE ROLE sports_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS PASSWORD {quote(cfg['password'])};
GRANT CONNECT ON DATABASE neondb TO sports_runtime;
GRANT USAGE ON SCHEMA sports TO sports_runtime;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA sports TO sports_runtime;
REVOKE UPDATE ON sports.audit_events,sports.loyalty_ledger FROM sports_runtime;
REVOKE INSERT,UPDATE ON sports.schema_history FROM sports_runtime;
INSERT INTO sports.tenants(id,settings) VALUES({quote(cfg['tenantId'])},{quote(settings)}::jsonb);
INSERT INTO sports.users(tenant_id,id,username,name,password_hash,role) VALUES({quote(cfg['tenantId'])},{quote(cfg['adminId'])},'admin','Administrador',{quote(password_hash)},'ADMIN');
"""
# Abort the same transaction if the new role could access sales tables or create in public.
checks_sql="""
DO $$ BEGIN
IF EXISTS(SELECT 1 FROM pg_tables WHERE schemaname='public' AND (has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'SELECT') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'INSERT') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'UPDATE') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'DELETE'))) OR has_schema_privilege('sports_runtime','public','CREATE') THEN RAISE EXCEPTION 'Sports role is not isolated'; END IF;
END $$;
"""
sql('BEGIN;\n'+body+bootstrap+checks_sql+'\nCOMMIT;')
after=sql(fingerprint);assert before==after,'Sales schema fingerprint differs; investigate before deployment'
runtime={**env,'PGUSER':cfg['role'],'PGPASSWORD':cfg['password']}
assert sql("SELECT count(*) FROM sports.users",runtime)=='1'
report={'target':name,'host':host,'salesSchemaBefore':before,'salesSchemaAfter':after,'salesTables':21,'sportsTables':int(sql("SELECT count(*) FROM pg_tables WHERE schemaname='sports'")),'versions':sql("SELECT string_agg(version::text,',' ORDER BY version) FROM sports.schema_history"),'migrations':checks}
save(OUT/(name+'-verified.json'),report)
settingsfile=OUT/(name+'-koyeb.env')
settingsfile.write_text('SPRING_PROFILES_ACTIVE=sports\nSPORTS_DB_URL=jdbc:postgresql://'+host.replace('.c-','-pooler.c-')+'/neondb?sslmode=require\nSPORTS_DB_USER='+cfg['role']+'\nSPORTS_DB_PASSWORD='+cfg['password']+'\nLOGGING_LEVEL_COM_PORTAFOLIO=INFO\nLOGGING_LEVEL_ORG_SPRINGFRAMEWORK=INFO\n')
settingsfile.chmod(0o600)
print(json.dumps(report,indent=2));print('Credentials saved privately in .local/sports-release; no passwords printed.')
