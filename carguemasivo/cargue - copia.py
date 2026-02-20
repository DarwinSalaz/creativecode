import pandas as pd
import requests
import json
from datetime import datetime
import argparse

# Constantes
WALLET_ID = 2
BASE_URL = 'https://creative-code-darwinsalaz.koyeb.app'
API_HEADERS = {'Content-Type': 'application/json'}
AUTH_TOKEN = 'eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJzb2Z0dGVrSldUIiwic3ViIjoicmFsb3MiLCJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzI5NjI4NDgyLCJleHAiOjE3Mjk2MjkwODJ9.POfPe3y_ZARKEcq108TijuJ1TYzFqhNn7r0sBkLgcsPpYVbgVbD13GwIIm8zP6lARZLOPXblM-rGvJ319thA1g'  # Token de autenticación
ERROR_COLUMN = 'error_message'

# Función para consultar productos y almacenarlos en memoria
def get_products():
    url = f"{BASE_URL}/api/portfolio/products?sort=name,asc"
    response = requests.post(url, headers=API_HEADERS, json={"wallet_ids": [WALLET_ID]})
    
    if response.status_code == 200:
        
        return {product['name'].lower(): product for product in response.json()['products']}
    else:
        #print(f"Error fetching products: {response.text}")
        raise Exception(f"Error fetching products: {response.text}")

# Función para registrar un cliente y devolver su ID
def register_customer(row):
    row['observation'] = row['observation'] if pd.notna(row['observation']) else ""
    row['address'] = row['address'] if pd.notna(row['address']) else ""

    url = f"{BASE_URL}/api/portfolio/customer/create"
    customer_data = {
        "company_id": 1,
        "name": row['name'],
        "last_name": row['last_name'],
        "cellphone": row['cellphone'],
        "email": row['email'],
        "address": row['address'],
        "identification_number": row['identification_number'],
        "active": True,
        "gender": row['gender'],
        "observation": row['observation'],
        "wallet_id": WALLET_ID
    }
    response = requests.post(url, headers=API_HEADERS, json=customer_data)
    if response.status_code == 200 or response.status_code == 201:
        #print(f"Success registering customer: {response.json().get('customer_id')}")
        return response.json().get('customer_id')
    else:
        raise Exception(f"Error registering customer: {response.text}")

# Función para registrar una venta (servicio)
def register_service(row, customer_id, products_info):
    product_data = []
    
    # Separar productos y cantidades de la fila
    product_names = str(row['product_name']).split('|')
    product_quantities = str(row['product_quantity']).split('|')
    
    for i in range(len(product_names)):
        product_name = product_names[i].strip().lower()
        product_quantity = int(product_quantities[i].strip())

        if product_name in [p.strip().lower() for p in products_info]:
            product = products_info[product_name]
            product_data.append({
                "product_id": product['product_id'],
                "value": product['value'],
                "quantity": product_quantity,
                "name": product['name']
            })
        else:
            print(f"Product '{product_names[i]}' not found in product list")
            raise Exception(f"Product '{product_names[i]}' not found in product list")

    # Headers con el token de autorización
    service_headers = {
        'Content-Type': 'application/json',
        'Authorization': f'{AUTH_TOKEN}'  # Aquí se agrega el token en el header
    }

    date_str = row['next_payment_date']  # Esto es una cadena en formato MM/DD/YYYY

    # Convertir la fecha de cadena a un objeto datetime
    try:
        next_payment_date = pd.to_datetime(date_str, format='%m/%d/%Y')

        # Convertir la fecha a formato requerido por el servicio
        next_payment_date_str = next_payment_date.strftime('%Y-%m-%dT%H:%M:%S')
        
    except ValueError as e:
        # Manejar errores de formato de fecha
        raise Exception(f"Error en la conversión de la fecha: {str(e)}")

    url = f"{BASE_URL}/api/portfolio/service/create"
    service_data = {
        "application_user_id": 48,  # Aquí va el usuario que registra el servicio
        "service_value": row['valor_servicio'],
        "down_payment": row['cuota_inicial'],
        "discount": row['descuento'],
        "total_value": row['valor_total'],
        "debt": row['deuda'],
        "days_per_fee": row['dias_cuota'],
        "quantity_of_fees": row['nro_cuotas'],
        "fee_value": row['valor_cuota'],
        "initial_payment": row['abono'],
        "wallet_id": WALLET_ID,
        "has_products": True,
        "customer_id": customer_id,
        "state": "created",
        "observations": "cargue_automatico",
        "next_payment_date": next_payment_date_str,
        "service_products": product_data
    }

    response = requests.post(url, headers=service_headers, json=service_data)
    if response.status_code == 200:
        
        return response.json()
    else:
        raise Exception(f"Error registering service: {response.text}")

# Función principal para procesar el archivo
def process_file(file_path):
    # Leer archivo Excel/CSV
    df = pd.read_excel(file_path) if file_path.endswith('.xlsx') else pd.read_csv(file_path, sep=';')

    # Agregar columna de error si no existe
    if ERROR_COLUMN not in df.columns:
        df[ERROR_COLUMN] = None

    # Obtener la información de los productos
    try:
        print(f"Consultando productos...")
        products_info = get_products()
        print(f"Consulta de productos exitosa")

        # Procesar cada fila del archivo
        for idx, row in df.iterrows():
            try:
                print(f"Registrando cliente...")
                # Registrar cliente
                customer_id = register_customer(row)

                print(f"Registro exitoso ID: {customer_id}")

                try:
                    print(f"Registrando servicio...")
                    # Registrar servicio con los productos
                    register_service(row, customer_id, products_info)
                    print(f"Servicio registrado para cliente {customer_id}")
                except Exception as service_error:
                    # Registrar error en la columna cuando ocurre en register_service
                    error_message = f"Error en 'register_service': {str(service_error)}"
                    print(error_message)
                    df.at[idx, ERROR_COLUMN] = error_message
            
            except Exception as customer_error:
                # Registrar error en la columna cuando ocurre en register_customer
                error_message = f"Error en 'register_customer': {str(customer_error)}"
                print(error_message)
                df.at[idx, ERROR_COLUMN] = error_message
    
    except Exception as product_error:
        # Registrar error en la columna cuando ocurre al consultar los productos
        error_message = f"Error en 'get_products': {str(product_error)}"
        print(error_message)
        df[ERROR_COLUMN] = error_message
    
    # Guardar el archivo con los errores si los hubo
    df.to_excel(str('corrected-')+str(file_path), index=False) if (str('corrected-')+str(file_path)).endswith('.xlsx') else df.to_csv(str('corrected-')+str(file_path), index=False, sep=';')


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Procesar archivo Excel o CSV")
    parser.add_argument("file_path", help="Ruta del archivo Excel o CSV a procesar")
    args = parser.parse_args()

    process_file(args.file_path)
