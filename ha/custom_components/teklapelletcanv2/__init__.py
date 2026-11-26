"""TeklaPelletCANv2: integracja modulu ESP (most CAN <-> API) z kotlem pelletowym Estyma igneoTouch.

Domyslnie tworzy tylko to, co niezbedne (czujnik polaczenia). Pozostale encje wybierasz w konfiguratorze (Opcje). Karta `custom:teklapelletcan-card`
(panel 1:1) dziala na danych koordynatora i nie wymaga tworzenia encji."""
from __future__ import annotations

import logging
from pathlib import Path

import homeassistant.helpers.config_validation as cv
import voluptuous as vol
from homeassistant.components import websocket_api
from homeassistant.components.frontend import add_extra_js_url
from homeassistant.components.http import StaticPathConfig
from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant, callback
from homeassistant.helpers import entity_registry as er
from homeassistant.helpers.aiohttp_client import async_get_clientsession
from homeassistant.helpers.typing import ConfigType

from .api import TeklaApi
from .const import CONF_ID, CONF_LEVEL, CONF_PIDS, CONF_TOKEN, DOMAIN, URL_BASE, VERSION
from .coordinator import TeklaCoordinator

_LOGGER = logging.getLogger(__name__)
PLATFORMS = ["binary_sensor", "sensor"]
CONFIG_SCHEMA = cv.config_entry_only_config_schema(DOMAIN)


async def async_setup(hass: HomeAssistant, config: ConfigType) -> bool:
    hass.data.setdefault(DOMAIN, {})
    www = Path(__file__).parent / "www"
    await hass.http.async_register_static_paths([StaticPathConfig(URL_BASE, str(www), False)])
    add_extra_js_url(hass, f"{URL_BASE}/teklapelletcan-card.js?v={VERSION}")   # karta ladowana automatycznie (bez recznego dodawania zasobu)
    websocket_api.async_register_command(hass, ws_subscribe)
    websocket_api.async_register_command(hass, ws_read)
    websocket_api.async_register_command(hass, ws_devices)
    return True


async def async_setup_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    api = TeklaApi(async_get_clientsession(hass), entry.data[CONF_HOST_KEY], entry.data[CONF_TOKEN])
    coord = TeklaCoordinator(hass, entry, api)
    await coord.async_config_entry_first_refresh()
    hass.data[DOMAIN][entry.entry_id] = coord
    # usun encje, ktorych nie wybrano w konfiguratorze (zostaje czujnik polaczenia)
    keep = {f"{entry.data[CONF_ID]}_{p}" for p in entry.options.get(CONF_PIDS, [])} | {f"{entry.data[CONF_ID]}_connected"}
    reg = er.async_get(hass)
    for e in er.async_entries_for_config_entry(reg, entry.entry_id):
        if e.unique_id not in keep:
            reg.async_remove(e.entity_id)
    await hass.config_entries.async_forward_entry_setups(entry, PLATFORMS)
    entry.async_on_unload(entry.add_update_listener(_reload))
    return True


async def _reload(hass: HomeAssistant, entry: ConfigEntry) -> None:
    await hass.config_entries.async_reload(entry.entry_id)


async def async_unload_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    ok = await hass.config_entries.async_unload_platforms(entry, PLATFORMS)
    if ok:
        hass.data[DOMAIN].pop(entry.entry_id, None)
    return ok


CONF_HOST_KEY = "host"


def _coord(hass: HomeAssistant, entry_id: str | None) -> TeklaCoordinator | None:
    items = hass.data.get(DOMAIN, {})
    if entry_id and entry_id in items:
        return items[entry_id]
    return next(iter(items.values()), None) if items else None


def _snapshot(c: TeklaCoordinator) -> dict:
    return {"values": c.values, "connected": bool(c.last_update_success), "level": int(c.entry.options.get(CONF_LEVEL, 0)), "name": c.entry.title,
            "fw": (c.info or {}).get("fw"), "can": (c.info or {}).get("can")}


@websocket_api.websocket_command({vol.Required("type"): f"{DOMAIN}/devices"})
@callback
def ws_devices(hass, connection, msg):
    connection.send_result(msg["id"], [{"entry_id": i, "name": c.entry.title} for i, c in hass.data.get(DOMAIN, {}).items()])


@websocket_api.websocket_command({vol.Required("type"): f"{DOMAIN}/subscribe", vol.Optional("entry_id"): str})
@callback
def ws_subscribe(hass, connection, msg):
    c = _coord(hass, msg.get("entry_id"))
    if c is None:
        connection.send_error(msg["id"], "not_found", "Brak skonfigurowanego modułu")
        return

    @callback
    def push() -> None:
        connection.send_message(websocket_api.event_message(msg["id"], _snapshot(c)))

    connection.subscriptions[msg["id"]] = c.async_add_listener(push)
    connection.send_result(msg["id"])
    push()


@websocket_api.websocket_command({vol.Required("type"): f"{DOMAIN}/read", vol.Required("pids"): [str], vol.Optional("entry_id"): str})
@websocket_api.async_response
async def ws_read(hass, connection, msg):
    c = _coord(hass, msg.get("entry_id"))
    if c is None:
        connection.send_error(msg["id"], "not_found", "Brak skonfigurowanego modułu")
        return
    connection.send_result(msg["id"], await c.read_now(msg["pids"][:40]))
