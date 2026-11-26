# TeklaPelletCANv2

Nowy, **niezależny** projekt: dodatkowy sterownik/moduł **ESP32-C6 (Seeed XIAO) + MCP2515** podłączony do magistrali CAN kotła na pellet z panelem Estyma igneoTouch (firmware sterownika FW2213TK), z pełnym odczytem i (opcjonalnie) zapisem, oraz klienci: strona WWW, karta i integracja Home Assistant, aplikacja Android (natywna, Kotlin/Compose).

Repozytorium jest **samodzielnym monorepo**: zawiera zarówno produkt (ESP, WWW, Android), jak i całą analizę panelu (emulacja, dekompilacja, narzędzia, dowody) w `analiza/`. Dane o urządzeniu (menu, PID-y, kodowanie zapisu, alarmy, moduły) pochodzą z odtworzenia panelu w emulatorze; gotowy model produktowy to `data/device_model.json`.

## Struktura
| Katalog | Zawartość |
|---|---|
| `docs/` | specyfikacje (API v2, pamięć/OTA, bezpieczeństwo, interfejs), decyzje |
| `data/` | `device_model.json` (źródło prawdy dla ESP i klientów), `menu_rows.json` |
| `analiza/` | **kompletne środowisko analizy**: plik firmware sterownika, wynik dekompilacji (Ghidra), ~230 narzędzi (emulacja Cortex‑M, skany, budowa modelu), dowody i raporty, bitmapy i atlas panelu, dokumentacja procesu. Opis: `analiza/README.md` |
| `archiwum_v1/` | historyczne: stary produkt v1 (firmware, WWW, Android, integracja HA) i dane sprzętowe PelletCAN (ESPHome); tylko do odczytu |
| `esp/` | firmware ESP32-C6 (ESP-IDF; rdzeń testowalny na hoście) |
| `ui/` | wspólny opis ekranów (JSON) + renderer JS (WWW i karta HA) |
| `ha/` | integracja HA + karta Lovelace |
| `android/` | aplikacja natywna (Kotlin + Jetpack Compose) |
| `tools/` | generatory (tabele firmware z `device_model.json`), testy |

## Zasady (skrót, pełne w `CLAUDE.md`)
- Domyślnie **tylko odczyt**; zapis dopiero po świadomym włączeniu trybu i próbie na kotle z właścicielem.
- Żadnych haseł/PIN-ów w repo (repo publiczne).
- Etap = jeden commit + push; CI ma być oszczędne.

## Status
Szkielet + specyfikacja API v2 (`docs/ESP_API_V2.md`) z decyzjami wlasciciela (runda 1 i 2) - do akceptacji; makiety `ui/mockups/`. Kodu ESP jeszcze nie ma.


**Instrukcja dla kogoś, kto chce złożyć własnego klienta lub moduł niezależnie: `docs/INSTRUKCJA_BUDOWY_NIEZALEZNEJ.md`.**
