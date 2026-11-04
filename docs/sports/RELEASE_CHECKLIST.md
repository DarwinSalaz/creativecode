> Actualización 4 noviembre: tablas deportivas productivas creadas con autorización; publicación del JAR pendiente de variables Koyeb. Ver [estado de publicación](PRODUCTION_RELEASE.md). Las restricciones anteriores se conservan abajo como registro histórico.

# Activación controlada — NO ejecutada

No desplegar como operativo ni afirmar P0 validado en producción antes de completar esta lista. El permiso actualizado permite estructura y datos ficticios en la instancia local exclusiva; V001–V003 ya se aplicaron allí. Producción continúa sin autorización para escrituras o cambios de estructura. Ver LOCAL_SETUP.md.

## 1. Preparación sin escrituras (realizable ahora)

- Compilar módulo y frontend; ejecutar tests unitarios, aislamiento de rutas y navegador simulado.
- Construir JAR conjunto en carpeta aislada. Comprobar que contiene `sports` y `static/sports/index.html`.
- Verificar que cambios de ventas se limitan al enlace del módulo y configuración opt-in.
- Presentar V001–V003, permisos, estrategia de respaldo, alta inicial y riesgos pendientes para aprobación.

## 2. Entorno de ensayo (requiere aprobación de BD)

- Usar una base/branch de ensayo autorizada. No apuntar tests a producción. Reducir datos personales en las copias.
- Aplicar V001, V002 y V003 en orden y en transacciones por conexión directa y verificar `schema_history`.
- Crear rol limitado, negocio, sede y cuentas individuales; contraseñas aportadas por canal seguro, hashes generados con BCrypt 12. Nunca valores predeterminados en código.
- Verificar que el rol deportivo no puede leer/escribir tablas de `public`, crear objetos ni modificar/borrar auditoría.
- Ejecutar carreras reales: misma cancha/horario (solo una alta), cancelación vs inicio, cambio entre canchas, dos inicios, doble fin, generación simultánea de series.
- Validar que una serie con conflicto revierte todas las instancias; tenant A no ve ni modifica B, incluso alterando IDs de usuarios, clientes, profesores, canchas y reservas.
- Ejecutar una restauración de respaldo en una base descartable. Verificar filas, FK, versiones y cuentas. No tomar una mención histórica de backup como prueba vigente.

## 3. Despliegue (pendiente)

- Identificar versión/artefacto que realmente corre en Koyeb. Guardar imagen/JAR anterior y configuración efectiva.
- Medir CPU, RAM, pool de ventas, cuotas de Neon y latencia. El módulo agrega hasta 2 conexiones: no aumentar límites a ciegas.
- Revisar el puerto efectivo (Spring 5000 vs Docker EXPOSE 8080), comando de arranque, Java 17, health check y suspensión por inactividad.
- Respaldar la base antes de migrar; proteger archivos de backup y verificar restauración.
- Con aprobación, aplicar únicamente `sports`, provisionar las cuentas y configurar variables deportivas. Mantener valores DB de ventas.
- Promover exactamente el JAR aprobado; activar perfil `sports`. Web en `/sports/index.html`; ambos módulos en el mismo proceso.
- Antes/después: verificar endpoints de lectura de ventas, autenticación y flujos de ventas en ensayo. Las pruebas productivas que generen registros requieren autorización explícita y datos controlados.
- Validar P0 en dispositivos reales: calendario compartido, partidos independientes, tolerancia, agua, audio visible/fondo, recarga, corte de red y reconexión.

## 4. Retorno

- Si falla el código: restaurar el JAR/imagen y configuración anteriores. No revertir ni borrar tablas deportivas automáticamente. Si solo se desactiva deportes, restaurar también el perfil/contexto de ventas de forma coordinada.
- No restaurar toda la base para resolver un fallo deportivo: podría destruir ventas posteriores al respaldo.
- Mantener el esquema deportivo como datos conservados. Cualquier restauración selectiva se ensaya y requiere aprobación, conciliando reservas posteriores al backup.
- El cambio de proceso reinicia ambas aplicaciones; coordinar ventana de bajo uso. No existe aislamiento físico mientras comparten servidor.

## Cuentas iniciales

El bootstrap aún requiere una operación autorizada. V001 no inserta tenants ni usuarios. No hay endpoint público de registro/creación de negocios. Preparar un alta segura por operador con identificadores UUID y hash BCrypt coste 12; después el administrador puede crear los otros usuarios desde la interfaz. Esta operación se realizó solo en la base local aislada.
