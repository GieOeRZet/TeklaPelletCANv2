"""Klient REST modulu TeklaPellet (docs/API_ESP.md, api = 2)."""
from __future__ import annotations

import asyncio
from typing import Any

import aiohttp


class TeklaError(Exception):
    """Blad komunikacji lub odpowiedzi modulu."""

    def __init__(self, code: str, msg: str = "") -> None:
        super().__init__(msg or code)
        self.code = code


class TeklaApi:
    def __init__(self, session: aiohttp.ClientSession, host: str, token: str | None = None) -> None:
        self._s = session
        self.host = host
        self.token = token

    @property
    def base(self) -> str:
        return (self.host if self.host.startswith("http") else f"http://{self.host}").rstrip("/")

    async def call(self, method: str, path: str, body: dict | None = None, timeout: float = 8) -> dict[str, Any]:
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        try:
            async with asyncio.timeout(timeout):
                async with self._s.request(method, self.base + path, json=body, headers=headers) as r:
                    try:
                        j = await r.json(content_type=None)
                    except Exception:  # noqa: BLE001
                        raise TeklaError("bad_response") from None
        except (aiohttp.ClientError, TimeoutError) as e:
            raise TeklaError("network", str(e)) from e
        if not isinstance(j, dict) or not j.get("ok"):
            raise TeklaError((j or {}).get("code", "error") if isinstance(j, dict) else "error")
        return j

    async def identity(self) -> dict[str, Any]:
        return await self.call("GET", "/api/identity")

    async def pair(self, password: str, name: str) -> dict[str, Any]:
        return await self.call("POST", "/api/pair", {"password": password, "name": name})

    async def state(self) -> dict[str, float]:
        j = await self.call("GET", "/api/state?all=1")
        return {k: float(v) for k, v in (j.get("v") or {}).items()}

    async def info(self) -> dict[str, Any]:
        return await self.call("GET", "/api/info")

    async def params(self, pids: list[str]) -> dict[str, float]:
        out: dict[str, float] = {}
        for i in range(0, len(pids), 30):
            j = await self.call("GET", "/api/params?pids=" + ",".join(pids[i : i + 30]), timeout=15)
            out.update({k: float(v) for k, v in (j.get("values") or {}).items()})
        return out

    async def set_level(self, pin: str) -> int:
        return int((await self.call("POST", "/api/level", {"pin": pin})).get("level", 0))
