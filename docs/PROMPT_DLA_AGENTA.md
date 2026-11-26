# Polecenie startowe dla agenta (do wklejenia)

Pracujesz w samodzielnym repozytorium **TeklaPelletCANv2** (monorepo): kocioł na pellet z panelem Estyma igneoTouch (sterownik FW2213TK), CAN/CANopen 125 kbit/s, kocioł = węzeł 4. Wszystko potrzebne do pracy jest w tym repo; nie korzystaj z żadnych innych repozytoriów.

**Zacznij od:** `README.md` → `CLAUDE.md` (zasady) → `docs/INSTRUKCJA_BUDOWY_NIEZALEZNEJ.md` (mapa zasobów, protokół, komendy, luki) → `docs/ESP_API_V2.md` (kontrakt modułu ESP).

**Gdzie jest co:**
- `data/device_model.json` - źródło prawdy o urządzeniu (655 PID-ów, listy nastaw, konfiguracja, alarmy, harmonogramy, stany); `data/menu_rows.json` - menu panelu z poziomami PIN i warunkami widoczności.
- `analiza/` - cała analiza: plik firmware kotła, dekompilacja, ~230 narzędzi (emulacja Cortex‑M, skany, budowa modelu), dowody i raporty, **pełny zestaw bitmap panelu** i atlas zrzutów prawdziwych ekranów. Start: `analiza/README.md`, `analiza/NewEstyma/00_AGENT_START_HERE.md`.
- `esp/` - firmware ESP32‑C6 + MCP2515; rdzeń testowalny na hoście (`esp/components/tekla_core`).
- `ui/` i `android/` - istniejący klient WWW i Android (możesz je zignorować i zbudować własnego klienta; wspólny opis ekranów to `ui/gen/app.json`, ale nie musisz go używać).
- `archiwum_v1/` - tylko historia (stary produkt, sprzęt PelletCAN).

**Zasady (twarde):** kocioł jest prawdziwy: domyślnie tylko odczyt; zapis wyłącznie dla PID-ów z polem `panel_write`, z odczytem zwrotnym, po świadomym włączeniu trybu i za zgodą właściciela. Żadnych sekretów w repo (hasła, PIN-y, SSID, klucze). Opieraj się na faktach z etykietami `MEASURED`/`STRONG`; `INFERENCE`/`OPEN` traktuj ostrożnie. Nie zakładaj, że cokolwiek było uruchomione na prawdziwym kotle lub telefonie: nie było.

**Komendy:** `python3 tools/gen_esp_tables.py`, `python3 tools/gen_app_model.py`, `sh esp/components/tekla_core/test/run.sh`, `python3 analiza/tools/validate_ui_model.py` (PASS), `cd android && gradle assembleDebug`, `cd esp && idf.py set-target esp32c6 && idf.py build`.

**Czego Ci nikt nie narzuca:** wyglądu, układu ekranów, nawigacji i technologii klienta. Wybierasz sam; dane i zachowanie urządzenia bierz z modelu i dowodów.
