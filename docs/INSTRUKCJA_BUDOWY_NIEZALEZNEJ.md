# Instrukcja: jak złożyć własną aplikację / oprogramowanie z zasobów tego repozytorium

Dla agenta lub osoby, która chce **niezależnie** zbudować klienta (aplikacja mobilna, strona, integracja) albo własny moduł. Dokument opisuje **dane, fakty o urządzeniu i narzędzia**. Niczego nie narzuca w kwestii wyglądu ani układu interfejsu: to decyzja budującego.

## 1. Co to jest
Kocioł na pellet z panelem Estyma igneoTouch (oprogramowanie sterownika **FW2213TK**). Panel i kocioł rozmawiają po CAN (CANopen, 125 kbit/s). Kocioł = węzeł 4. Dodatkowy moduł (ESP32‑C6 + MCP2515, domyślnie węzeł 95) może czytać dane i (opcjonalnie) zapisywać nastawy. Fakty o urządzeniu pochodzą z odtworzenia panelu w emulatorze (analiza oprogramowania panelu), nie z dokumentacji producenta.

## 2. Mapa repozytorium (co gdzie leży)
| Ścieżka | Zawartość | Rola |
|---|---|---|
| `data/device_model.json` | **źródło prawdy**: 655 PID-ów (nazwa, typ, zakres, skala, tryb, kodowanie zapisu), listy nastaw, konfiguracja, alarmy, moduły, harmonogramy, stany, ekran INFO/SIEĆ/INTERNET | czytają je ESP, WWW, Android |
| `data/menu_rows.json` | 21 wierszy menu panelu: poziom PIN, warunki widoczności (PID + operator + wartość) | menu zależne od konfiguracji |
| `analiza/` | **całe środowisko analizy** (opis: `analiza/README.md`): `FW2213TK_kopia.bin` (firmware sterownika), `firmware/FW2213TK-ghidra/` (dekompilacja), `tools/` (~230 narzędzi: emulacja Cortex‑M, skany, budowa modelu), `analiza_archiwum/FW2213TK-2026-11-22/data/` (dowody: CSV/JSON, `ui_model/` = modele ekranów, `bitmaps/` = **pełny zestaw bitmap panelu**, atlas zrzutów prawdziwych ekranów), raporty kolejnych przejść, `NewEstyma/` (metodyka, plan, backlog, audyty), `rejestry/` (rejestry CAN, przewodnik Ghidry) | dowody, odtwarzalność, szczegóły, gdy model w `data/` nie wystarcza |
| `archiwum_v1/` | stary produkt v1 + dane sprzętowe PelletCAN (ESPHome, piny, tożsamość węzła) | tylko historia |
| `tools/` | generatory i testy v2 (pkt 7) | budowa |
| `esp/` | firmware ESP32‑C6 (ESP‑IDF 5.4): most CAN↔API + mała strona serwisowa, **bez UI**; rdzeń testowalny na hoście (`esp/components/tekla_core`) | moduł |
| `ui/` | klient WWW (JS bez bibliotek; **nie serwowany z ESP**), `ui/gen/*.json` (model wspólny), ikony `ui/assets/ic/` | klient referencyjny / baza pod Windows |
| `android/` | aplikacja Kotlin + Compose (CI buduje APK) | klient Android |
| `docs/API_ESP.md` | **aktualny kontrakt REST/WebSocket modułu ESP** (starszy szkic: `ESP_API_V2.md`) (uwierzytelnianie, tryby, endpointy, formaty) | integracja |

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
- **Stany wyjść** (pompy, podajniki, zapalarka, ruszt, mieszacze): młodszy bajt odpowiedzi; mieszacz 1 = zamykanie, 2 = otwieranie; zbiór par PID→bitmapa: `analiza/analiza_archiwum/FW2213TK-2026-11-22/data/ui_model/outputs_test.json`, `presenter_widget_map.json`.
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
sh esp/components/tekla_core/test/run.sh   # testy rdzenia (natywnie)
cd tools/mock && npm i && node server.js   # serwer‑atrapa API (hasło: demo; po co: tools/mock/README.md) ; node e2e.js = test klienta WWW (Playwright)
cd android && gradle assembleDebug         # APK (JDK 17, Gradle 8.9, SDK 34)
cd esp && idf.py set-target esp32c6 && idf.py build   # ESP‑IDF v5.4.1
```
CI: `.github/workflows/ci.yml` (testy, kompilacja ESP, APK, zrzuty z emulatora do `docs/zrzuty_android/`). Spójność kopii zasobów Androida z `ui/gen` i `ui/assets/ic` jest sprawdzana w CI.

## 8. Moduł ESP: API
Pełny kontrakt: `docs/API_ESP.md`. W skrócie: wykrywanie (mDNS `_teklapellet._tcp`, `/api/identity`, stały `id`), **pierwsze parowanie hasłem → stały token klienta** (`POST /api/pair`), potem `Authorization: Bearer`; `/api/state` + `/ws` (na żywo), `/api/params?list=|pids=`, `PUT /api/param/{pid}` (zapis z odczytem zwrotnym); tryb `readonly`/`full` i PIN-y poziomów wymuszane w ESP; operacje serwisowe po odblokowaniu hasłem (Wi‑Fi, IP z potwierdzeniem, nazwa, hasło, klienci, OTA, restart, reset); konto DEV i OTA z kanału BETA.

## 9. Zasady bezpieczeństwa (obowiązkowe)
1. Kocioł jest prawdziwy. Domyślnie **tylko odczyt**; żadnych testów zapisu na kotle bez właściciela.
2. Zapis tylko dla PID-ów z `panel_write`, z odczytem zwrotnym; nie zapisuj parametrów spoza list panelu.
3. Żadnych sekretów w repo (hasła, PIN-y, Wi‑Fi, klucze, tokeny): repo jest publiczne.
4. Nie zakładaj, że stan zapisanego harmonogramu/trybu jest „sprawdzony” - patrz luki.

## 10. Odtwarzalność: wszystko jest w repo
Nie potrzeba żadnego innego repozytorium. W `analiza/` są: plik firmware sterownika `FW2213TK_kopia.bin` (SHA‑256 w `device_model.firmware_sha256`; **własność producenta, odpowiedzialność za użycie i udostępnianie ma właściciel**), dekompilacja, narzędzia emulacji i budowy modelu oraz dowody. Przepływ zmiany modelu: popraw/odtwórz dane w `analiza/` (`analiza/tools/build_device_model.py` itd., zależności w `analiza/requirements.txt`) → skopiuj `device_model.json` do `data/` → `python3 tools/gen_esp_tables.py` i `python3 tools/gen_app_model.py`. Sprawdzenie spójności: `python3 analiza/tools/validate_ui_model.py` (oczekiwane PASS) oraz CI (kopie modelu muszą być identyczne). Nie ma w repo grafik w wyższej rozdzielczości niż w panelu (producent ich nie ma).

## 11. Znane luki (stan 2026‑11‑26)
Zapis harmonogramów/nastaw/konfiguracji niesprawdzony na kotle · test wyjść (sterowanie) nieznany: nie wiadomo, czy kocioł sam wraca do Auto · okres odpytywania i tempo animacji panelu niezmierzone · widgety ekranu PALNIK/KASKADA bez pozycji · ekran INTERNET częściowo · historia alarmów (odczyt) niezaimplementowana · czas (RTC) nieustawiany · **nic nie było uruchomione na prawdziwym kotle ani na telefonie**. Pełna lista: `analiza/NewEstyma/BACKLOG.md`, `AUDYT_BINARKA_VS_MODEL.md`.

## 12. Webowy instalator pierwszego wgrania ESP32-C6
Dla pierwszego flashowania nie trzeba używać osobnej aplikacji ani lokalnego esptool. Repo zawiera instalator przeglądarkowy w `web_installer/index.html`, oparty o Web Serial + `esptool-js`. Dokumentacja: `docs/WEB_INSTALLER.md`.

Instalator pobiera aktualny `tekla_pellet_flash.bin` z wydania `installer-latest`, wykonuje pełne kasowanie flash i zapis obrazu **od `0x000000`**. Jest to ścieżka do pierwszego, czystego wgrania obrazu zawierającego bootloader + tablicę partycji + firmware. Po takim flashowaniu NVS jest wyczyszczone, więc moduł przechodzi pierwszą konfigurację.

Workflow `.github/workflows/firmware-installer.yml` buduje obraz i publikuje go jako `installer-latest`. Workflow `.github/workflows/pages-installer.yml` publikuje stronę instalatora przez GitHub Pages. Strona wymaga aktualnego Chrome/Edge z Web Serial i USB do ESP32-C6.

**Odnotowana zmiana repo:** dodano `web_installer/index.html`, `docs/WEB_INSTALLER.md`, `.github/workflows/firmware-installer.yml` oraz `.github/workflows/pages-installer.yml`. Nie zmienia to protokołu CAN, API ESP ani zasad bezpieczeństwa zapisu do kotła.
