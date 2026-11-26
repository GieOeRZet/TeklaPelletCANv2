"""Czujnik polaczenia z modulem - jedyna encja tworzona domyslnie."""
from __future__ import annotations

from homeassistant.components.binary_sensor import BinarySensorDeviceClass, BinarySensorEntity

from .const import DOMAIN
from .entity import TeklaEntity


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities([TeklaConnected(hass.data[DOMAIN][entry.entry_id])])


class TeklaConnected(TeklaEntity, BinarySensorEntity):
    _attr_device_class = BinarySensorDeviceClass.CONNECTIVITY
    _attr_name = "Połączenie"

    def __init__(self, coordinator):
        super().__init__(coordinator)
        self._attr_unique_id = f"{self._dev}_connected"

    @property
    def available(self) -> bool:   # offline = "wylaczony", nie "niedostepny"
        return True

    @property
    def is_on(self) -> bool:
        return bool(self.coordinator.last_update_success)

    @property
    def extra_state_attributes(self):
        i = self.coordinator.info or {}
        return {k: i.get(k) for k in ("fw", "api", "can", "mode", "uptime", "id") if i.get(k) is not None}
