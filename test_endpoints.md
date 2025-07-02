# Nuevos Endpoints de Gastos

## 1. Listar Gastos con Filtros

### Endpoint
```
GET /api/portfolio/expense/list
```

### Parámetros de Query (todos opcionales)
- `wallet_id`: ID de la cartera
- `start_date`: Fecha de inicio (formato: YYYY-MM-DD)
- `end_date`: Fecha de fin (formato: YYYY-MM-DD)
- `expense_type`: Tipo de gasto

### Ejemplo de uso
```bash
curl -X GET "http://localhost:5000/api/portfolio/expense/list?wallet_id=1&start_date=2024-01-01&end_date=2024-12-31&expense_type=Gasolina" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Respuesta
```json
[
  {
    "expense_id": 1,
    "expense_type": "Gasolina",
    "value": 50000,
    "expense_date": "2024-01-15T00:00:00",
    "justification": "Combustible para moto de reparto",
    "wallet_id": 1,
    "username": "juan_perez"
  }
]
```

## 2. Eliminar Gasto

### Endpoint
```
DELETE /api/portfolio/expense/delete/{expense_id}
```

### Ejemplo de uso
```bash
curl -X DELETE "http://localhost:5000/api/portfolio/expense/delete/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Respuesta
```json
{
  "success": true,
  "message": "Gasto eliminado exitosamente"
}
```

## Características Implementadas

### Filtros del Listado
- **wallet_id**: Filtra por cartera específica
- **start_date**: Filtra gastos desde esta fecha (inclusive)
- **end_date**: Filtra gastos hasta esta fecha (inclusive)
- **expense_type**: Filtra por tipo de gasto específico

### Seguridad
- Autenticación JWT requerida en ambos endpoints
- Validación de usuario antes de permitir operaciones

### Eliminación Segura
- Elimina el gasto de la tabla `expenses`
- Elimina el movimiento de caja asociado de `cash_movements`
- Actualiza los valores del control de caja activo
- Mantiene la integridad referencial

### Ordenamiento
- Los gastos se ordenan por fecha de gasto descendente (más recientes primero)

### Manejo de Errores
- Respuestas HTTP apropiadas para diferentes escenarios
- Mensajes descriptivos en caso de error
- Validación de parámetros de entrada 