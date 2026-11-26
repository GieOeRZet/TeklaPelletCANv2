# Status prac (aktualizacja: 2026-11-26)

Legenda: ✅ sprawdzone (test/CI/emulator) · 🟡 kod jest, kompiluje się, ale nie uruchomiony na sprzęcie · ⬜ brak.

## Moduł ESP (`esp/`)
| Element | Stan |
|---|---|
| Most CAN: odczyt SDO, zapis z odczytem zwrotnym, strażnik zapisu, tożsamość węzła (heartbeat/SDO) | 🟡 rdzeń ✅ testy natywne (`esp/components/tekla_core/test`), reszta 🟡 (kompilacja w CI) |
| API REST + WebSocket wg `docs/API_ESP.md`; tryby, PIN-y poziomów | 🟡 |
| Tożsamość: stały ID z MAC, nazwa, mDNS (TXT: id, name, fw, api, node) | 🟡 |
| Pierwsza konfiguracja (AP + strona serwisowa), parowanie tokenem (do 8 klientów, odwoływanie) | 🟡 |
| Operacje serwisowe po haśle (5 min): Wi‑Fi (z cofnięciem 40 s), IP/DNS (z potwierdzeniem 60 s), nazwa, hasło, restart, reset | 🟡 |
| Awaryjny AP po 3 min bez Wi‑Fi | 🟡 |
| OTA firmware z pliku (zwykły .bin bez klucza, kontrola obrazu, rollback po nieudanym starcie) | 🟡 · skompilowane, niesprawdzone na sprzęcie |
| Konto DEV (osobne hasło) + OTA z kanału BETA przez HTTPS | 🟡 · publikacja BETA w CI (gałąź `beta`), niesprawdzone na sprzęcie |
| Historia alarmów (odczyt rejestrów 0x4011‑0x404B) | ⬜ (`/api/alarms` zwraca pustą listę) |
| Wykrywanie konfliktu węzła CAN (domyślnie 95), synchronizacja czasu, TLS | ⬜ |
| UI z ESP | usunięte celowo (zostaje mała strona serwisowa) |

## Klienci
| Element | Stan |
|---|---|
| Android (Kotlin/Compose): Pulpit (łuk + karty wg konfiguracji), ekrany, nastawy, harmonogramy, konfiguracja kotła, moduł ESP, PIN aplikacji, tryb demo | ✅ kompilacja i uruchomienie w emulatorze (CI, zrzuty w `docs/zrzuty_android/`); 🟡 połączenie z ESP |
| Android: **parowanie tokenem, lista wielu urządzeń, wykrywanie mDNS** | ⬜ (aplikacja loguje się jeszcze hasłem) |
| Android: odcisk palca, WebSocket (teraz odpytywanie co 1 s) | ⬜ |
| Klient WWW (`ui/`) z serwerem‑atrapą | ✅ test e2e (Playwright); nieserwowany z ESP |
| Klient Windows | ⬜ (pomysł: oparty o `ui/`) |
| Integracja Home Assistant (`ha/`) | ⬜ |

## Dane i analiza
`data/device_model.json` (655 PID-ów, listy, alarmy, harmonogramy…) ✅ walidacja (`python3 analiza/tools/validate_ui_model.py` → PASS); kopia produktowa zgodna ze źródłem w `analiza/` (CI). Komplet danych do zbudowania interfejsu: `docs/interfejs_kompletny.json`. Plik firmware kotła, dekompilacja i narzędzia emulacji są w `analiza/` (nie uruchamiane ponownie po przeniesieniu poza walidatorem).

## Czego NIE sprawdzono (ważne)
Nic nie było uruchomione na prawdziwym kotle, module ESP ani telefonie. W szczególności niezweryfikowane: działanie MCP2515/CAN, Wi‑Fi, mDNS, zmiana IP/Wi‑Fi z cofnięciem, awaryjny AP, OTA (także BETA po HTTPS), zapis jakichkolwiek wartości do kotła (harmonogramy, nastawy, konfiguracja), test wyjść (sterowanie), okres odpytywania i tempo animacji panelu.

## Jak sprawdzić stan samemu
`sh esp/components/tekla_core/test/run.sh` · `python3 analiza/tools/validate_ui_model.py` · `cd tools/mock && npm i && node server.js` i `node e2e.js` · CI (`.github/workflows/ci.yml`): kompilacja ESP, APK, zrzuty z emulatora.


## Układ flash (4 MB) i rozmiar firmware
NVS 24 kB · otadata 8 kB · phy 4 kB · **ota_0 i ota_1 po 0x1E0000 (1,875 MB)** · store 128 kB (0x3E0000). Brak partycji UI (klienci mają własny interfejs). Obraz z BLE+HTTPS+mDNS przy `-Os` zajmuje ok. 1,34 MB, czyli w slocie zostaje ok. 0,5 MB zapasu. Zmiana tablicy partycji wymaga wgrania **jednego pliku `tekla_pellet_flash.bin`** (artefakt CI, adres 0x0) przez UART/USB; kolejne aktualizacje już przez OTA samym `tekla_pellet.bin`.
## Aktualizacja 2026-11-26 — przygotowanie pierwszego testu sprzętowego
- Web installer został utwardzony: przed automatycznym flashowaniem pobiera `manifest.json`, sprawdza projekt/układ/adres/tryb kasowania, rozmiar obrazu i SHA-256; błędny obraz nie jest flashowany.
- Workflow `.github/workflows/firmware-installer.yml` sprawdza rozmiar `tekla_pellet.bin` względem pojedynczego slotu OTA (1,875 MiB); powyżej 1,60 MiB daje ostrzeżenie, a przekroczenie slotu zatrzymuje build.
- Instalator nadal wykonuje pełne kasowanie i zapis `tekla_pellet_flash.bin` od `0x000000`. Po takim flashu NVS jest puste i urządzenie powinno wejść w pierwszą konfigurację.
- Po flashu plan testu właściciela: BLE provisioning Android → Wi-Fi → mDNS → `/api/identity` → `/api/pair` → REST/WebSocket. Następnie dopiero odczyt CAN z prawdziwego kotła.
- Stan: zmiany kodu przygotowane; wynik CI i faktyczny rozmiar obrazu z bieżącego builda należy potwierdzić po zakończeniu workflow. Sprzęt nadal nie był testowany.

## Przejęcie projektu
Stan i zasady przejęcia z 2026-11-26 są zapisane w `docs/PRZEJECIE_PROJEKTU_2026-11-26.md`. Dokument rozróżnia TESTED/CONFIRMED, CI PASS, EMULATED, MOCK, INFERENCE i OPEN oraz wskazuje kolejność dalszych prac.
