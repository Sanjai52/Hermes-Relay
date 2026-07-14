from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, EmailStr


# ---------- Auth ----------

class UserCreate(BaseModel):
    email: EmailStr
    password: str


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class UserResponse(BaseModel):
    id: UUID
    email: str
    created_at: datetime

    model_config = {"from_attributes": True}


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


# ---------- API Keys ----------

class ApiKeyCreate(BaseModel):
    name: str | None = None


class ApiKeyResponse(BaseModel):
    id: UUID
    prefix: str
    name: str | None
    created_at: datetime
    last_used_at: datetime | None

    model_config = {"from_attributes": True}


class ApiKeyCreatedResponse(ApiKeyResponse):
    raw_key: str


# ---------- Devices ----------

class DeviceRegisterRequest(BaseModel):
    device_name: str


class DeviceResponse(BaseModel):
    id: UUID
    device_name: str
    status: str
    last_seen: datetime | None
    created_at: datetime

    model_config = {"from_attributes": True}


# ---------- SMS ----------

class SendSmsRequest(BaseModel):
    to: str
    message: str


class SendSmsResponse(BaseModel):
    jobId: str
    status: str


class SmsJobResponse(BaseModel):
    id: UUID
    device_id: UUID | None
    phone_number: str
    message: str
    status: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}
