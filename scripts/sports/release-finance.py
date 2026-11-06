#!/usr/bin/env python3
"""Explicit V005 release; existing deployments only. No seeds, legacy writes or startup DDL."""
import argparse,datetime,hashlib,json,os,pathlib,shlex,subprocess,urllib.parse
ROOT=pathlib.Path(__file__).resolve().parents[2]
OUT=ROOT/'.local/sports-release'
PROD='ep-winter-band-b7vwqo6z.c-13.us-east-1.aws.neon.tech'
STAGE='ep-flat-star-b7qm5x8s.c-13.us-east-1.aws.neon.tech'
BRANCH='br-empty-term-b7i9btbd'

def credentials():
 values={}
 for line in (ROOT/'.env').read_text().splitlines():
  if '=' in line and not line.lstrip().startswith('#'):
   k,v=line.split('=',1);values[k.strip()]=shlex.split(v)[0] if v else ''
 url=urllib.parse.urlparse(values['DB_URL'].removeprefix('jdbc:'))
 assert url.hostname.replace('-pooler','')==PROD and url.path=='/neondb'
 return values

def environment(host,runtime=False):
 values=credentials();env={k:v for k,v in os.environ.items() if not k.startswith('PG')}
 user,password=values['DB_USER'],values['DB_PASSWORD']
 if runtime:
  cfg=json.loads((OUT/'production-credentials.json').read_text());user,password=cfg['role'],cfg['password']
 env.update(PGHOST=host,PGPORT='5432',PGDATABASE='neondb',PGUSER=user,PGPASSWORD=password,PGSSLMODE='require',PGCONNECT_TIMEOUT='15',PGOPTIONS='-c statement_timeout=60000 -c lock_timeout=5000')
 return env

def sql(statement,host,runtime=False):
 r=subprocess.run(['psql','-X','-At','-v','ON_ERROR_STOP=1'],input=statement,text=True,capture_output=True,env=environment(host,runtime))
 if r.returncode:raise RuntimeError('Release SQL failed: '+r.stderr.splitlines()[0])
 return r.stdout.strip()

def save(path,value):
 path.write_text(json.dumps(value,indent=2)+'\n');path.chmod(0o600)

# Function exists only in the session's temporary schema. Rows never leave PostgreSQL.
GUARD="""
CREATE FUNCTION pg_temp.existing_fingerprint() RETURNS text LANGUAGE plpgsql AS $$
DECLARE r record; h text; parts text:='';
BEGIN
 FOR r IN SELECT schemaname,tablename FROM pg_tables WHERE schemaname='public' OR (schemaname='sports' AND tablename NOT LIKE 'finance_%' AND tablename<>'schema_history') ORDER BY schemaname,tablename LOOP
  EXECUTE format($q$SELECT md5(coalesce(string_agg(h,'' ORDER BY h),'')) FROM (SELECT md5(row_to_json(t)::text) h FROM %I.%I t) x$q$,r.schemaname,r.tablename) INTO h;
  parts:=parts||r.schemaname||'.'||r.tablename||':'||h;
 END LOOP;
 RETURN md5(parts);
END $$;
CREATE TEMP TABLE finance_release_guard AS SELECT pg_temp.existing_fingerprint() AS rows;
"""
SCHEMA="""SELECT md5(string_agg(item,E'\n' ORDER BY item)) FROM (
SELECT row_to_json(t)::text item FROM (SELECT table_name,column_name,data_type,is_nullable,column_default,ordinal_position FROM information_schema.columns WHERE table_schema='public') t
UNION ALL SELECT row_to_json(t)::text FROM (SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public') t
UNION ALL SELECT row_to_json(t)::text FROM (SELECT c.relname,k.conname,pg_get_constraintdef(k.oid) definition FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public') t
UNION ALL SELECT row_to_json(t)::text FROM (SELECT grantee,table_name,privilege_type,is_grantable FROM information_schema.role_table_grants WHERE table_schema='public') t
UNION ALL SELECT nspname||coalesce(nspacl::text,'') FROM pg_namespace WHERE nspname='public') s"""
TABLES=['finance_settings','finance_accounts','finance_receipts','finance_days','finance_handovers','finance_handover_days','finance_entries','finance_months','finance_commands']

