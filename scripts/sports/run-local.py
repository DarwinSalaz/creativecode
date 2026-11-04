#!/usr/bin/env python3
"""Run the joint backend against localhost only. Does not load .env or inherit remote DB settings."""
import json, os, pathlib
ROOT=pathlib.Path(__file__).resolve().parents[2]
config=json.loads((ROOT/'.local/sports-local.json').read_text())
jar=pathlib.Path('/private/tmp/creativecode-sports-package/libs/creativecode-1.0.jar')
if not jar.exists():raise SystemExit('Build the isolated joint JAR first; see sports/README.md')
env={k:v for k,v in os.environ.items() if not k.startswith(('DB_','SPORTS_','SPRING_','LOGGING_','DEBUG','TRACE','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'))}
env.update({'SPRING_PROFILES_ACTIVE':'sports','DB_URL':'jdbc:postgresql://127.0.0.1:55432/sports_local','DB_USER':'sales_local_app','DB_PASSWORD':config['salesPassword'],'SPORTS_DB_URL':'jdbc:postgresql://127.0.0.1:55432/sports_local','SPORTS_DB_USER':'sports_app','SPORTS_DB_PASSWORD':config['sportsPassword'],'SPORTS_ALLOWED_ORIGINS':'http://localhost:5002,http://127.0.0.1:5002,http://localhost:4200,http://127.0.0.1:4200'})
java=ROOT/'.local/jdks/temurin8-x64/Contents/Home/bin/java'
args=[str(java),'-jar',str(jar),'--server.port=5002','--server.address=127.0.0.1','--debug=false','--trace=false','--logging.level.root=INFO','--logging.level.com.portafolio=INFO','--logging.level.org.springframework=INFO','--spring.jpa.hibernate.ddl-auto=none']
print('Starting LOCAL backend http://localhost:5002/sports/index.html — database 127.0.0.1:55432/sports_local',flush=True)
os.execve(java,args,env)
