import json
from datetime import datetime, timezone

from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from sqlalchemy import select

from app.auth import decode_access_token
from app.database import async_session_factory
from app.models import Device, SmsJob, User
from app.ws.manager import manager

router = APIRouter()


@router.websocket("/ws")
async def websocket_endpoint(ws: WebSocket):
    device_id = None
    user_id = None

    try:
        await ws.accept()

        auth_data = await ws.receive_json()
        if auth_data.get("type") != "auth" or "token" not in auth_data:
            await ws.send_json({"type": "error", "message": "Missing auth token"})
            await ws.close(code=4001)
            return

        try:
            payload = decode_access_token(auth_data["token"])
            user_id = payload.get("sub")
        except Exception:
            await ws.send_json({"type": "error", "message": "Invalid token"})
            await ws.close(code=4001)
            return

        async with async_session_factory() as db:
            result = await db.execute(select(User).where(User.id == user_id))
            user = result.scalar_one_or_none()
            if user is None:
                await ws.send_json({"type": "error", "message": "User not found"})
                await ws.close(code=4001)
                return

            result = await db.execute(
                select(Device).where(Device.user_id == user.id, Device.status == "online").limit(1)
            )
            device = result.scalar_one_or_none()

            if device is None:
                device = Device(user_id=user.id, device_name="Android Agent", status="online", last_seen=datetime.now(timezone.utc))
                db.add(device)
            else:
                device.status = "online"
                device.last_seen = datetime.now(timezone.utc)
            await db.commit()
            await db.refresh(device)
            device_id = device.id

        await manager.connect(device_id, ws)
        await ws.send_json({"type": "auth_ok", "deviceId": str(device_id)})

        while True:
            data = await ws.receive_json()
            msg_type = data.get("type")

            if msg_type == "ping":
                await ws.send_json({"type": "pong"})
            elif msg_type == "sms_result":
                await handle_sms_result(data, device_id, user_id)
            elif msg_type == "device_info":
                async with async_session_factory() as db:
                    result = await db.execute(select(Device).where(Device.id == device_id))
                    dev = result.scalar_one_or_none()
                    if dev:
                        dev.device_name = data.get("device_name", dev.device_name)
                        dev.last_seen = datetime.now(timezone.utc)
                        await db.commit()

    except WebSocketDisconnect:
        pass
    except Exception:
        pass
    finally:
        if device_id:
            manager.disconnect(device_id)
            async with async_session_factory() as db:
                result = await db.execute(select(Device).where(Device.id == device_id))
                dev = result.scalar_one_or_none()
                if dev:
                    dev.status = "offline"
                    await db.commit()


async def handle_sms_result(data: dict, device_id, user_id):
    job_id = data.get("jobId")
    status = data.get("status", "unknown")

    if not job_id:
        return

    async with async_session_factory() as db:
        result = await db.execute(select(SmsJob).where(SmsJob.id == job_id))
        job = result.scalar_one_or_none()
        if job:
            job.status = status
            job.device_id = device_id
            await db.commit()