def main():
 p=argparse.ArgumentParser();p.add_argument('--production',action='store_true');p.add_argument('--inspect',action='store_true');a=p.parse_args()
 host=PROD if a.production else STAGE;name='production' if a.production else 'rehearsal'
 migration=ROOT/'sports/migrations/V005__daily_finance.sql';digest=hashlib.sha256(migration.read_bytes()).hexdigest()
 versions=sql("SELECT string_agg(version::text,',' ORDER BY version) FROM sports.schema_history",host)
 before=sql(SCHEMA,host)
 if a.inspect:
  print(json.dumps({'target':name,'host':host,'versions':versions,'salesSchemaFingerprint':before,'salesTables':sql("SELECT count(*) FROM pg_tables WHERE schemaname='public'",host)}));return
 record=OUT/('finance-'+name+'-verified.json')
 if versions=='1,2,3,4,5':
  assert record.exists() and json.loads(record.read_text())['sha256']==digest,'Existing V005 lacks matching release evidence; inspect before retry'
  print(name+': V005 already applied; no writes');return
 assert versions=='1,2,3,4','Unexpected migration history'
 assert sql("SELECT count(*) FROM pg_roles WHERE rolname='sports_runtime'",host)=='1'
 if a.production:
  evidence=json.loads((OUT/'finance-rehearsal-verified.json').read_text())
  assert evidence['sha256']==digest and evidence['rowsUnchanged'] and evidence['salesSchemaBefore']==evidence['salesSchemaAfter']
  smoke=json.loads((OUT/'finance-rehearsal-smoke.json').read_text())
  assert smoke['sha256']==digest and smoke['passed'] and smoke['jarSha256']==hashlib.sha256(pathlib.Path('/private/tmp/creativecode-sports-package/libs/creativecode-1.0.jar').read_bytes()).hexdigest()
 body=migration.read_text().replace('BEGIN;','').replace('COMMIT;','')
 grants='GRANT SELECT,INSERT ON '+','.join('sports.'+t for t in TABLES)+' TO sports_runtime;\nGRANT UPDATE ON sports.finance_settings,sports.finance_handovers TO sports_runtime;\nREVOKE UPDATE,DELETE,TRUNCATE ON '+','.join('sports.'+t for t in TABLES if t not in ['finance_settings','finance_handovers'])+' FROM sports_runtime;'
 guard_after="""DO $$ BEGIN
 IF (SELECT rows FROM finance_release_guard)<>pg_temp.existing_fingerprint() THEN RAISE EXCEPTION 'Existing sports or sales rows changed'; END IF;
 IF EXISTS(SELECT 1 FROM pg_tables WHERE schemaname='public' AND (has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'SELECT') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'INSERT') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'UPDATE') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'DELETE'))) THEN RAISE EXCEPTION 'Sports role has legacy privileges'; END IF;
 END $$;
"""
 # Stable snapshot ignores unrelated legitimate live writes, while detecting any writes by this migration.
 sql('BEGIN ISOLATION LEVEL REPEATABLE READ;\n'+GUARD+'\n'+body+'\n'+grants+'\n'+guard_after+'\nCOMMIT;',host)
 after=sql(SCHEMA,host);assert before==after,'Legacy metadata changed; inspect before publishing'
 assert sql("SELECT count(*) FROM sports.finance_entries",host,True)=='0'
 evidence={'target':name,'host':host,'branch':None if a.production else BRANCH,'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sha256':digest,'salesSchemaBefore':before,'salesSchemaAfter':after,'rowsUnchanged':True,'financeTables':9,'versions':'1,2,3,4,5'}
 save(record,evidence);print(json.dumps(evidence))
if __name__=='__main__':main()
