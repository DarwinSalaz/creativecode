import pandas as pd
import requests
import json
from datetime import datetime
import argparse

# Constantes
WALLET_ID = 32
BASE_URL = 'https://creative-code-darwinsalaz.koyeb.app'
API_HEADERS = {'Content-Type': 'application/json'}
AUTH_TOKEN = 'eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJzb2Z0dGVrSldUIiwic3ViIjoicHJ1ZWJhIiwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTc1MjcyMTU4NCwiZXhwIjoxNzUyNzIyMTg0fQ.9zWB2Zpn-w6zaKUm40j__rwAw2CklTQ2XHEzIke1eQY-ckPHBVy36TRYdekflF4QSAYl-GuSl_WpubfsEUU2lA'  # Token de autenticación
ERROR_COLUMN = 'error_message'

def get_scalar(row, field):
    # Devuelve un valor escalar para el campo, nunca una Serie/DataFrame
    if hasattr(row, 'at') and field in row.index:
        value = row.at[field]
    else:
        value = row.get(field, None)
    # Si es Serie/DataFrame, lo convertimos a None
    if hasattr(value, 'shape') and (len(getattr(value, 'shape', [])) > 0 and value.shape[0] > 1):
        return None
    return value

def is_scalar_and_notna(value):
    # Devuelve True si el valor es escalar y no es None ni NaN
    if hasattr(value, 'shape') and (len(getattr(value, 'shape', [])) > 0 and value.shape[0] > 1):
        return False
    if value is None:
        return False
    try:
        if pd.isna(value):
            return False
    except Exception:
        pass
    return True

def validate_customer_row(row):
    errors = []
    for field in ['name']:
        value = get_scalar(row, field)
        if not is_scalar_and_notna(value):
            errors.append(f"Campo obligatorio vacío: {field}")
        elif str(value).strip() == "":
            errors.append(f"Campo obligatorio vacío: {field}")
    return errors

def validate_service_row(row):
    errors = []
    numeric_fields = [
        ('service_value', 'valor_servicio'),
        ('down_payment', 'cuota_inicial'),
        ('discount', 'descuento'),
        ('debt', 'deuda'),
        ('total_value', 'valor_total'),
        ('days_per_fee', 'dias_cuota'),
        ('quantity_of_fees', 'nro_cuotas'),
        ('fee_value', 'valor_cuota'),
        ('pending_fees', 'nro_cuotas'),
    ]
    for field, col in numeric_fields:
        value = get_scalar(row, col)
        if not is_scalar_and_notna(value):
            errors.append(f"Campo obligatorio vacío: {col}")
        elif str(value).strip() == "":
            errors.append(f"Campo obligatorio vacío: {col}")
        else:
            try:
                float(value)
            except Exception:
                errors.append(f"Campo numérico inválido: {col}")
    # customer_id y wallet_id
    for field in ['customer_id']:
        value = get_scalar(row, field)
        if not is_scalar_and_notna(value):
            errors.append(f"Campo obligatorio vacío: {field}")
        elif str(value).strip() == "":
            errors.append(f"Campo obligatorio vacío: {field}")
    return errors

def validate_product_row(product_name, product_quantity, products_info):
    errors = []
    if product_name not in products_info:
        errors.append(f"Producto '{product_name}' no encontrado en la lista de productos")
    try:
        qty = int(product_quantity)
        if qty <= 0:
            errors.append(f"Cantidad inválida para producto '{product_name}': {product_quantity}")
    except Exception:
        errors.append(f"Cantidad inválida para producto '{product_name}': {product_quantity}")
    return errors

# Función para consultar productos y almacenarlos en memoria
def get_products():
    url = f"{BASE_URL}/api/portfolio/products?sort=name,asc"
    response = requests.post(url, headers=API_HEADERS, json={"wallet_ids": [WALLET_ID]})
    if response.status_code == 200:
        return {product['name'].lower(): product for product in response.json()['products']}
    else:
        raise Exception(f"Error fetching products: {response.status_code} - {response.text}")

# Función para registrar un cliente y devolver su ID
def register_customer(row):
    row['observation'] = row['observation'] if pd.notna(row['observation']) else ""
    row['address'] = row['address'] if pd.notna(row['address']) else ""
    cellphone = row.get('cellphone', None)
    if cellphone is None or str(cellphone).strip() == '':
        cellphone = '0'
    customer_data = {
        "company_id": 1,
        "name": row['name'],
        "last_name": row.get('last_name', None),
        "cellphone": cellphone,
        "email": row.get('email', None),
        "address": row.get('address', None),
        "identification_number": row.get('identification_number', None),
        "active": True,
        "gender": row.get('gender', None),
        "observation": row.get('observation', None),
        "wallet_id": WALLET_ID
    }
    url = f"{BASE_URL}/api/portfolio/customer/create"
    response = requests.post(url, headers=API_HEADERS, json=customer_data)
    if response.status_code in [200, 201]:
        return response.json().get('customer_id')
    else:
        raise Exception(f"Error registering customer: {response.status_code} - {response.text}")

