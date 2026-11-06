# Caja y cierres deportivos

Implementación del módulo `sports.finance`. Interfaz: **Caja y cierres**. Usa las sesiones y usuarios deportivos; no utiliza entidades, tablas, cuentas ni servicios del sistema de ventas.

## Flujo operativo

1. El administrador registra cuentas bancarias y su titular: negocio, empleado o administrador. La base inicial es 150.000 COP y se puede configurar para futuros cierres.
2. Quien realiza el cierre registra fecha, ingresos en efectivo y transferencias por cuenta receptora. Hay una sola caja y un único cierre diario por negocio. Se puede registrar un día sin ingresos.
3. **El efectivo del cierre es el ingreso bruto del día:** excluye la base y dinero retenido de otros días; incluye lo que posteriormente se registra como gasto. Si se calcula desde el dinero físico restante, se deben sumar los gastos del día pagados con ese efectivo para reconstruir el ingreso. No existe conciliación automática con reservas, pues también hay tienda.
4. El efectivo se asigna a quien registra el cierre. Las transferencias se asignan directamente a la cuenta seleccionada. El efectivo puede permanecer varios días con el empleado.
5. En «Dinero y entregas» el empleado selecciona al administrador y solicita entregar **todo su saldo de efectivo**, descontados los gastos ya registrados. Se agrupan todos sus cierres todavía sin entrega. El dinero sigue bajo su responsabilidad hasta la aceptación.
6. Mientras existe una solicitud pendiente, no se pueden registrar gastos ni ajustes sobre ese efectivo. Sí se pueden ingresar nuevos cierres: estos quedan para una siguiente entrega.
7. Solo el administrador designado puede aceptar. Si recibe menos, debe explicar la diferencia; se registra como faltante histórico atribuible al efectivo del remitente. Se descuenta la totalidad declarada del remitente, se acredita lo realmente recibido al administrador y no queda una deuda abierta por el faltante. No se permite declarar más de lo esperado.
8. Los gastos requieren fecha, cuenta, importe y concepto. El comprobante PDF/PNG/JPEG de hasta 2 MB es opcional. Se descuenta del saldo elegido, sin usar la base. Los empleados pueden gastar de sus cuentas y de cuentas del negocio; los administradores pueden registrar gastos de cualquier saldo del negocio. Se exige saldo disponible, incluyendo el efecto sobre movimientos posteriores si el gasto tiene fecha anterior.
9. El resumen mensual muestra ingresos en efectivo y transferencias, gastos, faltantes, ajustes, cierres diarios, movimientos, entregas y saldos por responsable. Las entregas internas no cuentan como ingresos nuevos. El resultado del mes y los saldos acumulados son conceptos diferentes.
10. El administrador puede cerrar un mes que ya terminó, con nota y confirmación de revisión. Debe verificar manualmente que estén todos los días operados; la aplicación muestra cuántos cierres hay y no inventa días faltantes. Se conserva una fotografía del informe. Quedan bloqueadas las fechas de ese mes y todas las anteriores, incluso si un mes anterior no se cerró individualmente.
11. Después del cierre mensual, las correcciones requieren un ajuste de administrador en una fecha abierta, importe positivo/negativo y justificación vinculada al mes cerrado. La fotografía original no cambia. Se pueden aceptar en el mes actual entregas de cierres del mes anterior.

Los cierres diarios y movimientos guardados no se editan ni eliminan. Revisar importes antes de confirmar. La versión inicial no incluye anulación de entregas pendientes, traspasos bancarios entre cuentas ni exportación contable. Si un receptor se desactiva con una entrega pendiente, un administrador debe reactivarlo para que pueda aceptarla.

## Aislamiento y persistencia

