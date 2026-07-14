from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import ApiKey

api_key_scheme = HTTPBearer(auto_error=False)


async def get_api_key(
    credentials: HTTPAuthorizationCredentials = Depends(api_key_scheme),
    db: AsyncSession = Depends(get_db),
) -> ApiKey:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing API key")
    raw_key = credentials.credentials

    from app.auth import hash_password as hash_fn

    result = await db.execute(select(ApiKey))
    all_keys = result.scalars().all()
    for api_key in all_keys:
        from app.auth import verify_password

        if verify_password(raw_key, api_key.key_hash):
            return api_key

    raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid API key")
