from __future__ import annotations

from homeassistant.helpers.device_registry import DeviceInfo
from homeassistant.helpers.update_coordinator import CoordinatorEntity

from .const import CONF_ID, DOMAIN
from .coordinator import TeklaCoordinator


class TeklaEntity(CoordinatorEntity[TeklaCoordinator]):
    _attr_has_entity_name = True

    def __init__(self, coordinator: TeklaCoordinator) -> None:
        super().__init__(coordinator)
        e = coordinator.entry
        self._dev = e.data[CONF_ID]
        self._attr_device_info = DeviceInfo(identifiers={(DOMAIN, self._dev)}, name=e.title, manufacturer="TeklaPellet", model="Moduł CAN ESP32-C6 (Estyma igneoTouch)",
                                            sw_version=(coordinator.info or {}).get("fw"), configuration_url=f"http://{e.data['host']}/")
