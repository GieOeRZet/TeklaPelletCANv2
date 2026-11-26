# Polecenie startowe dla agenta (do wklejenia)

Pracujesz w samodzielnym repozytorium **TeklaPelletCANv2** (monorepo): kocioł na pellet z panelem Estyma igneoTouch (sterownik FW2213TK), CAN/CANopen 125 kbit/s, kocioł = węzeł 4. Wszystko potrzebne do pracy jest w repo; nie korzystaj z żadnych innych repozytoriów.

## Jak widzisz projekt
- **Moduł ESP32‑C6** to cienki most CAN ↔ API (REST + WebSocket) z małą stroną serwisową; **UI serwują klienci** (Android, Windows, HA). Klient paruje się z modułem raz hasłem, dostaje stały token; moduł ogłasza się przez mDNS ze stałym `id`.
- **Źródło prawdy o urządzeniu**: `data/device_model.json` (655 PID-ów, listy, alarmy, harmonogramy) + `data/menu_rows.json`; w jednym pliku dla budujących interfejs: `docs/interfejs_kompletny.json`. Dowody i narzędzia analizy: `analiza/`.
- **Czego nie ma i co jest niezweryfikowane**: `docs/STATUS.md`. Najważniejsze: **nic nie było uruchomione na prawdziwym kotle, module ESP ani telefonie**.

## Zacznij od
`README.md` → `CLAUDE.md` (zasady) → `docs/STATUS.md` → `docs/ZADANIA.md` (co robić, w jakiej kolejności) → `docs/API_ESP.md` (kontrakt API) → `docs/INSTRUKCJA_BUDOWY_NIEZALEZNEJ.md` (mapa zasobów, protokół, komendy).

## Co możesz sprawdzić lokalnie, a co tylko CI
- **Lokalnie (bez sprzętu):** `sh esp/components/tekla_core/test/run.sh` (rdzeń ESP, g++), `python3 analiza/tools/validate_ui_model.py`, `python3 tools/gen_esp_tables.py && python3 tools/gen_app_model.py`, serwer‑atrapa API + test klienta WWW (`tools/mock/`: `npm i && node server.js`, `node e2e.js`; patrz `tools/mock/README.md`).
- **Tylko w CI (GitHub Actions, `.github/workflows/ci.yml`):** kompilacja firmware (kontener `espressif/idf:v5.4.1`), budowa APK Androida, uruchomienie aplikacji w emulatorze i zrzuty ekranów do `docs/zrzuty_android/`. Na własnym koncie włącz Actions w swoim forku/repozytorium i sprawdzaj wyniki zakładki Actions (workflow ma `concurrency`, kolejny push anuluje poprzedni bieg).
- **Wymaga sprzętu i właściciela:** wszystko z listy w `docs/STATUS.md` → „Czego NIE sprawdzono”.

## Praca na własnym koncie
Sklonuj/zrób fork, pracuj na własnej gałęzi, commit + push na koniec etapu (PR-ów nie twórz bez polecenia). **Nie umieszczaj w repo żadnych sekretów** (hasła, PIN-y, SSID, tokeny, klucze OTA, tokeny GitHub). Klucz prywatny OTA i sekrety CI zakłada właściciel (opis w `docs/ZADANIA.md` pkt 5). Nie masz dostępu do kotła ani modułu: nie deklaruj, że coś „działa”, jeśli sprawdziłeś to tylko w CI lub na atrapie; podawaj, co faktycznie zweryfikowałeś.

## Zasady (twarde)
Kocioł jest prawdziwy: domyślnie tylko odczyt; zapis wyłącznie dla PID-ów z polem `panel_write`, z odczytem zwrotnym, po świadomym włączeniu trybu i za zgodą właściciela. Opieraj się na faktach z etykietami `MEASURED`/`STRONG`; `INFERENCE`/`OPEN` traktuj ostrożnie. Minimalne zmiany, bez refaktoryzacji przy okazji. Wyglądu, układu ekranów i technologii klienta nikt Ci nie narzuca: wybierasz sam, dane i zachowanie bierz z modelu i dowodów.

## Komendy
`python3 tools/gen_esp_tables.py` · `python3 tools/gen_app_model.py` · `sh esp/components/tekla_core/test/run.sh` · `python3 analiza/tools/validate_ui_model.py` (PASS) · `cd android && gradle assembleDebug` · `cd esp && idf.py set-target esp32c6 && idf.py build`
