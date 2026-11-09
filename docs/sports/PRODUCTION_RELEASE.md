## Promociones: avance anterior y descuento visible — 9 noviembre 2026

Publicación autorizada por el usuario. Permite al administrador ajustar el avance del ciclo con motivo e historial; resalta los descuentos en calendario, pendientes, operación y reserva. Conserva los precios históricos.

- Sin migraciones ni modificaciones de datos productivos para esta publicación.
- Validación: 82 pruebas Kotlin, 11 frontend y pruebas Chrome con datos simulados y PostgreSQL local. Las 229 entradas de clases/configuración de ventas permanecen idénticas.
- JAR anterior: `.local/sports-release/pre-loyalty-progress-20261109.jar`. Reversión de código mediante ese JAR, conservando los datos.
- Despliegue automático por GitHub/master. Verificación del hash público y lecturas de ventas, promociones y caja mediante `scripts/sports/verify-finance-public.py`.

# Actualización de saldos administrativos — 8 noviembre 2026

- Publicación de código únicamente: sin migraciones ni escrituras para transformar datos de producción.
- Los administradores ven su efectivo a cargo y el traslado opcional. Los empleados conservan el proceso de entrega.
- Nueva consulta autenticada `GET /api/sports/v1/finance/accounts/{id}/statement?month=AAAA-MM`: usa el libro y las asociaciones de cierres existentes; no modifica fotografías mensuales ni reescribe movimientos.
- Traslado administrativo opcional habilitado para dinero recibido, aun sin cierres propios.
- 80 pruebas Kotlin y pruebas locales de PostgreSQL y navegador aprobadas. Verificación pública mediante `verify-finance-public.py`, incluyendo el nuevo extracto y las rutas de ventas.
- JAR previo conservado privadamente en `.local/sports-release/pre-finance-statement-20261108.jar`.

---

# Publicación de Caja y cierres — 5 noviembre 2026

- Autorizada por el usuario. V005 aplicada en producción con nueve tablas `sports.finance_*`, sin datos ficticios.
- Ensayo en la rama temporal `br-empty-term-b7i9btbd` (expira 12 noviembre, 23:59 UTC), incluyendo ejecución del JAR conjunto con Java 17 y el rol `sports_runtime`.
- Verificación dentro de una transacción con snapshot estable: ninguna fila previa de ventas o deportes modificada por la migración. Estructura y permisos de las 21 tablas de ventas conservaron la huella `dd4569add018ca448e1e1eb778064a0f`.
- Paquete probado: `build/libs/creativecode-1.0.jar`. Publicación por el flujo automático de Koyeb al actualizar `master`. No se cambian variables, credenciales ni el arranque de ventas.
- URL: https://creative-code-darwinsalaz.koyeb.app/sports/index.html
- Verificación pública: `python3 scripts/sports/verify-finance-public.py --wait 600`; comprueba hash del bundle, estado/lectura de productos de ventas y endpoints deportivos autenticados. Evidencia privada: `.local/sports-release/finance-public-verified.json`.
- Respaldo del JAR remoto anterior: `.local/sports-release/pre-finance-deployed.jar`. Para revertir el código, desplegar ese JAR y conservar las tablas financieras; no restaurar toda la base de ventas.
- Límites operativos de la primera versión documentados en [FINANCE.md](FINANCE.md).

---

## Registro histórico de la primera publicación

# Publicación deportiva — 4 noviembre 2026

Autorizada por el usuario: crear las tablas deportivas y publicar el módulo sin alterar ventas.

## Base de datos preparada

Aplicadas V001–V004 en la rama productiva de Neon. V004 agrega un alias público de acceso sin modificar el UUID interno ni sus relaciones; el negocio inicial usa `centenario`. Se creó el esquema `sports` con 14 tablas, un rol SQL `sports_runtime` limitado y un negocio con administrador. No se copiaron datos ficticios locales. Las 21 tablas de ventas conservan estructura y permisos (huella antes/después `dd4569add018ca448e1e1eb778064a0f`). El rol deportivo no puede leer/escribir tablas de ventas ni crear objetos en `public`.

Ensayo previo sobre rama independiente con PostgreSQL 18 y Java 17 aprobado: autenticación deportiva, cancha, reserva pendiente, confirmación, inicio y finalización; lectura de status y productos de ventas mediante sus rutas originales.

Respaldo previo sin cómputo: `sports-pre-release-20261104` (`br-sweet-grass-b7sfg98t`), expira el 11 de noviembre de 2026 a las 23:00 UTC. No restaurar toda la base para deshacer un problema deportivo.

## Activación pendiente en Koyeb

1. Agregar las variables del archivo privado `.local/sports-release/production-koyeb.env` al servicio existente. No modificar `DB_URL`, `DB_USER`, `DB_PASSWORD`, puerto ni ruta de salud de ventas. Si ya hay perfiles activos, conservarlos y agregar `sports`.
2. Configurar `SPORTS_ALLOWED_ORIGINS` con el origen HTTPS público exacto si el navegador envía Origin. No usar comodín. Obtener URL del servicio para verificarlo antes del despliegue.
3. Subir el JAR preparado en `build/libs/creativecode-1.0.jar` mediante el flujo GitHub habitual. Esto dispara el despliegue automático; hacerlo después de confirmar las variables.
4. Comprobar `/api/portfolio/status`, lectura de ventas, `/sports/index.html` y login deportivo `/api/sports/v1/auth/login`.
5. El acceso inicial está en `.local/sports-release/production-access.txt`. Configurar canchas y tarifas reales desde Administración; no hay reservas o canchas de prueba en producción.

El JAR anterior y el manifiesto de hashes están en `.local/sports-release/`. Para retornar, restaurar JAR y configuración anteriores; conservar las tablas deportivas. La activación comparte proceso con ventas y su despliegue reinicia el servicio.

Credenciales excluidas de Git. No se ha subido el JAR ni verificado publicación pública mientras faltan configuración Koyeb y URL del servicio.
