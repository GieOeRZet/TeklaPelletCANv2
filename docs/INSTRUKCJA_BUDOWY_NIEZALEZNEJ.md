# Instrukcja: jak złożyć własną aplikację / oprogramowanie z zasobów tego repozytorium

Dla agenta lub osoby, która chce **niezależnie** zbudować klienta (aplikacja mobilna, strona, integracja) albo własny moduł. Dokument opisuje **dane, fakty o urządzeniu i narzędzia**. Niczego nie narzuca w kwestii wyglądu ani układu interfejsu: to decyzja budującego.

## 1. Co to jest
Kocioł na pellet z panelem Estyma igneoTouch (oprogramowanie sterownika **FW2213TK**). Panel i kocioł rozmawiają po CAN (CANopen, 125 kbit/s). Kocioł = węzeł 4. Dodatkowy moduł (ESP32‑C6 + MCP2515, domyślnie węzeł 95) może czytać dane i (opcjonalnie) zapisywać nastawy. Fakty o urządzeniu pochodzą z odtworzenia panelu w emulatorze (analiza oprogramowania panelu), nie z dokumentacji producenta.

## 2. Mapa repozytorium (co gdzie leży)
| Ścieżka | Zawartość | Rola |
|---|---|---|
| `data/device_model.json` | **źródło prawdy**: 655 PID-ów (nazwa, typ, zakres, skala, tryb, kodowanie zapisu), listy nastaw, konfiguracja, alarmy, moduły, harmonogramy, stany, ekran INFO/SIEĆ/INTERNET | czytają je ESP, WWW, Android |
| `data/menu_rows.json` | 21 wierszy menu panelu: poziom PIN, warunki widoczności (PID + operator + wartość) | menu zależne od konfiguracji |
| `zrodla/model_ekranow/` | dane z analizy panelu: `menu_tree.json`, `lists.json`, `list_dialogs.json`, `firmware_row_table.json`, `presenter_widget_map.json`, `real_panel_screens.json`, `home_indicators.json`, `screens_config.json`, `program_pids.json`, `write_frames.json`, `alarm_codes_verified.json`, `outputs_test.json`, `live_sources.json`, `siec_modules.json` itd. (`README.md` tam opisuje zestaw) | dowody i szczegóły, gdy model w `data/` nie wystarcza |
| `zrodla/bitmapy/` | **pełny zestaw bitmap panelu** (PNG + `manifest.csv`); numer bitmapy = numer w tabelach (np. ikony wyjść, trybów, obrazy kotła) | grafiki (odpowiedzialność za prawa do nich ma właściciel) |
| `zrodla/atlas_panelu/` | zrzuty prawdziwych ekranów panelu (stany, listy) | wzorzec zachowania/zawartości |
| `zrodla/dokumenty/` | raporty: metodyka, audyt binarka‑vs‑model, backlog, drzewo menu, widgety wg ekranów, poziomy dostępu | kontekst i luki |
| `zrodla/narzedzia_analizy/` | skrypty, które zbudowały model z analizy (pochodzenie danych); **działają tylko ze starym repo i plikiem firmware** (pkt 10) | pochodzenie |
| `tools/` | generatory i testy v2 (pkt 7) | budowa |
| `esp/` | firmware ESP32‑C6 (ESP‑IDF 5.4) + rdzeń testowalny na hoście (`esp/components/tekla_core`) | moduł |
| `ui/` | strona WWW (JS bez bibliotek), `ui/gen/*.json` (model wspólny), ikony `ui/assets/ic/` | klient WWW |
| `android/` | aplikacja Kotlin + Compose (CI buduje APK) | klient Android |
| `docs/ESP_API_V2.md` | **kontrakt REST/WebSocket modułu ESP** (uwierzytelnianie, tryby, endpointy, formaty) | integracja |

## 3. Etykiety dowodów (obowiązują w całym projekcie)
`MEASURED` (zmierzone w emulatorze / na CAN) · `STRONG` (mocna analiza kodu) · `INFERENCE` (wnioskowanie, jawnie oznaczone) · `OPEN` (nierozstrzygnięte) · `REJECTED` (obalone). Zapis w kodzie klienta opieraj tylko na `MEASURED`/`STRONG`, resztę traktuj ostrożnie.

## 4. Protokół w skrócie (CAN/SDO)
- Odczyt PID: ramka `0x604` = `40 idx_lo idx_hi 01 00 00 00 00`; odpowiedź `0x584` (4 bajty danych, pierwsze bajty = wartość; stan wyjścia = młodszy bajt).
- Zapis: `0x604` = `42 idx_lo idx_hi 01 <u32 LE>`; po zapisie **zawsze odczyt zwrotny** i porównanie. Kodowanie wartości (`plain` itp.) jest w `device_model.pids.items[pid].panel_write` (tylko PID-y z tym polem wolno zapisywać).
- Skala wyświetlania: `format.scale`; surowa wartość = wyświetlana / skala.
- Heartbeat/tożsamość węzła ESP: `esp/components/tekla_core/include/tekla/identity.hpp`.
- Gotowe, przetestowane funkcje: `sdo.hpp`, `guard.hpp` (kto/kiedy może zapisać), `rules.hpp` (reguły stanów, ikon, diod) - testy `esp/components/tekla_core/test/`.

