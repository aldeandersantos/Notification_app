from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from db.pool import pool
from db.errors import UniqueViolationError
from typing import List

router = APIRouter()

class AppEndpointMapping(BaseModel):
    app_name: str
    endpoint_id: int

class AppEndpointMappingResponse(BaseModel):
    app_name: str
    endpoint_url: str
    endpoint_id: int

@router.post("/app-endpoint-mapping", response_model=dict)
async def create_app_endpoint_mapping(mapping: AppEndpointMapping):
    async with pool.acquire() as conn:
        # Verificar se o endpoint existe
        endpoint = await conn.fetchrow(
            "SELECT id FROM webhook_endpoints WHERE id = $1", 
            mapping.endpoint_id
        )
        if not endpoint:
            raise HTTPException(
                status_code=404, 
                detail=f"Endpoint com ID {mapping.endpoint_id} não encontrado"
            )

        try:
            await conn.execute("""
                INSERT INTO app_endpoint_mappings (app_name, endpoint_id)
                VALUES ($1, $2)
            """, mapping.app_name, mapping.endpoint_id)
            return {"message": "Mapeamento criado com sucesso"}
        except UniqueViolationError:
            raise HTTPException(
                status_code=400, 
                detail="Este mapeamento já existe"
            )

@router.get("/app-endpoint-mappings", response_model=List[AppEndpointMappingResponse])
async def get_app_endpoint_mappings():
    async with pool.acquire() as conn:
        mappings = await conn.fetch("""
            SELECT aem.app_name, 
                   we.url as endpoint_url,
                   we.id as endpoint_id
            FROM app_endpoint_mappings aem
            JOIN webhook_endpoints we ON we.id = aem.endpoint_id
            ORDER BY aem.app_name, we.id
        """)
        return [dict(m) for m in mappings]

@router.delete("/app-endpoint-mapping", response_model=dict)
async def delete_app_endpoint_mapping(mapping: AppEndpointMapping):
    async with pool.acquire() as conn:
        result = await conn.execute("""
            DELETE FROM app_endpoint_mappings
            WHERE app_name = $1 AND endpoint_id = $2
        """, mapping.app_name, mapping.endpoint_id)
        
        if result == "DELETE 0":
            raise HTTPException(
                status_code=404,
                detail="Mapeamento não encontrado"
            )
            
        return {"message": "Mapeamento removido com sucesso"} 