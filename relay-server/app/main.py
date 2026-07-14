from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.routers import auth, keys


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title="Hermes Relay Server", version="0.1.0", lifespan=lifespan)

app.include_router(auth.router)
app.include_router(keys.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