- Puerto `FinanceStore`, servicio de dominio y adaptador `PostgresFinanceStore` independientes del dominio de ventas. El adaptador reutiliza únicamente el pool privado y el gestor de transacciones de deportes.
- Nueve tablas nuevas en `sports`, con prefijo `finance_`; claves foráneas compuestas por negocio y UUID. Comprobantes almacenados en `sports.finance_receipts`, no en tablas ni archivos de ventas.
- Migración explícita `sports/migrations/V005__daily_finance.sql`. No se ejecuta al arrancar. No modifica relaciones de ventas ni sus datos.
- Escrituras financieras serializadas por fila de negocio y ejecutadas en una transacción, incluyendo el resultado de idempotencia. UUID de comando obligatorio; reutilizarlo con otros datos/actor devuelve 409.
- Libro de movimientos, cierres diarios, fotografías mensuales y comandos son inmutables mediante triggers y permisos. El usuario de ejecución no necesita DDL ni DELETE.
- El cliente conserva el UUID de una solicitud fallida en `sessionStorage`, separado por actor, negocio y operación, para reintentar sin duplicar el movimiento.
- La base fija se conserva como referencia en cada cierre; no se incorpora al libro de ingresos/saldos.
- Para extraer el módulo, mover `sports` y su pool o sustituir `FinanceStore`; no hay dependencias hacia servicios de ventas. La identidad deportiva es la única dependencia compartida con reservas.

## API

Prefijo habitual `/api/sports/v1/finance`, bajo autenticación deportiva. Todos los POST JSON llevan `id` UUID.

| Método y ruta | Datos adicionales / resultado |
|---|---|
| GET `/` | fecha del negocio, base, personas sin credenciales, cuentas/saldos, cierres del actor pendientes de agrupación, entregas |
| GET `/report?month=AAAA-MM` | informe abierto o fotografía cerrada |
| POST `/accounts` | `name`, `ownerId` opcional (null = negocio); administrador |
| POST `/base` | `amount`; administrador |
| POST `/days` | `day`, `cash`, `transfers:[{accountId,amount}]`, `note` opcional |
| POST `/handovers` | `receiverId`; calcula todo el efectivo disponible del actor |
| POST `/handovers/{id}/accept` | UUID de comando nuevo, `received`, `note`; administrador receptor |
| POST `/expenses` | `day`, `accountId`, `amount`, `note`, `receiptId` opcional |
| POST `/months` | `month`, `note`; administrador |
| POST `/adjustments` | `day`, `accountId`, `amount` firmado, `note`, `relatesToMonth`; administrador |
| POST `/receipts` | multipart `file`; devuelve UUID del comprobante |
| GET `/receipts/{id}` | descarga autenticada del mismo negocio, `no-store`, attachment |

## Prueba y publicación

Local: `http://localhost:5002/sports/index.html`. Acceso habitual en `.local/sports-local-access.txt` (archivo privado). La configuración local solo conecta a `127.0.0.1:55432/sports_local` y no carga `.env`.

- `python3 scripts/sports/setup-local.py`: aplica V005 únicamente al clúster local verificado, ajusta permisos y conserva los datos de prueba existentes.
- `python3 scripts/sports/finance-local.py`: crea un negocio ficticio independiente, prueba API/PostgreSQL y compara esquema/filas locales de ventas antes/después. Guarda tokens solo en `.local/sports-finance-test.json` privado.
- Desde `sports-web`: `node tests/finance-local.mjs`, después del script anterior y con el paquete local actualizado. Ejecuta flujos reales de empleado y administrador y valida anchos de 320, 390 y 760 px. Cada repetición completa debe partir de un negocio nuevo generado por el script anterior.
- Las pruebas Kotlin cubren reglas financieras, permisos, reintentos, gastos retroactivos, cierres y aislamiento. Las pruebas PostgreSQL verifican además concurrencia, restricciones y persistencia real.

**Publicación autorizada el 5 de noviembre de 2026:** V005 aplicada a producción después de un ensayo sobre una rama de la base productiva. El JAR conjunto ensayado con Java 17 se entrega en `build/libs/creativecode-1.0.jar` mediante el despliegue automático de `master`. URL: https://creative-code-darwinsalaz.koyeb.app/sports/index.html.

La publicación incremental utiliza `release-finance.py` (ensayo primero, `--production` después) y `finance-rehearsal.py` para validar el JAR con Java 17. `verify-finance-public.py --wait 600` comprueba que el bundle servido corresponda al JAR y verifica ventas, autenticación y lecturas financieras sin crear movimientos reales. Las evidencias y los JAR anteriores se conservan en `.local/sports-release/`. No usar el antiguo `release-db.py` para V005. No hay reversión mediante borrado de tablas: ante un problema se revierte el código y se conservan los registros financieros.
