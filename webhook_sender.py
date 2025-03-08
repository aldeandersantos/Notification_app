from datetime import datetime
import aiohttp
from log import logger
from db.pool import pool

def format_webhook_message(app_name, ip_address, timestamp):
    # Remove o prefixo 'com.kms.free' do nome do app para exibição
    display_name = app_name.split('.')[-1] if '.' in app_name else app_name
    
    # Formata a data e hora de forma mais legível
    formatted_date = timestamp.strftime("%d/%m/%Y")
    formatted_time = timestamp.strftime("%H:%M:%S")
    
    return {
        "embeds": [{
            "title": display_name.capitalize(),
            "color": 5814783,  # Cor azul bonita
            "fields": [
                {
                    "name": "IP",
                    "value": ip_address,
                    "inline": True
                },
                {
                    "name": "Data",
                    "value": formatted_date,
                    "inline": True
                },
                {
                    "name": "Hora",
                    "value": formatted_time,
                    "inline": True
                }
            ],
            "footer": {
                "text": f"App ID: {app_name}"  # Mantém o nome original do app no footer
            }
        }]
    }

async def send_webhook_notifications(app_name, ip_address):
    timestamp = datetime.now()
    
    # Busca todos os endpoints associados a este aplicativo
    async with pool.acquire() as conn:
        endpoints = await conn.fetch("""
            SELECT we.url, we.id 
            FROM webhook_endpoints we
            JOIN app_endpoint_mappings aem ON we.id = aem.endpoint_id
            WHERE aem.app_name = $1
        """, app_name)
    
    message = format_webhook_message(app_name, ip_address, timestamp)
    
    # Envia para todos os endpoints configurados
    for endpoint in endpoints:
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(endpoint['url'], json=message) as response:
                    if response.status not in (200, 204):
                        logger.error(f"Falha ao enviar webhook para {endpoint['url']}: {response.status}")
        except Exception as e:
            logger.error(f"Erro ao enviar webhook para {endpoint['url']}: {str(e)}") 