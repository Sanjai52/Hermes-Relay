from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth import get_current_user
from app.database import get_db
from app.models import ApiKey, Device, SmsJob, User
from app.schemas import SmsJobResponse, DeviceResponse, ApiKeyResponse

router = APIRouter(tags=["dashboard"])

templates = Jinja2Templates(directory="app/templates")


@router.get("/", response_class=HTMLResponse)
async def dashboard_page(request: Request):
    return templates.TemplateResponse(request=request, name="login.html", context={"title": "Login"})


@router.get("/dashboard", response_class=HTMLResponse)
async def dashboard_page(request: Request):
    return templates.TemplateResponse(request=request, name="dashboard.html", context={"title": "Dashboard"})


@router.get("/history", response_class=HTMLResponse)
async def history_page(request: Request):
    return templates.TemplateResponse(request=request, name="history.html", context={"title": "SMS History"})


@router.get("/keys", response_class=HTMLResponse)
async def keys_page(request: Request):
    return templates.TemplateResponse(request=request, name="keys.html", context={"title": "API Keys"})


@router.get("/api/v1/dashboard/stats")
async def dashboard_stats(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    device_count = (await db.execute(select(func.count(Device.id)).where(Device.user_id == current_user.id))).scalar()
    online_devices = (
        await db.execute(
            select(func.count(Device.id)).where(Device.user_id == current_user.id, Device.status == "online")
        )
    ).scalar()
    total_sms = (await db.execute(select(func.count(SmsJob.id)).where(SmsJob.user_id == current_user.id))).scalar()
    recent_sms_result = await db.execute(
        select(SmsJob).where(SmsJob.user_id == current_user.id).order_by(SmsJob.created_at.desc()).limit(5)
    )
    recent_sms = [SmsJobResponse.model_validate(j) for j in recent_sms_result.scalars().all()]

    return {
        "device_count": device_count or 0,
        "online_devices": online_devices or 0,
        "total_sms": total_sms or 0,
        "recent_sms": [j.model_dump(mode="json") for j in recent_sms],
    }
