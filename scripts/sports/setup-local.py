#!/usr/bin/env python3
"""Provision ONLY the dedicated localhost cluster; never reads the project's .env."""
import getpass, hashlib, json, os, pathlib, secrets, subprocess
ROOT=pathlib.Path(__file__).resolve().parents[2]
LOCAL=ROOT/'.local'
SOCKET=LOCAL/'sports-postgres-socket'
DB='sports_local'
PORT='55432'
env={k:v for k,v in os.environ.items() if not k.startswith('PG')}
def query(sql,db='postgres'):
    r=subprocess.run(['psql','-X','-h',str(SOCKET),'-p',PORT,'-U',getpass.getuser(),'-d',db,'-v','ON_ERROR_STOP=1','-At'],input=sql,text=True,capture_output=True,env=env)
    if r.returncode:
        # Never print SQL or parameters: they can contain local credentials.
        raise SystemExit('Local SQL failed. No remote connection was attempted. '+r.stderr.splitlines()[0].split('STATEMENT:')[0])
    return r.stdout.strip()
def quote(s):return "'"+s.replace("'","''")+"'"
actual=pathlib.Path(query('SHOW data_directory;')).resolve()
if actual!=(LOCAL/'sports-postgres').resolve():raise SystemExit('Refusing: wrong PostgreSQL data directory')
path=LOCAL/'sports-local.json'
if path.exists(): config=json.loads(path.read_text())
else:
    config={'sportsPassword':secrets.token_urlsafe(30),'salesPassword':secrets.token_urlsafe(30),'loginPassword':secrets.token_urlsafe(18)}
    path.write_text(json.dumps(config,indent=2)+'\n');path.chmod(0o600)
for role,key in [('sports_app','sportsPassword'),('sales_local_app','salesPassword')]:
    if not query('SELECT 1 FROM pg_roles WHERE rolname='+quote(role)+';'):
        query('CREATE ROLE '+role+' LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '+quote(config[key])+';')
if not query('SELECT 1 FROM pg_database WHERE datname='+quote(DB)+';'):query('CREATE DATABASE '+DB+';')
query('REVOKE ALL ON DATABASE sports_local FROM PUBLIC; GRANT CONNECT ON DATABASE sports_local TO sports_app,sales_local_app; REVOKE ALL ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO sales_local_app;',DB)
migration=ROOT/'sports/migrations/V001__sports.sql'
digest=hashlib.sha256(migration.read_bytes()).hexdigest()
if not query("SELECT 1 FROM pg_namespace WHERE nspname='sports';",DB):
    query(migration.read_text(),DB)
    config['migrationSha256']=digest
    path.write_text(json.dumps(config,indent=2)+'\n');path.chmod(0o600)
else:
    if config.get('migrationSha256')!=digest:raise SystemExit('Migration checksum changed; prepare a new version, do not overwrite the schema')
    if query('SELECT version FROM sports.schema_history ORDER BY version;',DB) not in ('1','1\n2','1\n2\n3','1\n2\n3\n4','1\n2\n3\n4\n5','1\n2\n3\n4\n5\n6'):raise SystemExit('Unexpected sports migration history')
migration2=ROOT/'sports/migrations/V002__optional_team_name.sql'
digest2=hashlib.sha256(migration2.read_bytes()).hexdigest()
if not query('SELECT 1 FROM sports.schema_history WHERE version=2;',DB):
    query(migration2.read_text(),DB)
    config['migrationV002Sha256']=digest2
    path.write_text(json.dumps(config,indent=2)+'\n');path.chmod(0o600)
elif config.get('migrationV002Sha256')!=digest2:
    raise SystemExit('V002 checksum changed; prepare a new migration')
migration3=ROOT/'sports/migrations/V003__client_loyalty.sql'
digest3=hashlib.sha256(migration3.read_bytes()).hexdigest()
if not query('SELECT 1 FROM sports.schema_history WHERE version=3;',DB):
    query(migration3.read_text(),DB)
    config['migrationV003Sha256']=digest3
    path.write_text(json.dumps(config,indent=2)+'\n');path.chmod(0o600)
elif config.get('migrationV003Sha256')!=digest3:
    raise SystemExit('V003 checksum changed; prepare a new migration')
