# Reservas semanales

En Nueva reserva, **Fija** crea una serie semanal en el día y horario del inicio seleccionado. En **Escuelita**, seleccionar profesor y activar **Repetir cada semana**. La fecha final es opcional e inclusiva; vacía significa sin vencimiento. El estado inicial predeterminado sigue siendo Por confirmar. Cada serie corresponde a un día de la semana; para varios días se crea una serie por día.

Cada fecha es una reserva independiente: confirmar, iniciar, finalizar, cancelar o editar afecta solo esa instancia. Al cancelarla, **Reservar este horario para otra persona** abre una reserva nueva con cancha y horas preseleccionadas. Mover una fecha deja una excepción para no regenerarla en el horario original.

Administración → Series semanales permite terminar una serie desde hoy o una fecha futura. Cancela únicamente instancias pendientes/confirmadas desde ese día, incluso las consultadas con mucha anticipación. Conserva historial y partidos ya iniciados/finalizados. Solo un administrador puede terminar series; los operadores pueden crearlas como las demás reservas.

Las reservas antiguas marcadas Fija sin serie mantienen su comportamiento individual. El botón **Repetir desde la próxima semana** prepara una serie nueva y conserva la reserva original. Las antiguas series finitas conservan sus instancias existentes y no se regeneran automáticamente.

## Persistencia y aislamiento

- Sin migraciones: usa `sports.recurrence_rules.definition` y las reservas existentes. Campos adicionales del JSON tienen valores por defecto compatibles con reglas antiguas (`automatic=false`).
- Al crear una serie se generan hasta 93 días. Las nuevas reglas guardan `automatic=true`; las ventanas posteriores se materializan transaccionalmente al consultar agenda/operación, sin límite de vigencia artificial ni tareas programadas. Los GET de agenda pueden, por tanto, persistir instancias faltantes de reglas nuevas.
- Cada escritor bloquea las canchas en orden estable. Crear/editar una reserva individual verifica también ocurrencias aún no generadas. Crear series verifica patrones semanales solapados y reservas ya existentes, incluso fuera de los primeros 93 días. Un conflicto rechaza toda la operación.
- Una instancia cancelada permanece guardada y cuenta como generada. Al mover una instancia se conserva la fecha original en `excludedStarts`. La finalización de una serie usa control de versión y registra auditoría.
- Los precios se fijan al generar cada instancia. Editar sin cambiar cancha/horario conserva su precio. Las clases conservan `CLASS` y profesor y no participan en promociones de partidos normales/fijos.
- No se modifican entidades, tablas ni configuración de ventas. No se convierten reservas productivas antiguas automáticamente.

## Validación local

`RecurrenceServiceTest`: horizonte a diez años, idempotencia, cancelación y reemplazo, fecha movida, rango finito de más de 93 días, conflictos remotos y nocturnos, fin de serie, conservación de partido en juego, roles, tenant y compatibilidad de reglas antiguas.

`python3 scripts/sports/recurrence-local.py`: PostgreSQL local, cuatro consultas simultáneas sin duplicados, reglas sin vencimiento, reemplazo, movimiento de fecha, cinco clases semanales de dos horas con profesor, permisos y versión de cierre. Crea una cancha ficticia aislada, termina sus reglas y la desactiva al finalizar.

`cd sports-web && node tests/recurrence-local.mjs`: Chrome contra localhost, creación de serie desde el formulario, siguiente semana, cancelación de una fecha, reemplazo, fin de serie y diseño móvil.

Publicación autorizada por el usuario; seguimiento en PRODUCTION_RELEASE.md.
