"""Koordynator danych: jeden odczyt /api/state (zestawy 'na zywo') + rzadszy odczyt wybranych PID-ow spoza tego zestawu."""
from __future__ import annotations

import logging
from datetime import timedelta
from typing import Any

from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant
from homeassistant.exceptions import ConfigEntryAuthFailed
from homeassistant.helpers.update_coordinator import DataUpdateCoordinator, UpdateFailed

from .api import TeklaApi, TeklaError
from .const import CONF_PIDS, CONF_SCAN, DEFAULT_SCAN, DOMAIN, EXTRA_EVERY

_LOGGER = logging.getLogger(__name__)


class TeklaCoordinator(DataUpdateCoordinator[dict[str, Any]]):
    def __init__(self, hass: HomeAssistant, entry: ConfigEntry, api: TeklaApi) -> None:
        super().__init__(hass, _LOGGER, name=DOMAIN, update_interval=timedelta(seconds=int(entry.options.get(CONF_SCAN, DEFAULT_SCAN))))
        self.api = api
        self.entry = entry
        self.extra: dict[str, float] = {}
        self.info: dict[str, Any] = {}
        self._n = 0

    @property
    def values(self) -> dict[str, float]:
        return (self.data or {}).get("v", {})

    async def read_now(self, pids: list[str]) -> dict[str, float]:
        """Odczyt wskazanych PID-ow z kotla (dla karty: ekrany list)."""
        try:
            return await self.api.params(pids)
        except TeklaError:
            return {}

    async def _async_update_data(self) -> dict[str, Any]:
        try:
            v = await self.api.state()
            if self._n % 12 == 0:
                self.info = await self.api.info()
            sel = [p for p in self.entry.options.get(CONF_PIDS, []) if p not in v]
            if sel and self._n % EXTRA_EVERY == 0:
                self.extra.update(await self.api.params(sel))
            self._n += 1
        except TeklaError as e:
            if e.code in ("unauthorized", "bad_password"):
                raise ConfigEntryAuthFailed("Token odwolany - sparuj ponownie") from e
            raise UpdateFailed(f"{e.code}: {e}") from e
        return {"v": {**self.extra, **v}}
