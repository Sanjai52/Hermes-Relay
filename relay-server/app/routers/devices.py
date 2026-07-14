from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth import get_current_user
from app.database import get_db
from app.models import Device, User
from app.schemas import DeviceResponse

router = APIRouter(prefix="/api/v1/devices", tags=["devices"])


@router.get("/", response_model=list[DeviceResponse])
async def list_devices(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Device).where(Device.user_id == current_user.id).order_by(Device.last_seen.desc().nullslast())
    )
    return [DeviceResponse.model_validate(d) for d in result.scalars().all()]
