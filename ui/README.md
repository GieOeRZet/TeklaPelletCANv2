# ui - strona WWW (serwowana z ESP, partycja ui_0/ui_1)

- `src/` - index.html, app.css, app.js (czysty JS, bez bibliotek); `assets/ic/` - bitmapy panelu (PNG, na odpowiedzialnosc wlasciciela); `gen/model.min.json` - GENEROWANY przez `tools/gen_esp_tables.py`.
- Konwencje danych: `/ws` wysyla wartosci w jednostkach wyswietlanych (temperatury w C), PID-y wyjsc jako stan (mlodszy bajt SDO: 0/1/2). Reguly i zakresy: `docs/ESP_API_V2.md`.
- Uruchomienie z serwerem-atrapa: `cd tools/mock && npm install && node server.js` -> http://localhost:8099 (haslo demo `demo`, PIN-y demo 1111/2222/3333).
- Test e2e (Playwright, telefon 412x915 i pulpit 1280x800, zrzuty w /tmp/tp_shots): `NODE_PATH=$(npm root -g) node tools/mock/e2e.js`.
- Zasady ukladu: `docs/UI_ZASADY.md` (nic nie zasloniete: safe-area, dvh, dolne zakladki nad paskiem systemowym).
