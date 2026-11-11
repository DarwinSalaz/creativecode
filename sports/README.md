# Módulo deportivo — implementación aislada

Deportes se compila dentro del mismo JAR Kotlin/Spring Boot de ventas. El frontend Ionic/Angular puede empaquetarse en ese JAR: un servidor, un proceso Java, un despliegue. El módulo no importa clases de ventas y no contiene entidades JPA: utiliza JDBC con tablas calificadas `sports.*` y transacciones propias.

## Estado y límite de autorización

Implementación con esquema deportivo aislado. V001–V004 están versionadas; V004 conserva el UUID interno y permite un alias público para iniciar sesión. El módulo sigue desactivado por defecto en la configuración normal y se habilita únicamente con el perfil `sports`.

Ya funcionan login y reservas con persistencia real local. Ver [inicio y acceso local](../docs/sports/LOCAL_SETUP.md). Las pruebas originales sin BD siguen disponibles; ahora también hay pruebas explícitas de integración local. La validación productiva y la regresión completa de ventas siguen pendientes.

## Límites de dominio

- `Domain.kt`: entidades y puerto `SportsStore`.
- `BookingService.kt`: casos de uso, transiciones, agenda, tolerancia y concurrencia.
- `RecurrenceService.kt`: reglas semanales, materialización por ventana, conflictos, excepciones y finalización de series.
- `PostgresSportsStore.kt`: adaptador JDBC, transacciones y auditoría.
- `SportsAuth.kt`: identidad deportiva independiente; BCrypt coste 12, sesiones opacas de 256 bits, hash SHA-256 en almacenamiento, expiración de 12 horas, cuentas desactivables.
- `SportsAccessInterceptor.kt`: autenticación, contexto de tenant y control de origen exclusivamente para rutas deportivas.
- `SportsConfiguration.kt`: pool privado (máximo 2 conexiones, mínimo 0); no publica un bean `DataSource`, `UserDetailsService`, `AuthenticationManager` ni `PasswordEncoder` que reemplace los de ventas.
- `../sports-web`: frontend independiente, sin dependencias del frontend legado ni plugins nativos. No se cachean respuestas API; el token permanece en `sessionStorage`.
- `migrations/`: SQL revisable fuera del classpath; no existe ejecutor automático.

## Compilar y probar sin base de datos

Desde la raíz del repositorio, con el JDK local ya existente:

```sh
JAVA_HOME="$PWD/.local/jdks/temurin8-x64/Contents/Home" ./gradlew test :sports:test
cd sports-web
npm ci --include=optional --ignore-scripts
npm test
npm run build
npm run test:browser
```

El test de navegador utiliza Chrome instalado en macOS; en otros sistemas configurar `CHROME_BIN`. Usa un servidor estático local efímero y rutas interceptadas; no arrancar Spring para esta prueba. Las capturas son de **datos simulados**, no de operación productiva.

## Paquete único

Después de compilar la web, desde la raíz:

```sh
JAVA_HOME="$PWD/.local/jdks/temurin8-x64/Contents/Home" ./gradlew bootJar \
  -PpackageSportsWeb -PisolatedBuildDir=/private/tmp/creativecode-sports-package
```

`-PisolatedBuildDir` conserva intacto `build/libs/creativecode-1.0.jar`. El paquete queda en `/private/tmp/creativecode-sports-package/libs/creativecode-1.0.jar`. El Dockerfile existente copia el JAR de `build/libs`; promover el paquete verificado a esa ruta es una acción de entrega posterior, no parte de estas pruebas.

No arrancar ese JAR contra la base real antes de autorizar y preparar la estructura y las cuentas. Compilar no exige credenciales y no inicia conexiones.

## Configuración de activación (pendiente)

El perfil Spring `sports` activa el módulo y sirve:

- Ventas: `/api/portfolio/...` (URLs públicas conservadas).
- Deportes: `/api/sports/v1/...`.
- Frontend: `/sports/index.html`.

