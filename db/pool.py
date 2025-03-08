import asyncpg

async def create_pool():
    return await asyncpg.create_pool(
        user='seu_usuario',
        password='sua_senha',
        database='seu_banco',
        host='localhost'
    )

pool = None

async def init_db():
    global pool
    pool = await create_pool() 