migration4=ROOT/'sports/migrations/V004__tenant_login_slug.sql'
digest4=hashlib.sha256(migration4.read_bytes()).hexdigest()
if not query('SELECT 1 FROM sports.schema_history WHERE version=4;',DB):
    query(migration4.read_text(),DB)
    config['migrationV004Sha256']=digest4
    path.write_text(json.dumps(config,indent=2)+'\n');path.chmod(0o600)
elif config.get('migrationV004Sha256')!=digest4:
    raise SystemExit('V004 checksum changed; prepare a new migration')
migration5=ROOT/'sports/migrations/V005__daily_finance.sql'
digest5=hashlib.sha256(migration5.read_bytes()).hexdigest()
if not query('SELECT 1 FROM sports.schema_history WHERE version=5;',DB):
    query(migration5.read_text(),DB)
    config['migrationV005Sha256']=digest5
    path.write_text(json.dumps(config,indent=2)+'\n');path.chmod(0o600)
elif config.get('migrationV005Sha256')!=digest5:
    raise SystemExit('V005 checksum changed; prepare a new migration')
migration6=ROOT/'sports/migrations/V006__administrator_investments.sql'
digest6=hashlib.sha256(migration6.read_bytes()).hexdigest()
if not query('SELECT 1 FROM sports.schema_history WHERE version=6;',DB):
    query(migration6.read_text(),DB)
    config['migrationV006Sha256']=digest6
    path.write_text(json.dumps(config,indent=2)+'\n');path.chmod(0o600)
elif config.get('migrationV006Sha256')!=digest6:
    raise SystemExit('V006 checksum changed; prepare a new migration')
query('''GRANT USAGE ON SCHEMA sports TO sports_app;
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA sports TO sports_app;
REVOKE UPDATE,DELETE ON sports.finance_entries,sports.finance_days,sports.finance_months,sports.finance_commands,sports.finance_handover_days,sports.finance_receipts FROM sports_app;
REVOKE UPDATE,DELETE ON sports.investments,sports.investment_contributions FROM sports_app;
REVOKE UPDATE ON sports.audit_events FROM sports_app;
REVOKE UPDATE,DELETE ON sports.loyalty_ledger FROM sports_app;
REVOKE INSERT,UPDATE ON sports.schema_history FROM sports_app;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
''',DB)
tenant='11111111-1111-4111-8111-111111111111'
settings=json.dumps({'name':'Canchas · pruebas locales','timezone':'America/Bogota','currency':'COP','toleranceMinutes':15,'waterMinutes':30,'warningMinutes':5})
query("INSERT INTO sports.tenants(id,slug,settings) VALUES ("+quote(tenant)+","+quote(tenant)+","+quote(settings)+"::jsonb) ON CONFLICT DO NOTHING;",DB)
for suffix,name,role in [('1','admin','ADMIN'),('2','empleado1','EMPLOYEE'),('3','empleado2','EMPLOYEE')]:
    uid='22222222-2222-4222-8222-22222222222'+suffix
    query("INSERT INTO sports.users(tenant_id,id,username,name,password_hash,role) VALUES ("+','.join([quote(tenant),quote(uid),quote(name),quote('Prueba '+name),'public.crypt('+quote(config['loginPassword'])+",public.gen_salt('bf',12))",quote(role)])+") ON CONFLICT DO NOTHING;",DB)
query("INSERT INTO sports.venues(tenant_id,id,name) VALUES ('"+tenant+"','55555555-5555-4555-8555-555555555555','Sede local') ON CONFLICT DO NOTHING;",DB)
for uid,name,color in [('33333333-3333-4333-8333-333333333333','Cancha 1','#167d69'),('44444444-4444-4444-8444-444444444444','Cancha 2','#bc8053')]:
    query("INSERT INTO sports.courts(tenant_id,id,venue_id,name,sport,capacity,duration_minutes,color,active) VALUES ('"+tenant+"',"+quote(uid)+",'55555555-5555-4555-8555-555555555555',"+quote(name)+",'Fútbol 5',10,60,"+quote(color)+",true) ON CONFLICT DO NOTHING;",DB)
access=LOCAL/'sports-local-access.txt'
access.write_text('SOLO PRUEBAS LOCALES\nURL: http://localhost:5002/sports/index.html\nNegocio: '+tenant+'\nUsuarios: admin, empleado1, empleado2\nContraseña local: '+config['loginPassword']+'\n')
access.chmod(0o600)
print('PASS: isolated sports_local on port 55432; migrations V001-V006; 3 local accounts and 2 courts. Access details: .local/sports-local-access.txt')
