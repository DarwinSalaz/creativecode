# Extracción del dominio deportivo

Estado: arquitectura implementada; extracción y restauración no ejecutadas.

## Inventario

El subproyecto `sports` depende de Kotlin, Spring Web/JDBC, Jackson, Hikari y BCrypt. No depende de paquetes `com.portafolio`. `BookingService` depende del puerto `SportsStore` y de un reloj inyectable. Su adaptador PostgreSQL utiliza exclusivamente `sports.*`. Usuarios y sesiones pertenecen a deportes. No hay FK hacia ventas.

El host actual importa `SportsConfiguration` mediante `SportsModuleConfig`, condicionado a `sports.enabled=true`. El frontend consume HTTP mediante `ApiService`, cuya URL base puede cambiar sin modificar casos de uso ni pantallas.

## Secuencia futura

1. Crear un host Kotlin independiente que importe `SportsConfiguration`, con el driver PostgreSQL y versiones compatibles. Conservar `sports.api-path=/api/sports/v1` y los mismos contratos. Eliminar únicamente la integración de prefijos del host de ventas.
2. Preparar una base PostgreSQL propia, un rol propietario para migraciones y otro restringido para ejecución. Conservar nombre de esquema `sports` para no cambiar SQL.
3. En entorno de ensayo autorizado, exportar solo el esquema `sports` por conexión directa y restaurarlo en una base vacía. Usar `pg_dump --format=custom --schema=sports --no-owner --no-acl` y `pg_restore --no-owner --no-acl --single-transaction`. Las conexiones y contraseñas se suministran fuera de argumentos visibles, mediante mecanismo seguro. No ejecutar estos comandos sin autorización.
4. No aplicar V001 sobre un esquema restaurado: restaurar su historial o aplicar migraciones en una base vacía y transferir los datos con un procedimiento separado y probado. Comprobar versiones, recuentos, FK, sesiones activas, auditoría y pruebas de tenant.
5. Trasladar usuarios y hashes BCrypt incluidos en el esquema. Preferir invalidar sesiones existentes y solicitar login en el nuevo backend. No cambiar contraseñas ni reutilizar identidades de ventas.
6. Programar corte breve: detener escrituras deportivas (no las de ventas), realizar exportación final consistente, restaurar, validar y cambiar URL de API o ruta del proxy. Nunca mantener dos escritores independientes para las mismas reservas.
7. Conservar el servicio viejo sin escrituras hasta cerrar la validación. Si se revierte después de haber escrito en el nuevo servicio, conciliar esas escrituras antes del retorno; no cambiar la URL de vuelta ignorando datos nuevos.
8. Desactivar `sports.enabled` en el host original y retirar su integración cuando se haya cerrado el período de retorno. Restituir el contexto servlet de ventas mediante su configuración normal y verificar sus URLs.

## Criterios de aceptación

Los casos de uso deportivos y el frontend no se reescriben. Cambian arranque, empaquetado, URL, credenciales y configuración. El nuevo rol no accede a ventas, y el esquema exportado no requiere objetos del dominio de ventas. Probar autenticación, concurrencia, idempotencia, timestamps, alertas reconstruidas, calendario y aislamiento de tenant antes de cambiar tráfico.
