# ui - klient WWW (czysty JS) i modele wspólne

**Nie jest serwowany z ESP.** To klient referencyjny używany z serwerem-atrapą (`../tools/mock`) i baza pod klienta Windows. Android ma własną implementację, ale te same modele.

- `src/` - index.html, app.css, app.js (bez bibliotek); `assets/ic/` - bitmapy panelu (PNG, na odpowiedzialność właściciela); `gen/` - GENEROWANE: `model.min.json` (`tools/gen_esp_tables.py`) i `app.json` (`tools/gen_app_model.py`; menu z warunkami, ekrany jako dane, konfiguracja, harmonogramy). Nie edytuj ręcznie.
- Dane: `/ws` wysyła wartości w jednostkach wyświetlanych; PID-y wyjść jako stan (młodszy bajt SDO 0/1/2). Kontrakt: `../docs/API_ESP.md`.
- Uruchomienie: `cd ../tools/mock && npm install && node server.js` -> http://localhost:8099 (hasło `demo`, PIN-y 1111/2222/3333).
- Test e2e (Playwright, telefon 412x915 i pulpit 1280x800): `NODE_PATH=$(npm root -g) node ../tools/mock/e2e.js`.
- Uwaga: klient WWW loguje się hasłem (sesja). Parowanie tokenem (stały token klienta) jest w API, ale ten klient jeszcze go nie używa.
- Zasady układu: `../docs/UI_ZASADY.md`. `mockups/` - historyczne makiety.
