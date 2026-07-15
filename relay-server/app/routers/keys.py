import secrets

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth import get_current_user, hash_password
from app.database import get_db
from app.models import ApiKey, User
from app.schemas import ApiKeyCreatedResponse, ApiKeyCreate, ApiKeyResponse

router = APIRouter(prefix="/api/v1/keys", tags=["keys"])


def generate_api_key() -> tuple[str, str, str]:
    raw = f"hr_{secrets.token_urlsafe(32)}"
    prefix = raw[:8]
    return raw, prefix, hash_password(raw)


@router.post("/", response_model=ApiKeyCreatedResponse, status_code=status.HTTP_201_CREATED)
async def create_key(body: ApiKeyCreate, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    raw_key, prefix, key_hash = generate_api_key()
    api_key = ApiKey(user_id=current_user.id, prefix=prefix, key_hash=key_hash, name=body.name)
    db.add(api_key)
    await db.flush()
    await db.refresh(api_key)
    return ApiKeyCreatedResponse(raw_key=raw_key, **ApiKeyResponse.model_validate(api_key).model_dump())


@router.get("/", response_model=list[ApiKeyResponse])
async def list_keys(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(ApiKey).where(ApiKey.user_id == current_user.id).order_by(ApiKey.created_at.desc()))
    keys = result.scalars().all()
    return [ApiKeyResponse.model_validate(k) for k in keys]
