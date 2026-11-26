# TeklaPelletCANv2

Samodzielne monorepo: dodatkowy moduł **ESP32‑C6 (Seeed XIAO) + MCP2515** na magistrali CAN kotła na pellet z panelem Estyma igneoTouch (sterownik FW2213TK) oraz klienci (aplikacja Android, strona WWW, docelowo Windows i Home Assistant). W repo jest też **cała analiza panelu** (emulacja, dekompilacja, narzędzia, dowody), więc nie potrzeba żadnego innego repozytorium.

**Nowy agent/osoba: zacznij od `docs/PROMPT_DLA_AGENTA.md`**, potem `docs/STATUS.md` (co działa, co nie) i `docs/ZADANIA.md` (lista prac).

## Architektura w jednym akapicie
ESP to **cienki most CAN ↔ API (REST + WebSocket)** z małą wbudowaną stroną serwisową (konfiguracja Wi‑Fi/IP, hasło, parowanie klientów, aktualizacja firmware). **Interfejsu użytkownika ESP nie serwuje**: robią to klienci (Android, Windows, HA), którzy parują się z modułem raz hasłem i dostają stały token. Moduł się ogłasza (mDNS, stały ID z MAC), więc można mieć wiele urządzeń. Strażnik zapisu (tryb odczyt/pełny, PIN-y poziomów, kodowanie z modelu, odczyt zwrotny) działa w ESP.

## Struktura
| Katalog | Zawartość |
|---|---|
| `docs/` | `PROMPT_DLA_AGENTA.md`, `STATUS.md`, `ZADANIA.md`, `API_ESP.md` (kontrakt API), `INSTRUKCJA_BUDOWY_NIEZALEZNEJ.md` (mapa zasobów, protokół, komendy), `interfejs_kompletny.json` (komplet danych do zbudowania interfejsu), `UI_ZASADY.md`, `zrzuty_android/` |
| `data/` | `device_model.json` (źródło prawdy o urządzeniu), `menu_rows.json` |
| `esp/` | firmware ESP32‑C6 (ESP‑IDF 5.4); rdzeń `components/tekla_core` testowalny na hoście |
| `android/` | aplikacja natywna (Kotlin + Compose), tryb demo i połączenie z ESP |
| `ui/` | klient WWW (czysty JS) + `gen/` (modele wspólne) + `assets/ic/` (bitmapy panelu); **nie jest serwowany z ESP**, to baza pod klienta Windows i do testów |
| `tools/` | generatory modeli, podpis OTA, serwer‑atrapa API (`tools/mock/`), skrypt zrzutów Androida |
| `analiza/` | środowisko analizy: plik firmware kotła, dekompilacja, ~230 narzędzi, dowody, bitmapy, atlas panelu (opis: `analiza/README.md`) |
| `archiwum_v1/` | historia: stary produkt v1 i dane sprzętowe PelletCAN (tylko do odczytu) |

## Zasady (skrót, pełne w `CLAUDE.md`)
Domyślnie **tylko odczyt**; zapis do kotła tylko świadomie, z odczytem zwrotnym, nigdy bez właściciela. **Żadnych sekretów w repo** (hasła, PIN-y, SSID, klucze; repo publiczne). Dowody oznaczone MEASURED/STRONG/INFERENCE/OPEN/REJECTED. Minimalne zmiany; commit + push na koniec etapu.

## Stan
Szczegóły w `docs/STATUS.md`. W skrócie: firmware ESP, aplikacja Android i testy kompilują się w CI; aplikacja uruchamia się w emulatorze w trybie demo. **Nic nie było uruchomione na prawdziwym kotle, module ESP ani telefonie.**
