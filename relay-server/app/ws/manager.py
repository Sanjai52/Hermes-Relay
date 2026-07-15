import json
from uuid import UUID

from fastapi import WebSocket


class ConnectionManager:
    def __init__(self):
        self._connections: dict[UUID, WebSocket] = {}

    async def connect(self, device_id: UUID, ws: WebSocket) -> None:
        self._connections[device_id] = ws

    def disconnect(self, device_id: UUID) -> None:
        self._connections.pop(device_id, None)

    def is_connected(self, device_id: UUID) -> bool:
        return device_id in self._connections

    async def send_json(self, device_id: UUID, data: dict) -> bool:
        ws = self._connections.get(device_id)
        if ws is None:
            return False
        try:
            await ws.send_json(data)
            return True
        except Exception:
            self.disconnect(device_id)
            return False

    async def broadcast_to_user(self, user_id: UUID, data: dict) -> None:
        for device_id in list(self._connections.keys()):
            await self.send_json(device_id, data)

    @property
    def connected_devices(self) -> list[UUID]:
        return list(self._connections.keys())


manager = ConnectionManager()
