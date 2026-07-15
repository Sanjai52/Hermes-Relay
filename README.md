# Hermes-Relay

Self-hosted SMS relay — turn an Android phone into an SMS gateway.

```
Application  ──HTTP──►  Hermes Relay Server  ──WebSocket──►  Android Agent  ──►  SIM  ──►  Carrier
```

## Architecture

| Component | Tech | Role |
|-----------|------|------|
| **Relay Server** | FastAPI + PostgreSQL | Auth, API keys, WebSocket mgmt, SMS job routing, dashboard |
| **Android Agent** | Kotlin + Jetpack Compose | Login, persistent WS connection, send SMS via SmsManager |

## Quick Start (Server)

```bash
# Clone
git clone https://github.com/shalonjovan/Hermes-Relay.git
cd Hermes-Relay

# Docker (recommended)
docker compose up -d

# Or manually
cd relay-server
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env    # edit DATABASE_URL + SECRET_KEY
alembic upgrade head
uvicorn app.main:app --reload
```

Server runs at `http://localhost:8000`.

## API

### Auth

```bash
# Register
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret"}'

# Login
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret"}'
```

### API Keys

```bash
# Create key (save the raw_key response — shown once)
curl -X POST http://localhost:8000/api/v1/keys/ \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"my-server"}'

# List keys
curl http://localhost:8000/api/v1/keys/ \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Send SMS

```bash
curl -X POST http://localhost:8000/api/v1/messages/ \
  -H "Authorization: Bearer <API_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"to":"+919876543210","message":"Hello from Hermes!"}'
```

### WebSocket

Connect from the Android agent:

```json
// Send (auth)
{"type": "auth", "token": "<JWT_TOKEN>"}

// Receive (server → agent)
{"type": "send_sms", "jobId": "job_123", "to": "+919876543210", "message": "Hello!"}

// Send (agent → server, result)
{"type": "sms_result", "jobId": "job_123", "status": "sent"}
```

## Dashboard

Open `http://localhost:8000` in a browser. Login/register, then view device status, SMS history, and manage API keys.

## Android App

Open `mobile-app/` in Android Studio. Update the server URL in `PreferencesManager.kt` to point to your relay server. Build and install on your Android device.

## Database Migrations

```bash
cd relay-server
alembic revision --autogenerate -m "description"
alembic upgrade head
```

## Project Structure

```
hermes-relay/
├── relay-server/         # FastAPI backend
│   ├── app/
│   │   ├── main.py       # Entry point
│   │   ├── config.py     # Settings
│   │   ├── database.py   # Async SQLAlchemy
│   │   ├── models.py     # ORM models
│   │   ├── schemas.py    # Pydantic schemas
│   │   ├── auth.py       # JWT + password hashing
│   │   ├── dependencies  # Auth dependencies
│   │   ├── routers/      # API route handlers
│   │   ├── ws/           # WebSocket manager
│   │   └── templates/    # Dashboard HTML/CSS/JS
│   ├── alembic/          # DB migrations
│   ├── Dockerfile
│   └── requirements.txt
├── mobile-app/           # Android (Kotlin + Compose)
│   └── app/src/main/java/com/hermesrelay/agent/
│       ├── websocket/    # OkHttp WebSocket client
│       ├── service/      # Foreground service
│       ├── sms/          # SmsManager wrapper
│       ├── ui/           # Jetpack Compose screens
│       └── viewmodel/    # ViewModels
├── docker-compose.yml
└── README.md
```

## License

MIT