## 5. Jak odczytać z modelu najważniejsze rzeczy
- **Lista parametrów ekranu/nastaw**: `device_model.lists` (lub `ui/gen/model.min.json` w skróconej postaci): etykieta PL, PID, min/max/krok, typ okna (suwak/radio/przełącznik), opcje, minimalny poziom PIN.
- **Menu i widoczność**: `data/menu_rows.json`; konfiguracja kotła zmienia, co jest widoczne (PID-y: liczba obwodów `0x2105`, CWU `0x2106`, bufor `0x2107`, solary `0x2108`, kaskada `0x350C`, magazyn `0x3525`, czujnik powrotu `0x350A`, ciśnienie `0x350D`, wariant obrazu kotła `0x370A`).
- **Stany wyjść** (pompy, podajniki, zapalarka, ruszt, mieszacze): młodszy bajt odpowiedzi; mieszacz 1 = zamykanie, 2 = otwieranie; zbiór par PID→bitmapa: `zrodla/model_ekranow/outputs_test.json`, `presenter_widget_map.json`.
- **Stan palnika, diody, ikona paliwa, pora roku**: `home_indicators.json`, `real_panel_screens.json`, reguły w `rules.hpp`.
- **Harmonogramy**: 24‑bitowa maska na dzień (bit h = godzina h; 1 = komfort), PID-y per obwód i dzień: `device_model.program.pids_by_circuit`, `program_pids.json`. **Zapis harmonogramów nie był sprawdzony na prawdziwym kotle.**
- **Alarmy**: `alarm_codes_verified.json` (kody i teksty), rejestry historii `0x4011‑0x404B` (odczyt jeszcze nie zaimplementowany w ESP).
- **Poziomy dostępu PIN 0–3**: `SPEC-POZIOMY-DOSTEPU-0-1-2.md`; wartości PIN-ów ustawia właściciel (nie ma ich w repo).
- **42 parametry spoza list panelu**: zablokowane do zapisu (lista `rows_not_in_any_panel_list`).

## 6. Sprzęt modułu
Seeed XIAO ESP32‑C6 + MCP2515 (16 MHz, SPI 1 MHz): SCK=GPIO19, MISO=GPIO20, MOSI=GPIO18, CS=GPIO17, INT nieużywany, przełącznik anteny GPIO3/GPIO14 (domyślnie antena wewnętrzna). Wartości w `esp/main/config.hpp`. Węzeł domyślny 95, produkt 1 (panel pokazuje „UNKNOWN”).

## 7. Budowa i testy (komendy)
```
python3 tools/gen_esp_tables.py            # device_model.json -> tabele ESP (esp/components/tekla_core/gen) + ui/gen/model.min.json
python3 tools/gen_app_model.py             # + data/menu_rows.json + tools/app_spec.py -> ui/gen/app.json i android/.../assets/app.json
sh esp/components/tekla_core/test/run.sh   # testy rdzenia (natywnie) + paczka UI
cd tools/mock && npm i && node server.js   # serwer‑atrapa ESP (hasło: demo) ; node e2e.js = test strony (Playwright)
cd android && gradle assembleDebug         # APK (JDK 17, Gradle 8.9, SDK 34)
cd esp && idf.py set-target esp32c6 && idf.py build   # ESP‑IDF v5.4.1
```
CI: `.github/workflows/ci.yml` (testy, kompilacja ESP, APK, zrzuty z emulatora do `docs/zrzuty_android/`). Spójność kopii zasobów Androida z `ui/gen` i `ui/assets/ic` jest sprawdzana w CI.

## 8. Moduł ESP: API
Pełny kontrakt: `docs/ESP_API_V2.md`. W skrócie: `POST /api/login` (hasło → token), `/api/state` + `/ws` (wartości na żywo), `/api/params?list=` / `?pids=`, `PUT /api/param/{pid}` (zapis z odczytem zwrotnym), tryb `readonly`/`full` i PIN-y poziomów wymuszane po stronie ESP, `/api/alarms`, `/api/wifi*`, `/api/password`, `/api/restart`, `/api/factory_reset`, `/api/log`, `/api/network`, OTA (`/api/ota/fw`, `/api/ota/ui`, podpis ECDSA P‑256).

## 9. Zasady bezpieczeństwa (obowiązkowe)
1. Kocioł jest prawdziwy. Domyślnie **tylko odczyt**; żadnych testów zapisu na kotle bez właściciela.
2. Zapis tylko dla PID-ów z `panel_write`, z odczytem zwrotnym; nie zapisuj parametrów spoza list panelu.
3. Żadnych sekretów w repo (hasła, PIN-y, Wi‑Fi, klucze, tokeny): repo jest publiczne.
4. Nie zakładaj, że stan zapisanego harmonogramu/trybu jest „sprawdzony” - patrz luki.

## 10. Czego NIE ma w repo (i dlaczego)
- **Plik firmware sterownika FW2213TK** (kopia właściciela, jest w starym repo `GieOeRZet/TeklaPelletCAN`, a hasz SHA‑256 w `device_model.firmware_sha256`) oraz środowisko emulacji/dekompilacji. Dlatego `zrodla/narzedzia_analizy/` służy jako dokumentacja pochodzenia danych; ponowne wygenerowanie modelu wymaga tego pliku i starego repo. Dane w `data/` i `zrodla/` traktuj jako **zamrożone dowody**.
- Wyższa rozdzielczość grafik niż w panelu (producent ma tylko te bitmapy).

## 11. Znane luki (stan 2026‑11‑26)
Zapis harmonogramów/nastaw/konfiguracji niesprawdzony na kotle · test wyjść (sterowanie) nieznany: nie wiadomo, czy kocioł sam wraca do Auto · okres odpytywania i tempo animacji panelu niezmierzone · widgety ekranu PALNIK/KASKADA bez pozycji · ekran INTERNET częściowo · historia alarmów (odczyt) niezaimplementowana · czas (RTC) nieustawiany · **nic nie było uruchomione na prawdziwym kotle ani na telefonie**. Pełna lista: `zrodla/dokumenty/BACKLOG.md`, `AUDYT_BINARKA_VS_MODEL.md`.
