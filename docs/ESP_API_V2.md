# ESP_API_V2 - specyfikacja (WERSJA ROBOCZA do akceptacji przez wlasciciela)

Status: szkic. Kodu jeszcze nie ma. Wszystkie punkty oznaczone **OPEN** wymagaja decyzji/pomiaru.
Decyzje wlasciciela (2026-11-25): projekt niezalezny od starego (bez API v1); ESP serwuje pelna strone z osobnej partycji; OTA z automatycznym cofaniem (firmware i UI w dwoch slotach); na zywo przez WebSocket, nastawy na zadanie; haslo + tryb przy pierwszej konfiguracji; poziomy PIN 0-3 per sesja klienta, wymuszane w ESP, wartosci PIN-ow jak w panelu (ustawiane przy konfiguracji, nie w repo).

## 1. Sprzet i magistrala
- Seeed XIAO ESP32-C6 (flash 4 MB) + MCP2515 (SPI), CAN 125 kbit/s (CANopen).
- Kociol = wezel 4 (SDO: zapytanie 0x604, odpowiedz 0x584). Wezel ESP konfigurowalny; **domyslny Node ID = 95 (0x5F)** (decyzja wlasciciela; wolny, poza klasami modulow producenta -> panel pokazuje UNKNOWN; obecny 87 kolidowalby z drugim CAN IO BIG).
- ESP jest dodatkowym kontrolerem: czyta cyklicznie zestaw "live", reszte na zadanie; zapis wylacznie wg trybu i poziomu (pkt 5).

## 2. Uklad pamieci (4 MB) - propozycja
| Partycja | Offset | Rozmiar | Uwaga |
|---|---|---|---|
| bootloader | 0x0 | 32 KB | |
| partition table | 0x8000 | 4 KB | |
| nvs | 0x9000 | 24 KB | konfiguracja, hasha hasel/PIN-ow, wskaznik slotu UI |
| otadata | 0xF000 | 8 KB | |
| phy_init | 0x11000 | 4 KB | |
| ota_0 | 0x20000 | 0x170000 (1,4 MB) | firmware A |
| ota_1 | 0x190000 | 0x170000 (1,4 MB) | firmware B |
| ui_0 | 0x300000 | 0x70000 (448 KB) | interfejs A (LittleFS/pakiet tylko do odczytu) |
| ui_1 | 0x370000 | 0x70000 (448 KB) | interfejs B |
| store | 0x3E0000 | 0x20000 (128 KB) | dane uzytkownika (np. ostatnie odczyty, log) |
Razem 4 MB. **OPEN**: realny rozmiar firmware (po pierwszym buildzie), budzet UI: JS+HTML (min+gzip) ~150 KB + bitmapy panelu ~350 KB PNG = ponad 448 KB -> zmniejszyc zestaw bitmap (nie wszystkie 188 sa potrzebne) lub skorygowac podzial.

## 3. OTA i cofanie
- Firmware: `CONFIG_BOOTLOADER_APP_ROLLBACK_ENABLE`. Nowy slot jest "pending"; oznaczany jako poprawny dopiero po zdrowym starcie: Wi-Fi polaczone + pierwszy poprawny odczyt SDO z kotla (limit ok. 60 s, **OPEN**). Brak potwierdzenia / reset watchdog -> bootloader wraca do poprzedniego slotu.
- Interfejs: dwa sloty `ui_0/ui_1`, aktywny wskazany w NVS z licznikiem prob; nowy UI zostaje aktywny po potwierdzeniu przez klienta (`POST /api/ui/confirm`), inaczej po N nieudanych ladowaniach wraca do poprzedniego.
- Wgrywanie: `POST /api/ota/fw`, `POST /api/ota/ui` (binarnie, tylko admin, tryb "pelna kontrola" nie jest wymagany, wymagane haslo). Podpis/hasz: **OPEN**.

