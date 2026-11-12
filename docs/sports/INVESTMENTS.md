# Inversiones externas de administradores

En el menú **Inversiones**, exclusivamente para administradores, se registra fecha, categoría (equipos, mejoras, arreglos u otros), concepto, importe en COP y uno o varios aportantes. Cada aportante debe ser administrador activo del mismo negocio; sus importes positivos deben sumar exactamente el total. Se admiten fechas históricas, no futuras.

La pantalla muestra el total histórico invertido y el acumulado de cada administrador, ordenado de mayor a menor. El filtro mensual afecta únicamente al historial y su subtotal; el acumulado continúa siendo histórico. Cada movimiento se expande para consultar concepto completo, aportantes, autor y fecha de registro. Los administradores desactivados conservan su participación histórica.

## Separación de caja y ventas

Los aportes son dinero externo. No crean ingresos, gastos, entregas ni saldos en el libro de caja y no cambian cierres mensuales. Una compra pagada con dinero de caja sigue registrándose en Caja y cierres; no debe volver a registrarse como inversión externa.

Código dentro del módulo `sports`, paquete `investments`, con puerto `InvestmentStore` y adaptador PostgreSQL. Reutiliza identidad, negocio, auditoría y pool deportivo; no importa entidades ni repositorios de ventas. Todas las consultas llevan `tenant_id`; las claves foráneas compuestas impiden relacionar aportantes de otro negocio. La autorización ADMIN se verifica en el servicio, además de ocultar el menú a empleados.

Creación transaccional bajo bloqueo del negocio. El UUID del comando permite reintentos sin duplicar; reutilizarlo con otros datos o autor devuelve 409. La interfaz conserva el comando en sessionStorage durante un reintento. Los documentos y sus aportes son inmutables: esta primera versión no permite editar ni borrar inversiones. Auditoría `INVESTMENT_CREATED` con autor y referencia.

## Contrato

- `GET /api/sports/v1/investments?month=YYYY-MM`: registros del mes (omitir para todos), `periodTotal`, total histórico `total`, `byAdministrator`, administradores y fecha del negocio.
- `POST /api/sports/v1/investments`: `{id, day, category, description, amount, contributions:[{adminId, amount}]}`. Devuelve `{input, actorId, createdAt}`. Importe positivo, máximo 999999999999.99, dos decimales; concepto 1–2000 caracteres; 1–50 aportantes sin repetir.
- 401 sin sesión, 403 para empleado, 400 datos inválidos, 409 conflicto de idempotencia/persistencia.

## Preparación y publicación

V006 crea únicamente `sports.investments` y `sports.investment_contributions`, sus índices y protecciones contra edición/borrado. Requiere V001–V005. El backend no aplica migraciones automáticamente. El rol de ejecución necesita SELECT e INSERT en ambas tablas; no UPDATE ni DELETE.

V006 se aplicó al PostgreSQL local dedicado (`sports_local`, puerto 55432) y, con autorización del usuario, a producción el 11 de noviembre de 2026 (12 de noviembre UTC), después del ensayo aislado con Java 17. `scripts/sports/setup-local.py` comprueba directorio de datos y checksums y prepara el entorno local. El ensayo y la migración productiva comprobaron que las filas anteriores, la estructura y los permisos de ventas permanecen iguales. Ver PRODUCTION_RELEASE.md para respaldo y comprobación pública.

Pruebas: `InvestmentServiceTest`, `python3 scripts/sports/investments-local.py` y, en `sports-web`, `node tests/investments-local.mjs`. La integración crea un negocio desechable local; compara el libro financiero y los datos de ventas antes/después. La prueba de navegador usa sus sesiones locales, por lo que debe ejecutarse después de la integración.

Servidor de prueba: http://localhost:5002/sports/index.html. Credenciales locales en `.local/sports-local-access.txt` (no versionado). Paquete preparado fuera del JAR de despliegue, en `/private/tmp/creativecode-investments-package/libs/creativecode-1.0.jar`; se construyó sobre `a922a7b`, preservando cambios concurrentes de ventas en el árbol de trabajo.
