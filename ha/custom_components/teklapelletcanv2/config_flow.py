"""Konfiguracja: wykrywanie (zeroconf/reczny adres) -> parowanie haslem (raz, potem token) -> Opcje: poziom dostepu, wybor encji."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import voluptuous as vol
from homeassistant import config_entries
from homeassistant.helpers.aiohttp_client import async_get_clientsession
from homeassistant.helpers.selector import (NumberSelector, NumberSelectorConfig, SelectSelector, SelectSelectorConfig, SelectSelectorMode, TextSelector, TextSelectorConfig, TextSelectorType)

from .api import TeklaApi, TeklaError
from .const import API_VERSION, CONF_ID, CONF_LEVEL, CONF_PIDS, CONF_SCAN, CONF_TOKEN, DEFAULT_SCAN, DOMAIN

CATALOG = json.loads((Path(__file__).parent / "catalog.json").read_text(encoding="utf-8"))["pids"]
PW = TextSelector(TextSelectorConfig(type=TextSelectorType.PASSWORD))


class TeklaFlow(config_entries.ConfigFlow, domain=DOMAIN):
    VERSION = 1

    def __init__(self) -> None:
        self._host = ""
        self._ident: dict[str, Any] = {}
        self._reauth = None

    async def _identify(self, host: str) -> str | None:
        api = TeklaApi(async_get_clientsession(self.hass), host)
        try:
            self._ident = await api.identity()
        except TeklaError:
            return "cannot_connect"
        if int(self._ident.get("api", 0)) != API_VERSION:
            return "api_mismatch"
        if self._ident.get("setup_needed"):
            return "not_configured"
        return None

    async def async_step_user(self, user_input=None):
        errors = {}
        if user_input is not None:
            self._host = user_input["host"].strip()
            if (err := await self._identify(self._host)) is None:
                await self.async_set_unique_id(self._ident["id"])
                self._abort_if_unique_id_configured(updates={"host": self._host})
                return await self.async_step_pair()
            errors["base"] = err
        return self.async_show_form(step_id="user", data_schema=vol.Schema({vol.Required("host", default=self._host): str}), errors=errors)

    async def async_step_zeroconf(self, discovery_info):
        self._host = str(discovery_info.host)
        props = discovery_info.properties or {}
        did = props.get("id")
        if not did:
            return self.async_abort(reason="cannot_connect")
        await self.async_set_unique_id(did)
        self._abort_if_unique_id_configured(updates={"host": self._host})   # zmiana adresu IP = aktualizacja istniejacego wpisu
        if (err := await self._identify(self._host)) is not None:
            return self.async_abort(reason=err)
        self.context["title_placeholders"] = {"name": self._ident.get("name", "TeklaPellet")}
        return await self.async_step_pair()

    async def async_step_pair(self, user_input=None):
        errors = {}
        if user_input is not None:
            api = TeklaApi(async_get_clientsession(self.hass), self._host)
            try:
                j = await api.pair(user_input["password"], "Home Assistant")
            except TeklaError as e:
                errors["base"] = {"bad_password": "invalid_auth", "locked": "locked", "too_many_clients": "too_many_clients"}.get(e.code, "cannot_connect")
            else:
                data = {"host": self._host, CONF_TOKEN: j["token"], CONF_ID: self._ident["id"]}
                if self._reauth is not None:
                    return self.async_update_reload_and_abort(self._reauth, data={**self._reauth.data, CONF_TOKEN: j["token"]})
                return self.async_create_entry(title=self._ident.get("name", "TeklaPellet"), data=data, options={CONF_LEVEL: 0, CONF_PIDS: [], CONF_SCAN: DEFAULT_SCAN})
        return self.async_show_form(step_id="pair", data_schema=vol.Schema({vol.Required("password"): PW}), errors=errors,
                                    description_placeholders={"name": self._ident.get("name", "TeklaPellet"), "host": self._host})

    async def async_step_reauth(self, entry_data):
        self._reauth = self._get_reauth_entry()
        self._host = entry_data["host"]
        await self._identify(self._host)
        return await self.async_step_pair()

    @staticmethod
    def async_get_options_flow(config_entry):
        return TeklaOptions()


class TeklaOptions(config_entries.OptionsFlow):
    """Poziom dostepu (wg PIN-ow poziomow panelu) i wybor encji. PIN sluzy tylko do sprawdzenia i nie jest zapisywany."""

    def __init__(self) -> None:
        self._level = 0
        self._scan = DEFAULT_SCAN

    async def async_step_init(self, user_input=None):
        o = self.config_entry.options
        errors = {}
        if user_input is not None:
            lvl = int(user_input[CONF_LEVEL])
            if lvl > 0:
                api = TeklaApi(async_get_clientsession(self.hass), self.config_entry.data["host"], self.config_entry.data[CONF_TOKEN])
                try:
                    got = await api.set_level(user_input.get("pin", ""))
                    if got < lvl:
                        errors["pin"] = "pin_level_too_low"
                except TeklaError as e:
                    errors["pin"] = "locked" if e.code == "locked" else "bad_pin"
            if not errors:
                self._level = lvl
                self._scan = int(user_input[CONF_SCAN])
                return await self.async_step_entities()
        schema = vol.Schema({
            vol.Required(CONF_LEVEL, default=str(o.get(CONF_LEVEL, 0))): SelectSelector(SelectSelectorConfig(options=["0", "1", "2", "3"], mode=SelectSelectorMode.DROPDOWN, translation_key="level")),
            vol.Optional("pin"): PW,
            vol.Required(CONF_SCAN, default=o.get(CONF_SCAN, DEFAULT_SCAN)): NumberSelector(NumberSelectorConfig(min=2, max=60, step=1, unit_of_measurement="s")),
        })
        return self.async_show_form(step_id="init", data_schema=schema, errors=errors)

    async def async_step_entities(self, user_input=None):
        opts = [{"value": e["pid"], "label": f"{e['name']} ({e['pid']})" + (f" [{e['unit']}]" if e.get("unit") else "")} for e in CATALOG if e["level_min"] <= self._level]
        valid = {o["value"] for o in opts}
        if user_input is not None:
            return self.async_create_entry(data={CONF_LEVEL: self._level, CONF_SCAN: self._scan, CONF_PIDS: [p for p in user_input.get(CONF_PIDS, []) if p in valid]})
        cur = [p for p in self.config_entry.options.get(CONF_PIDS, []) if p in valid]
        return self.async_show_form(step_id="entities", data_schema=vol.Schema({vol.Optional(CONF_PIDS, default=cur): SelectSelector(SelectSelectorConfig(options=opts, multiple=True, mode=SelectSelectorMode.DROPDOWN))}))