## 4. Pierwsza konfiguracja i autoryzacja
1. Pierwsze uruchomienie: ESP w trybie AP (`TeklaPellet-XXXX`) + strona konfiguracji. Wpisuje sie: Wi-Fi, **haslo administratora** (potrzebne integracji HA i aplikacji Android), **tryb** (`readonly` | `full`), **PIN-y poziomow 1-3** (wartosci jak w panelu; podaje wlasciciel; nigdy do repo), nazwa urzadzenia.
2. Hasla i PIN-y w NVS wylacznie jako hasz z sola (PBKDF2/SHA-256). **OPEN**: parametry.
3. `POST /api/login {password}` -> token sesji (losowy, wygasa, **OPEN**: czas). Kazdy klient ma wlasna sesje.
4. Poziom sesji (0-3): `POST /api/level {pin}` podnosi poziom tej sesji; autoblokada po bezczynnosci (**OPEN**: czas, wartosci z panelu nie sa zmierzone); `DELETE /api/level` obniza.
5. Zmiana trybu: z ustawien klienta, wymaga hasla (`POST /api/mode {mode, password}`).

## 5. Tryby i bramki zapisu
- `readonly`: kazda operacja zapisu -> 403 (`code: "readonly"`). Odczyty dzialaja.
- `full`: zapis dozwolony, jezeli (a) sesja ma poziom >= `level_min` PID z modelu, (b) PID jest w liscie zapisywalnych (**OPEN**: zakres pierwszej wersji - wlasciciel chce poczatkowo waski), (c) kodowanie wg `pids[pid].panel_write` z `device_model.json`.
- Po zapisie ESP czyta wartosc zwrotnie i odsyla potwierdzona wartosc; rozbieznosc -> blad `verify_failed`.
- Surowego zapisu SDO nie ma w pierwszej wersji. Surowy ODCZYT SDO (`GET /api/sdo/{index}`) tylko dla admina.

## 6. Transport i wykrywanie
- HTTP (port 80) + WebSocket (`/ws`). TLS: **OPEN** (siec lokalna).
- mDNS: usluga `_teklapellet._tcp`, TXT: `id`, `api=2`, `fw`, `cfg` (znacznik konfiguracji), `mode`. Integracja HA wykrywa przez zeroconf.

## 7. Znacznik konfiguracji i informacje
`GET /api/info` (bez logowania - tylko to, co potrzebne do wykrywania; reszta po logowaniu):
```
{ "api": 2, "id": "...", "name": "...", "fw": "...", "mode": "readonly|full",
  "controller": {"sw": 2118, "short_name": "TE"},     // PID 0x3664 (v2.118), 0x3665
  "config": { "sig": "<hasz>", "regs": {"0x2105": 1, "0x2106": 1, "0x2107": 0, "0x2108": 0, "0x350C": 0, "0x3525": 0, ...} },
  "derived": {"circuits": 1, "cwu": true, "buffer": false, "solar": false, "cascade": false, "store": false},
  "nodes_online": ["0x56", ...] }
```
Rejestry konfiguracji i reguly widocznosci: `device_model.json` -> `configuration`. Klienci rysuja widoki wg `config`/`derived` (wiersze menu, kafle HOME, schematy).

## 8. REST (po zalogowaniu, `Authorization: Bearer <token>`)
| Metoda i sciezka | Opis | Poziom |
|---|---|---|
| `GET /api/model` | wersja i hasz modelu (sam model jest w pakiecie UI) | 0 |
| `GET /api/state` | migawka wartosci "live" | 0 |
| `GET /api/params?screen=KOCIOL` | nastawy ekranu/listy (wartosc, jednostka, min/max, krok, level_min) | wg `level_min` |
| `GET /api/param/{pid}` | jedna wartosc | wg `level_min` |
| `PUT /api/param/{pid}` `{value}` | zapis (wartosc wyswietlana; ESP przelicza wg modelu) | wg `level_min`, tryb `full` |
| `GET/PUT /api/schedule/{ekran}/{dzien}` | maska 24 bitow (kociol, CO1-4, CWU) | 0 / zapis `full` |
| `GET /api/alarms` | historia (kod, aktywny, potwierdzony, czasy) | 0 |
| `POST /api/alarms/ack` | potwierdzenie alarmu | `full` |
| `GET /api/network` | tabela modulow CAN (Node ID, klasa, SW, T) | 0 |
| `GET /api/sdo/{index}?sub=` | surowy odczyt (admin) | admin |
| `POST /api/mode`, `POST /api/level`, `DELETE /api/level`, `POST /api/login`, `POST /api/setup` | autoryzacja i konfiguracja | - |
| `POST /api/ota/fw`, `POST /api/ota/ui`, `GET /api/ota/status`, `POST /api/ui/confirm` | OTA | admin |
Bledy: JSON `{ "ok": false, "code": "...", "msg": "..." }`, kody HTTP: 400/401/403/404/409/423/500.

