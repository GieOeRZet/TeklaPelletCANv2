"""Czujniki wybrane w konfiguratorze (Opcje -> Encje). Wartosci w jednostkach wyswietlanych, jak w panelu."""
from __future__ import annotations

import json
from pathlib import Path

from homeassistant.components.sensor import SensorEntity, SensorStateClass

from .const import CONF_PIDS, DOMAIN
from .entity import TeklaEntity

CATALOG = {e["pid"]: e for e in json.loads((Path(__file__).parent / "catalog.json").read_text(encoding="utf-8"))["pids"]}


async def async_setup_entry(hass, entry, async_add_entities):
    c = hass.data[DOMAIN][entry.entry_id]
    async_add_entities([TeklaSensor(c, p) for p in entry.options.get(CONF_PIDS, []) if p in CATALOG])


class TeklaSensor(TeklaEntity, SensorEntity):
    def __init__(self, coordinator, pid: str) -> None:
        super().__init__(coordinator)
        e = CATALOG[pid]
        self._pid = pid
        self._attr_unique_id = f"{self._dev}_{pid}"
        self._attr_name = f"{e['name']} ({pid})"
        unit = e.get("unit")
        self._attr_native_unit_of_measurement = unit
        if unit:
            self._attr_state_class = SensorStateClass.MEASUREMENT

    @property
    def native_value(self):
        return self.coordinator.values.get(self._pid)

    @property
    def available(self) -> bool:
        return self.coordinator.last_update_success and self._pid in self.coordinator.values