El perfil cambia el contexto servlet a raíz y aplica `/api/portfolio` solamente a controladores del paquete legado. El perfil normal conserva exactamente el contexto original. Antes del despliegue real hay que validar también filtros de infraestructura, health checks, recursos estáticos y reglas del proxy con el perfil nuevo.

Variables, **sin valores secretos**:

| Variable | Uso |
|---|---|
| `SPRING_PROFILES_ACTIVE=sports` | Activación deliberada del perfil conjunto |
| `SPORTS_DB_URL` | JDBC PostgreSQL pooled para ejecución, SSL requerido |
| `SPORTS_DB_USER` | Rol deportivo limitado al esquema `sports` |
| `SPORTS_DB_PASSWORD` | Credencial deportiva propia |
| `SPORTS_ALLOWED_ORIGINS` | Orígenes exactos separados por coma; no comodines |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Ventas: conservar sus valores actuales |

En Koyeb, incluir el origen HTTPS público en `SPORTS_ALLOWED_ORIGINS`, incluso sirviendo la web desde el mismo dominio: el proxy puede terminar TLS antes de Tomcat. Verificar OPTIONS y login en ensayo.

No usar el propietario/migrador de la BD como rol deportivo. Otorgar únicamente USAGE de `sports`, SELECT/INSERT/UPDATE en sus tablas operativas y SELECT/INSERT en auditoría. Sin DELETE, DDL ni acceso a `public`. `schema_history` es solo lectura para ejecución. Los privilegios por defecto de `PUBLIC` también deben revisarse en la futura validación; el esquema por sí solo no garantiza aislamiento de permisos.

## Reglas y decisiones

- Toda consulta y modificación incluye tenant. El tenant y rol provienen de la sesión, nunca de los IDs enviados por la UI. FK compuestas evitan referencias cruzadas.
- Reservas, clases, fijos y bloqueos comparten una tabla para consultar conflictos de manera uniforme. Los bloqueos administrativos requieren administrador, incluso para editarlos/cancelarlos.
- Cada modificación de agenda bloquea la fila de cancha con `FOR UPDATE` antes de consultar solapamientos. Cambios de cancha bloquean ambos IDs en orden estable. La comprobación se repite dentro de la transacción. Una sesión activa por cancha está respaldada por índice único.
- Ediciones usan `version`; una vista desactualizada recibe 409. Inicio/fin/agua y confirmaciones repetidos son idempotentes. Creación de una reserva normal no tiene clave de idempotencia: ante respuesta perdida, actualizar antes de recrear.
- Intervalos semiabiertos `[inicio, fin)`. Pendientes bloquean. Cancelaciones y no asistencia liberan. Partidos finalizados conservan ocupación histórica.
- Cronómetro calculado desde inicio real y duración programada; no modifica la agenda y no finaliza automáticamente. Tolerancia referida siempre al horario original.
- «Finalizar e iniciar siguiente» son dos acciones explícitas e idempotentes. Si falla la segunda, la interfaz informa que la primera finalizó y el siguiente no quedó confirmado. No finge una transacción atómica entre ambas peticiones.
- Agua +30, aviso 5 minutos antes y fin, configurables. Preferencia de sonido por dispositivo que inicia; activación manual. Abrir varias pestañas en el mismo dispositivo puede duplicar avisos; no hay traspaso automático ni garantías con navegador suspendido/cerrado.
- El frontend consulta al abrir/cambiar vistas y cada 30 segundos en una pestaña visible. No hay solicitudes por segundo. El reloj usa referencia del servidor y tiempo monotónico local. No se guardan ediciones offline.
- Referencia horaria entre semana 16:00–23:00; fuera de ella requiere casilla de autorización. Configuración avanzada por día/cancha queda diferida.
- Tarifa: importe fijo por reserva según cancha, día y franja de **inicio**. No prorratea duraciones variables ni suma franjas cruzadas. La instantánea se conserva cuando solo cambia el nombre/notas. Revisar esta política con el negocio antes de configurar precios reales.
- Series: repetición semanal desde el formulario de reserva, para fijas y clases, con fecha final opcional. Generación inicial de hasta 93 días y materialización idempotente de ventanas futuras al consultar la agenda; cancelación y edición de una fecha, reemplazo y finalización administrativa de la serie. Ver [flujo y garantías](../docs/sports/RECURRENCE.md).
- Profesores: clases manuales y convenio mensual en notas, sin facturación. Clientes ocasionales no crean contactos automáticamente.
- WhatsApp solo se abre por acción humana después de editar el mensaje. Esa acción comparte nombre/teléfono/mensaje con WhatsApp; no se envía automáticamente.

