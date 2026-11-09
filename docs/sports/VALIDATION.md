# Evidencia de validación local

Actualización 3 de noviembre: se autorizó y preparó una instancia PostgreSQL local exclusiva. Login, permisos, seis reservas concurrentes (1 éxito, 5 conflictos) e inicio/fin idempotentes pasaron contra la BD real local. Ver [detalle y comandos](LOCAL_SETUP.md). Producción sigue sin cambios.

Actualización de reservas por persona: 54 pruebas de backend aprobadas (27 de la raíz y 27 deportivas), compilación frontend y JAR conjunto correctos. Prueba real en Chrome aprobada: crear reserva de Laura Gómez con equipo opcional, verificar que el calendario muestra a Laura y reabrir el detalle para comprobar ambos campos persistidos. V002 aplicada solo a PostgreSQL local. JAR versionado de ventas idéntico a HEAD.

Las secciones siguientes registran la validación inicial anterior a esa autorización.

Fecha de trabajo: 2 de noviembre de 2026 (Colombia; algunas herramientas registran 3 de noviembre UTC).

## Verificado sin acceder a bases de datos

- Backend: 51 pruebas, 0 fallos, 0 errores. Incluye 25 pruebas preexistentes de ventas, 2 de integración/configuración y 24 deportivas.
- Dominio deportivo: solapamiento, franjas adyacentes, independencia entre canchas, pendientes, cancelaciones, inasistencia, IDs de otro tenant, inicio real, tolerancia original, reintentos de agua/fin, estados concurrentes simulados, cambio de cancha, rol para bloqueos, series idempotentes y rollback simulado, horario y tarifa histórica.
- Acceso HTTP: token ausente o de ventas rechazado, login público limitado a POST, identidad derivada de sesión, origen no permitido, preflight permitido y `Cache-Control: no-store`.
- Configuración: deportes desactivado no exige credenciales ni crea runtime/pool. Perfil de rutas mantiene `/api/portfolio` para controladores de ventas y separa `/api/sports/v1`.
- Frontend: compilación de producción Angular/Ionic correcta; 5 pruebas de cálculos de tiempo, alertas y reconstrucción.
- Navegador Chrome: login, formulario de reserva, calendario, panel operativo, diseño móvil sin desbordamiento horizontal y cronómetro que continúa al abortar la red. Transporte API interceptado; 1 escritura en una variable de prueba, **cero conexiones a bases de datos**.
- Paquete: JAR conjunto contiene módulo `sports`, perfil opt-in y frontend bajo `static/sports`; V001 no se incluye en classpath. Se genera en carpeta temporal.
- Integridad local de ventas: JAR versionado `build/libs/creativecode-1.0.jar` idéntico byte por byte a HEAD. Sin modificaciones a controladores, servicios, entidades, repositorios, Dockerfile ni `application.properties` de ventas.

## Capturas (datos simulados)

- [Login escritorio](screenshots/login-desktop.png)
- [Calendario escritorio](screenshots/calendar-desktop.png)
- [Operación escritorio](screenshots/operations-desktop.png)
- [Operación móvil](screenshots/operations-mobile.png)

## No validado y no autorizado todavía

- Ejecución de V001, privilegios efectivos del rol, altas iniciales y SQL contra PostgreSQL real.
- Carreras entre transacciones reales; las pruebas de dominio no sustituyen esa evidencia.
- Arranque del JAR con el entorno productivo Java 17 y configuración real de Koyeb.
- Autenticación/persistencia de punta a punta contra una base preparada.
- Restauración de backups y smoke tests productivos.
- Audio real en los dispositivos del negocio o en segundo plano. El test del navegador valida cronómetro visual, no entrega audible.

Ninguna prueba autoriza afirmar que P0 está listo para operar en producción. La preparación del entorno de ensayo requiere autorización para estructura y registros de BD.

## Selección de horario desde el calendario

Compilación frontend y empaquetado local aprobados. Prueba Chrome con transporte simulado: clic en cancha 2 a las 18:00 completa cancha, inicio y fin; vista móvil con filtro de cancha permite seleccionar 23:00 y calcula el fin al día siguiente. Se mantiene la validación responsive del formulario. Sin migraciones ni escrituras de BD para este cambio.

Selección actualizada a intervalos de una hora; compilación y prueba de navegador aprobadas en escritorio y móvil.

## Alerta sonora prolongada

Secuencia de 12 tonos alternos durante 8,3 segundos, con envolvente suave y sin superposición. Chrome verificó la programación de duración y la detención inmediata al silenciar; compilación frontend y paquete local aprobados. La intensidad percibida debe comprobarse con los altavoces del dispositivo del negocio. Sin cambios de BD.

## Promociones por cliente — noviembre 2026

61 pruebas backend aprobadas: 27 raíz y 34 deportivas. Casos nuevos: cinco partidos y sexto gratis, diez y siguiente al 50%, finalización repetida, cancelación que conserva beneficio, vencimiento, excepción por lluvia, permisos, tenant ajeno, versiones obsoletas, política pausada y exclusión de clases/ocasionales. Compilación Angular y prueba de navegador con transporte simulado aprobadas.

Integración PostgreSQL real local: configuración por administrador, cinco finalizaciones, reintentos, cancelación y dos finalizaciones simultáneas en canchas distintas para el mismo cliente. Solo una canjeó el beneficio; la otra inició el siguiente ciclo. Historial y excepción versionada verificados. V003 aplicada exclusivamente a sports_local; no se conectó a producción.

