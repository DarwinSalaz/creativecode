# Entorno local independiente

Autorizado el 3 de noviembre de 2026. No se utiliza Neon, ni se cargan credenciales de producción, ni se han copiado datos reales.

Se reutilizó PostgreSQL 14.21 ya instalado por Homebrew. No fue necesario instalar PostgreSQL ni Docker. La compatibilidad con PostgreSQL 18 de producción deberá validarse en el proceso de entrega.

| Elemento | Ubicación |
|---|---|
| Instancia PostgreSQL propia | `127.0.0.1:55432` |
| Base local | `sports_local` |
| Archivos del cluster | `.local/sports-postgres` |
| Socket Unix exclusivo | `.local/sports-postgres-socket` |
| Aplicación conjunta local | `http://localhost:5002/sports/index.html` |
| Credenciales y negocio | `.local/sports-local-access.txt` (solo lectura/escritura del propietario) |
| Configuración privada | `.local/sports-local.json` (ignorada por Git) |

## Abrir y reiniciar

La app se sirve directamente desde el JAR conjunto; no es necesario `npm start` para utilizar esta versión.

Desde `/Users/darwin/code/creativecode`, después de un reinicio:

```sh
./scripts/sports/local-db.sh start
python3 scripts/sports/run-local.py
```

Dejar la segunda terminal abierta. Abrir `http://localhost:5002/sports/index.html`.

Usar `admin`, `empleado1` o `empleado2`, con el identificador de negocio y contraseña del archivo `.local/sports-local-access.txt`. Las tres cuentas iniciales comparten una contraseña generada exclusivamente para este entorno local; no se usa en producción.

El script de inicio usa exclusivamente credenciales locales y descarta variables DB/SPORTS/SPRING heredadas. Nunca carga `.env`. El JAR actual está en `/private/tmp/creativecode-sports-package/libs/creativecode-1.0.jar`; si el sistema limpia esa carpeta, reconstruir según `sports/README.md`.

Detener la app con Ctrl+C en la terminal que la inició. Detener la base después:

```sh
./scripts/sports/local-db.sh stop
```

## Preparación inicial (ya realizada)

```sh
./scripts/sports/local-db.sh start
python3 scripts/sports/setup-local.py
```

El setup comprueba que el socket pertenece exactamente a `.local/sports-postgres` antes de escribir. Crea roles, base, migraciones V001, V002 y V003, negocio, sede, dos canchas y tres usuarios ficticios. Comprueba el checksum de la migración al repetirlo; no borra ni reinicia datos existentes.

El rol `sports_app` solo accede a `sports`; no tiene USAGE sobre `public`, ni UPDATE sobre auditoría, ni DELETE sobre reservas. El rol `sales_local_app` no tiene USAGE sobre `sports`. El propietario del cluster es el usuario local del sistema y se conecta por socket Unix; los roles de aplicación usan contraseñas SCRAM por TCP.

**El esquema y datos de ventas no se han copiado a esta base.** El proceso conjunto arranca, pero todavía no se han probado flujos reales de ventas en este entorno. Es necesario preparar fixtures independientes para esa regresión completa.

## Pruebas reales locales

Con el backend y PostgreSQL iniciados:

```sh
python3 scripts/sports/smoke-local.py
cd sports-web
node tests/browser-local.mjs
```

Estas pruebas sí generan registros ficticios en la base local. No apuntan a URLs configurables ni toman conexiones de `.env`.

Verificado: login real; permisos de empleado; seis intentos de reservar simultáneamente producen un éxito y cinco conflictos; inicio y fin idempotentes; separación de permisos entre esquemas. El navegador prueba login, creación persistente y visualización de una reserva. Los datos de prueba se conservan localmente.

Pendiente: fixtures de ventas, aislamiento funcional con dos tenants reales, restauración de backup, más carreras de cambios/cancelaciones, y audio en dispositivos reales. Producción sigue sin modificaciones y no está autorizada para migraciones ni altas.
