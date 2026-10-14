# Conexión a Neon

Proyecto: `crimson-queen-57096693`. Rama: `production`.

El backend conserva PostgreSQL JDBC y Hikari. `application.properties` lee
`DB_URL`, `DB_USER` y `DB_PASSWORD` del entorno. `DB_URL` debe ser una URL JDBC
del endpoint pooled de Neon con `sslmode=require`. Las credenciales locales
están en `.env`, excluido de Git. `.env.example` contiene el formato.

Para ejecutar localmente:

```sh
./scripts/run-local.sh
```

El script usa el JDK 8 local cuando está instalado, porque el Gradle de este
repositorio requiere esa versión. Hibernate mantiene `ddl-auto=none`.

Para desplegar el JAR en el hosting del backend, configurar allí las mismas
tres variables. Modificar el `.env` local no cambia las variables del hosting.
Nunca subir el archivo `.env` al repositorio.

## Restore del 14 de octubre de 2026

Backup: `dump-_b86289ffcfe5-202610141000.backup` (PostgreSQL 14, formato custom).
Restaurado en Neon sin los propietarios ni los permisos del servidor antiguo.
El restore de esquema y datos se ejecutó en una sola transacción. Se conservaron
las secuencias, los índices, las restricciones y el trigger del backup.

Se verificaron las 21 tablas: sus 42.019 registros coinciden, tabla por tabla,
con el backup. Entre ellas: 41 usuarios, 2.384 clientes, 3.100 servicios,
12.742 pagos y 50 carteras. No se migraron datos desde el servidor antiguo en vivo;
la copia corresponde al momento del backup.

La rama `before-backup-restore-20261014` guarda el estado vacío anterior al restore
y expira el 21 de octubre de 2026 a las 10:00 (Colombia).

Validación:

```text
VERIFIED: 21 tables, 42019 rows; all row counts match the backup.
./gradlew test bootJar: BUILD SUCCESSFUL in 32s
Backend tests: 11 tests, 0 failures
POST /api/portfolio/wallets: HTTP 200, 50 restored wallets
```