# Función para registrar una venta (servicio)
def register_service(row, customer_id, products_info):
    product_data = []
    product_names = str(row['product_name']).split('|')
    product_quantities = str(row['product_quantity']).split('|')
    for i in range(len(product_names)):
        product_name = product_names[i].strip().lower()
        product_quantity = product_quantities[i].strip()
        # Ajuste: comparar quitando espacios en ambos lados
        matched_product = None
        for key, product in products_info.items():
            if product_name == key.strip().lower():
                matched_product = product
                break
        if not matched_product:
            raise Exception(f"Producto '{product_names[i]}' no encontrado en la lista de productos")
        product_errors = validate_product_row(product_name, product_quantity, products_info)
        if product_errors:
            raise Exception("; ".join(product_errors))
        product = matched_product
        product_data.append({
            "product_id": product['product_id'],
            "value": product['value'],
            "quantity": int(product_quantity),
            "name": product['name']
        })
    service_headers = {
        'Content-Type': 'application/json',
        'Authorization': f'{AUTH_TOKEN}'
    }
    date_str = row['next_payment_date']
    try:
        next_payment_date = pd.to_datetime(date_str, format='%m/%d/%Y')
        next_payment_date_str = next_payment_date.strftime('%Y-%m-%dT%H:%M:%S')
    except ValueError as e:
        raise Exception(f"Error en la conversión de la fecha: {str(e)}")
    service_data = {
        "application_user_id": int(row['application_user_id']) if 'application_user_id' in row and pd.notna(row['application_user_id']) else 48,
        "service_value": float(row['valor_servicio']),
        "down_payment": float(row['cuota_inicial']),
        "discount": float(row['descuento']),
        "total_value": float(row['valor_total']),
        "debt": float(row['deuda']),
        "days_per_fee": int(row['dias_cuota']),
        "quantity_of_fees": int(row['nro_cuotas']),
        "fee_value": float(row['valor_cuota']),
        "pending_fees": int(row['nro_cuotas']),
        "initial_payment": float(row['abono']) if 'abono' in row and pd.notna(row['abono']) else 0,
        "wallet_id": WALLET_ID,
        "has_products": True,
        "customer_id": customer_id,
        "state": "created",
        "observations": "cargue_automatico",
        "next_payment_date": next_payment_date_str,
        "service_products": product_data
    }
    url = f"{BASE_URL}/api/portfolio/service/create"
    response = requests.post(url, headers=service_headers, json=service_data)
    if response.status_code == 200:
        return response.json()
    else:
        raise Exception(f"Error registering service: {response.status_code} - {response.text}")

# Función principal para procesar el archivo
def process_file(file_path):
    df = pd.read_excel(file_path) if file_path.endswith('.xlsx') else pd.read_csv(file_path, sep=';')
    if ERROR_COLUMN not in df.columns:
        df[ERROR_COLUMN] = None
    try:
        print(f"Consultando productos...")
        products_info = get_products()
        print(f"Consulta de productos exitosa")
        for idx, row in df.iterrows():
            error_messages = []
            # Validaciones de cliente
            customer_validation_errors = validate_customer_row(row)
            if customer_validation_errors:
                error_messages.extend(customer_validation_errors)
                df.at[idx, ERROR_COLUMN] = " | ".join(error_messages)
                print(f"Fila {idx}: Errores de validación de cliente: {error_messages}")
                continue
            try:
                print(f"Registrando cliente...")
                customer_id = register_customer(row)
                print(f"Registro exitoso ID: {customer_id}")
                row['customer_id'] = customer_id  # Para validación de servicio
                # Validaciones de servicio
                service_validation_errors = validate_service_row(row)
                if service_validation_errors:
                    error_messages.extend(service_validation_errors)
                    df.at[idx, ERROR_COLUMN] = " | ".join(error_messages)
                    print(f"Fila {idx}: Errores de validación de servicio: {service_validation_errors}")
                    continue
                try:
                    print(f"Registrando servicio...")
                    register_service(row, customer_id, products_info)
                    print(f"Servicio registrado para cliente {customer_id}")
                except Exception as service_error:
                    error_message = f"Error en 'register_service': {str(service_error)}"
                    error_messages.append(error_message)
                    df.at[idx, ERROR_COLUMN] = " | ".join(error_messages)
                    print(f"Fila {idx}: {error_message}")
            except Exception as customer_error:
                error_message = f"Error en 'register_customer': {str(customer_error)}"
                error_messages.append(error_message)
                df.at[idx, ERROR_COLUMN] = " | ".join(error_messages)
                print(f"Fila {idx}: {error_message}")
    except Exception as product_error:
        error_message = f"Error en 'get_products': {str(product_error)}"
        print(error_message)
        df[ERROR_COLUMN] = error_message
    output_file = str('corrected-')+str(file_path)                  # Guardar el archivo con los errores si los hubo                                                                            
    if output_file.endswith('.xlsx'):
        df.to_excel(output_file, index=False)
    else:
        df.to_csv(output_file, index=False, sep=';')

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Procesar archivo Excel o CSV")
    parser.add_argument("file_path", help="Ruta del archivo Excel o CSV a procesar")
    args = parser.parse_args()
    process_file(args.file_path)
