#!/usr/bin/env python3
"""Explicit authorized V006 release. Existing rows are guarded inside one stable snapshot."""
import argparse,datetime,hashlib,importlib.util,json,pathlib
ROOT=pathlib.Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('finance_release',ROOT/'scripts/sports/release-finance.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
STAGE='ep-muddy-cloud-b7fkob2e.c-13.us-east-1.aws.neon.tech'
BRANCH='br-delicate-water-b79pl7d9'
BACKUP='br-autumn-cherry-b78hsr0v'
JAR=pathlib.Path('/private/tmp/creativecode-investments-package/libs/creativecode-1.0.jar')
GUARD=base.GUARD.replace("tablename NOT LIKE 'finance_%' AND tablename<>'schema_history'","tablename NOT IN ('investments','investment_contributions','schema_history')")
TABLES='sports.investments,sports.investment_contributions'
def main():
 p=argparse.ArgumentParser();p.add_argument('--production',action='store_true');p.add_argument('--inspect',action='store_true');a=p.parse_args()
 host=base.PROD if a.production else STAGE;name='production' if a.production else 'rehearsal'
 path=ROOT/'sports/migrations/V006__administrator_investments.sql';digest=hashlib.sha256(path.read_bytes()).hexdigest()
 versions=base.sql("SELECT string_agg(version::text,',' ORDER BY version) FROM sports.schema_history",host)
 before=base.sql(base.SCHEMA,host)
 if a.inspect:print(json.dumps({'target':name,'host':host,'versions':versions,'salesSchemaFingerprint':before}));return
 record=base.OUT/('investments-'+name+'-verified.json')
 if versions=='1,2,3,4,5,6':
  assert record.exists() and json.loads(record.read_text())['sha256']==digest,'Already applied without matching evidence; inspect before retry'
  print('V006 already applied and verified; no writes');return
 assert versions=='1,2,3,4,5','Unexpected migration history'
 if a.production:
  evidence=json.loads((base.OUT/'investments-rehearsal-verified.json').read_text())
  smoke=json.loads((base.OUT/'investments-rehearsal-smoke.json').read_text())
  assert evidence['sha256']==digest and evidence['rowsUnchanged']
  assert smoke['passed'] and smoke['sha256']==digest and smoke['jarSha256']==hashlib.sha256(JAR.read_bytes()).hexdigest()
 body=path.read_text().replace('BEGIN;','').replace('COMMIT;','')
 grants='GRANT SELECT,INSERT ON '+TABLES+' TO sports_runtime; REVOKE UPDATE,DELETE,TRUNCATE ON '+TABLES+' FROM sports_runtime;'
 guard_after="""DO $$ BEGIN
 IF (SELECT rows FROM finance_release_guard)<>pg_temp.existing_fingerprint() THEN RAISE EXCEPTION 'Existing rows changed'; END IF;
 IF EXISTS(SELECT 1 FROM pg_tables WHERE schemaname='public' AND (has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'SELECT') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'INSERT') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'UPDATE') OR has_table_privilege('sports_runtime',format('%I.%I',schemaname,tablename),'DELETE'))) THEN RAISE EXCEPTION 'Sports role has sales access'; END IF;
 END $$;"""
 base.sql('BEGIN ISOLATION LEVEL REPEATABLE READ;\n'+GUARD+'\n'+body+'\n'+grants+'\n'+guard_after+'\nCOMMIT;',host)
 after=base.sql(base.SCHEMA,host);assert before==after,'Sales metadata changed'
 assert base.sql('SELECT count(*) FROM sports.investments',host,True)=='0'
 evidence={'target':name,'host':host,'branch':None if a.production else BRANCH,'backupBranch':BACKUP,'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sha256':digest,'salesSchemaBefore':before,'salesSchemaAfter':after,'rowsUnchanged':True,'newTables':2,'versions':'1,2,3,4,5,6'}
 base.save(record,evidence);print(json.dumps(evidence))
if __name__=='__main__':main()
