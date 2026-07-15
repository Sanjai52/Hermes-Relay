from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.routers import auth, dashboard, devices, keys, messages, ws


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title="Hermes Relay Server", version="0.1.0", lifespan=lifespan)

app.include_router(auth.router)
app.include_router(keys.router)
app.include_router(devices.router)
app.include_router(messages.router)
app.include_router(ws.router)
app.include_router(dashboard.router)


@app.api_route("/health", methods=["GET", "HEAD"])
@app.api_route("/ping", methods=["GET", "HEAD"])
async def health():
    return {"status": "ok"}