## Pendientes y alcance parcial

- Validación básica SQL, concurrencia de reservas y roles: aprobada en PostgreSQL local. Restauración, escenarios restantes y pruebas productivas: pendientes.
- Provisionamiento local de negocio, sede, dos canchas y tres cuentas: completado con contraseña aleatoria. El provisionamiento productivo sigue pendiente.
- Seguridad existente de ventas no se corrigió ni se reutilizó; debe tratarse como trabajo separado.
- PWA: manifest y diseño responsive preparados. Sin service worker ni promesa de instalación completa/offline; Capacitor futuro.
- Auditoría básica append-only desde la aplicación; no hay visor avanzado ni historial before/after completo.
- Importaciones, pagos, facturación, portal público y notificaciones nativas no implementados.
- Recuperación de contraseña/autogestión, rotación de sesiones y limpieza de sesiones expiradas requieren una siguiente entrega operativa; actualmente cuentas creadas/desactivadas por administrador.
- Controles de acceso a infraestructura compartida, capacidad y compatibilidad Java 17 de producción aún deben comprobarse; el esquema separado no aísla CPU, memoria ni reinicios.

Ver [plan de extracción](../EXTRACTION_PLAN.md), [operación y aprobación](../docs/sports/RELEASE_CHECKLIST.md) y [contrato API](../docs/sports/openapi.json).

Las reservas pertenecen a personas: `guestName` conserva el nombre del cliente como identificación principal. `teamName` es opcional y solo se muestra en el formulario de detalle. Seleccionar un cliente registrado utiliza su nombre. V002 añade ese campo únicamente al esquema deportivo; se aplicó solo en la base local.

## Promociones por cliente

En Clientes → Promoción, un administrador configura 5 o 10 partidos y beneficio del 50% o 100%, plazo de racha y estado activo. Empleados consultan progreso e historial. Solo cuentan reservas normales/fijas vinculadas al cliente y finalizadas desde la activación; no se importan asistencias anteriores ni clases. Las series pueden vincularse a un cliente.

El canje ocurre al finalizar el siguiente partido: se conserva el precio base y se calcula el valor de referencia final, sin registrar cobros. Una cancelación no consume el beneficio; uno ganado no caduca ni se acumula. Partidos simultáneos se serializan por cliente: el primero en finalizar usa el beneficio. El partido bonificado reinicia el ciclo y no suma al siguiente.

Una racha sin beneficio ganado vence tras el máximo de días entre finalizaciones. La consulta muestra el avance efectivo; el reinicio queda registrado al finalizar otro partido. Una prórroga justificada hasta fecha futura permite conservar/restaurar el ciclo todavía no reiniciado. Pausar suspende conteos y canjes, conserva el avance, pero no congela el plazo de racha. Meta y porcentaje no cambian mientras haya avance o beneficio pendiente.

Historial inmutable con actor, fecha, reserva y estado posterior; configuración y excepciones usan versión optimista. Bloqueo de cliente, transacción común con finalización e índice único impiden canjes/conteos duplicados. V003 es aditiva, exclusivamente deportiva; aplicada solo en localhost.