Chrome contra API local: alta de cliente, configuración de beneficio al 50%, persistencia tras recargar, selección del cliente y aviso de promoción en reserva móvil aprobados. Regresión local: seis reservas concurrentes, permisos, inicio/fin idempotentes e aislamiento de esquemas aprobados. JAR versionado de ventas idéntico a HEAD.

## Corrección de precios históricos

Las reservas cerradas muestran exclusivamente el precio final y porcentaje confirmado por backend; una promoción actual no altera partidos anteriores. 11 pruebas frontend aprobadas y caso Chrome: quinto partido finalizado conserva 90.000 COP cuando el sexto pasa a ser gratis. Se conservan descuentos históricos del 50% y 100% y se evita proyectarlos sobre cancelaciones. Compilación frontend aprobada. Sin migraciones ni escrituras a BD.

## Diseño de tarifas semanales

Panel de ancho completo con filtro de cancha, siete días visibles por cancha, franjas ordenadas, precios completos y estados sin tarifa. Formulario separado de alta/edición con días por nombre y soporte de fin a medianoche. Prueba Chrome con transporte simulado aprobada: 14 días para dos canchas, edición que conserva 24:00, alta posterior sin sobrescribir la tarifa editada, filtro de siete días y contenido sin desbordamiento interno a 320/390 px. Compilación y empaquetado local aprobados; sin cambios de BD.

## Estado inicial de las reservas

Nuevas reservas por formulario y solicitudes API sin estado explícito comienzan en PENDING (Por confirmar). Pruebas backend y navegador aprobadas: se envía PENDING, iniciar sin confirmar se rechaza y confirmar permite iniciar. Reservas existentes conservan su estado. Sin migración de datos.

## Edición de clientes — 5 noviembre 2026

Editar carga una copia de nombre, teléfono y notas conservando el ID. El guardado reutiliza el endpoint deportivo existente y permite cancelar la edición. Chrome contra la base local verificó: crear sin teléfono, configurar promoción, editar desde móvil, guardar teléfono sin duplicar cliente, recargar conservando promoción y autocompletar teléfono en nueva reserva. Compilación y JAR aprobados. Sin migraciones ni escrituras en producción para esta mejora.

## Caja y cierres — 2026-11-05 (solo local)

- 76 pruebas Kotlin pasan: 27 del backend existente y 49 deportivas, incluidas 13 financieras nuevas.
- 11 pruebas JavaScript existentes pasan; smoke de navegador de reservas/tarifas pasa.
- Integración real sobre `127.0.0.1:55432/sports_local`: base excluida, cuentas receptoras, entregas acumuladas, faltante con nota, aceptación idempotente concurrente, cierre diario concurrente (un éxito y un conflicto), gastos, mes cerrado inmutable, ajustes y bloqueo de escrituras históricas. Comparación de metadatos/filas locales de ventas sin cambios.
- V005 aplicada exclusivamente al clúster local. Detalle funcional y pasos de publicación pendientes en [FINANCE.md](FINANCE.md).
- Navegador real con empleado y administrador: cierre con efectivo y transferencia, gasto con PDF y descarga, entrega total, aceptación con faltante, cuenta con titular y resumen mensual. Formularios sin desbordamiento a 320/390/760 px. Capturas: `screenshots/finance-month-desktop.png` y `screenshots/finance-daily-mobile.png`.

## Ensayo y migración productiva de caja — 5 noviembre 2026

- V005 ensayada sobre una rama de producción; JAR conjunto ejecutado con Java 17 y el rol deportivo real. Pasaron autenticación, cierre mixto, reintento idempotente, gasto, entrega, faltante, saldos y reporte mensual; ventas respondió correctamente.
- V005 aplicada en producción con verificación transaccional de filas preexistentes y huella de esquema/permisos de ventas sin cambios. Sin registros financieros ficticios en producción.
- Verificación pública reproducible con `scripts/sports/verify-finance-public.py`; registro privado de resultados en `.local/sports-release/finance-public-verified.json`.

## Saldos administrativos y extracto — 8 noviembre 2026

- 80 pruebas Kotlin pasan: 27 del backend existente y 53 deportivas. Casos nuevos: cierre administrativo sin entrega obligatoria, traslado opcional de dinero recibido sin cierres propios, saldo inicial y final del extracto, cierres del mes anterior y aislamiento por negocio.
- Integración local PostgreSQL/API: consulta con asociaciones históricas, recepción con faltante, saldo tras gastos/ajustes, traslado completo entre administradores y reintento idempotente. Fotografías mensuales cerradas y datos locales de ventas conservados.
- Esta entrega no incorpora ni ejecuta migraciones. El extracto consulta `finance_entries`, `finance_days` y `finance_handover_days` existentes.
- Navegador local: flujo completo de efectivo/transferencias/gastos/recepción conservado. Vista administrativa sin entrega obligatoria, consulta de cierres del mes anterior, cambio de mes y diseño sin desbordamientos a 320/390/760 px. Capturas `finance-statement-desktop.png` y `finance-statement-mobile.png`.
- Comparación de artefactos antes de publicar: todas las clases `com/portafolio` y `application.properties` idénticas al JAR productivo anterior; entre las librerías únicamente cambia `sports.jar`.
