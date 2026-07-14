from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_api_key
from app.models import ApiKey, Device, SmsJob
from app.schemas import SendSmsRequest, SendSmsResponse
from app.ws.manager import manager

router = APIRouter(prefix="/api/v1/messages", tags=["messages"])


@router.post("/", response_model=SendSmsResponse, status_code=status.HTTP_202_ACCEPTED)
async def send_sms(body: SendSmsRequest, api_key: ApiKey = Depends(get_api_key), db: AsyncSession = Depends(get_db)):
    user_id = api_key.user_id

    result = await db.execute(
        select(Device).where(Device.user_id == user_id, Device.status == "online").order_by(Device.last_seen.desc()).limit(1)
    )
    device = result.scalar_one_or_none()
    if device is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="No online device available")

    job = SmsJob(user_id=user_id, device_id=device.id, phone_number=body.to, message=body.message, status="pending")
    db.add(job)
    await db.flush()
    await db.refresh(job)

    ws_payload = {
        "type": "send_sms",
        "jobId": str(job.id),
        "to": body.to,
        "message": body.message,
    }

    sent = await manager.send_json(device.id, ws_payload)
    if not sent:
        job.status = "failed"
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Failed to forward to device")

    job.status = "sent"
    return SendSmsResponse(jobId=str(job.id), status="accepted")