## 9. WebSocket `/ws` (na zywo)
- Zasady wlasciciela: odczyty na zywo (temperatury, czujniki, stany, diody, animacje) przez push; nastawy na zadanie (REST) i powiadomienie o zmianie dla innych klientow.
- Po polaczeniu klient wysyla `{"t":"auth","token":"..."}` i `{"t":"sub","sets":["home","kociol"]}`.
- Serwer -> klient: `{"t":"live","v":{"0x2001":66.9,"0x2007":0,...}}` (tylko zmiany; temperatury maks. ok. 1 Hz, stany od razu), `{"t":"param","pid":"0x2003","v":65,"by":"<sesja>"}` (zmiana nastawy), `{"t":"alarm",...}`, `{"t":"net",...}`, `{"t":"mode","v":"readonly"}`.
- Klient -> serwer: `{"t":"set","id":1,"pid":"0x2003","v":66}` -> `{"t":"ack","id":1,"ok":true,"v":66}` lub blad jak w pkt 8.
- Zestawy "live" (ktore PID-y, jak czesto) generowane z `device_model.json` (**OPEN**: dobor PID-ow: temperatury, moc palnika, stany pomp/dmuchawy/podajnika/zasilania, kod stanu palnika, zapas paliwa; zrodla diod HOME ustalimy z odczytow na zywym kotle).

## 10. Przeplyw danych (zrodlo prawdy)
`data/device_model.json` -> `tools/` generuje tabele firmware (PID, typ, min/max, kodowanie, `level_min`, flaga live) -> ESP; ten sam model w pakiecie UI dla klientow. Wersja/hasz modelu w `/api/info`.

## 11. Otwarte pytania (do zamkniecia przed kodem)
1. ~~Node ID ESP~~ (zamkniete: 95). 2. Czasy: sesja, autoblokada, zdrowy start (OTA). 3. Zakres zapisow w wersji 1. 4. Zestaw live i czestotliwosci. 5. Budzet UI vs 448 KB. 6. TLS/podpis OTA. 7. Parametry hasha hasel/PIN-ow.

## 12. Decyzje wlasciciela - runda 2 (2026-11-25)
1. **Node ID ESP = 95 (0x5F)**, konfigurowalny.
2. **Zapisy v1: wszystkie nastawy z poziomami PIN** (pelna lista z `device_model.json`, odblokowywana PIN-em poziomu per sesja, tryb `full` wymagany). **Pierwszy test na kotle**: odczyt na zywo + strona (tylko odczyt) oraz **zapis licznika paliwa (`0x3601`)** jako pierwszy zapis przez ESP; pozostale zapisy dopiero po tym ttescie (kolejne etapy testow z wlascicielem przy kotle).
3. **Dane na zywo (decyzja delegowana do agenta; propozycja do akceptacji):** zestaw podstawowy zawsze (ok. 1 Hz temperatury, natychmiast zmiany stanow): temperatury kotla/powrotu/palnika/zewnetrznej/CO/CWU, kod stanu palnika, moc, stany pomp i diod pulpitu (zasilanie, palnik, CO1/CO2/CWU), paliwo i pojemnosc/rezerwa, alarm aktywny. **Dodatkowo zestaw biezacego ekranu** po subskrypcji (`{"t":"sub","screen":"PALNIK"}`): wartosci widoczne na tym ekranie (np. PALNIK: spalanie, moc kW/%, jasnosc, T. palnika, obroty dmuchawy, czas podawania), zwalniane po zamknieciu. Nastawy i listy tylko na zadanie (REST) + powiadomienie o zmianie.
4. Z pomiarow z prawdziwego panelu: diody i ikony pulpitu - patrz `data/device_model.json` -> `home_indicators`, `real_panel_screens`; dolne belki ekranow (animowane wyjscia) pochodza ze stanow wyjsc, nie z parametrow (zrodlo stanow wyjsc OPEN).
5. Odczyty widziane przez integracje HA, ktore sa zle/zerowe wzgledem panelu (moc palnika %, kW, spalanie kg/h) - ESP v2 ma czytac wlasciwe rejestry (OPEN: ustalic PID-y z panelu).
