# API modułu ESP (REST + WebSocket) - referencja zgodna z kodem

Stan: zgodne z `esp/main/web.cpp` (firmware 0.2, **API wersja 2**). Skompilowane w CI, **nieuruchamiane na sprzęcie**. Starszy szkic projektowy: `ESP_API_V2.md` (historia, część nieaktualna). Serwer-atrapa z tym samym kontraktem do testów klienta: `tools/mock/server.js` (hasło `demo`; może nie obsługiwać wszystkich punktów z sekcji 5-8).

## 1. Zasady ogólne
- HTTP (bez TLS) w sieci lokalnej lub przez VPN, port 80. Treść: JSON UTF‑8. Odpowiedź zawsze zawiera `"ok": true|false`; przy błędzie `"code"` (tabela w pkt 10).
- Uwierzytelnienie: nagłówek `Authorization: Bearer <token>`. Token pochodzi z `POST /api/login` (sesja, wygasa po 12 h bezczynności) **albo** z `POST /api/pair` (**stały token klienta**, bez wygaśnięcia, odwoływany ręcznie; zapamiętany w aplikacji).
- Bez tokenu dostępne tylko: `GET /api/identity`, `POST /api/login`, `POST /api/pair`, `POST /api/setup` (tylko przed konfiguracją) i strona serwisowa `GET /`.
- Limit prób haseł: 5 błędnych / 60 s na adres IP (HTTP 429 `locked`); PIN poziomów: 5 błędnych → blokada 5 min.
- Trzy niezależne bramki: **tryb zapisu do kotła** (`readonly`/`full`, globalny dla urządzenia, po restarcie zawsze `readonly`), **poziom PIN 0‑3** (per klient; auto‑blokada po 5 min bezczynności), **odblokowanie serwisowe** (hasło, 5 min, per klient; dla operacji na module, nie na kotle) i **konto DEV** (osobne hasło, 5 min, dla OTA BETA).

## 2. Wykrywanie i tożsamość
- **mDNS**: usługa `_teklapellet._tcp` (port 80), host `teklapellet-<ostatnie 6 znaków MAC>.local`, instancja = nazwa urządzenia. TXT: `id`, `name`, `fw`, `api`, `node`. Pole `id` (`tp-<12 hex z MAC>`) jest **stałym, unikalnym identyfikatorem** urządzenia (używaj go jako klucza zamiast IP/nazwy; obsługa wielu urządzeń).
- `GET /api/identity` (bez uwierzytelnienia): `{ok, id, name, model:"TeklaPellet", fw, api, node, setup_needed}`. Klient powinien sprawdzić `api` (obecnie `2`) i zgłosić niezgodność.
- mDNS bywa niedostępny między sieciami/VLAN-ami: klient musi pozwalać dodać urządzenie ręcznie po adresie.

## 3. Pierwsza konfiguracja i parowanie
1. **Konfiguracja (jednorazowa)**: urządzenie bez konfiguracji tworzy otwarty AP `TeklaPellet-<xxxxxx>`. Połącz się i otwórz `http://192.168.4.1/` (strona serwisowa) albo wywołaj `POST /api/setup` `{password, pin1, pin2, pin3, ssid, wifi_pass}` (hasło ≥ 8 znaków). Zapis w NVS (przeżywa restart, zanik zasilania i aktualizacje firmware); urządzenie restartuje się i łączy z Wi‑Fi. Powtórzenie → 409 `already_configured`.
2. **Parowanie klienta**: `POST /api/pair` `{password, name}` (`name` 1‑23 znaków, bez `"` i `\`) → `{ok, token, id}`. **Token zapisz w kliencie** (43 znaki, pokazywany tylko raz; w urządzeniu tylko SHA‑256). Maks. 8 klientów (409 `too_many_clients`). Dalej samo `Authorization: Bearer <token>`, hasło niepotrzebne. Zmiana hasła urządzenia **nie** odwołuje tokenów; odwołanie: pkt 8.
3. **Sesja z hasła** (np. strona serwisowa): `POST /api/login` `{password}` → `{ok, token, mode, level:0}` (12 h).

## 4. Informacje i tryby
- `GET /api/info` → `{ok, id, name, fw, api, node, can, uptime, mode, level, client}` (`client:true` gdy token sparowanego klienta).
- `POST /api/mode` `{mode:"full"|"readonly", password}` → `{ok, mode}` (włączenie zapisu do kotła wymaga hasła; globalne).
- `POST /api/level` `{pin}` → `{ok, level}` (poziom 1‑3 wg PIN-u; błędny → 403 `bad_pin`, blokada → 423 `locked`); `DELETE /api/level` → poziom 0.

## 5. Dane z kotła
- `GET /api/state` → `{ok, v:{"0x2001":66.9, ...}}` (ostatnie znane wartości „na żywo”; jednostki wyświetlane, stany wyjść jako 0/1/2).
- `GET /api/params?list=<klucz>` (np. `0x00|KOCIOL|inst1`; klucze list w `data/device_model.json` → `lists`) lub `GET /api/params?pids=0x2209,0x220A,...` (do ok. 40) → `{ok, values:{"0x2003":65,...}}` (odczyt z kotła przez SDO; brakujące PID-y pomijane).
- `PUT /api/param/{pid}` `{value}` (np. `/api/param/0x2003`) → `{ok, v}`; wartość po zapisie i **odczycie zwrotnym** (przy niezgodności 502 `verify_failed`). Warunki: tryb `full`, PID z `panel_write`, nie na liście zablokowanych, poziom PIN ≥ wymagany dla wiersza, wartość w zakresie. Wartość w jednostkach wyświetlanych (skala z modelu).
- `GET /api/alarms` → `{ok, alarms:[{n,code,text,active,acked}]}` (**obecnie pusta lista**: odczyt historii niezaimplementowany).
- `GET /api/network` → `{ok, nodes:[{node,name,state,age_s}]}` (uproszczone: kocioł i ten moduł).
- `GET /api/log` → `{ok, lines:[...]}` (ostatnie 12 zdarzeń).

## 6. WebSocket `/ws`
Po otwarciu: klient wysyła `{"t":"auth","token":"<token>"}` → `{"t":"hello"}` (albo `{"t":"err"}`) i od razu pełną migawkę `{"t":"live","v":{...}}`. Wybór zestawu odpytywanych danych: `{"t":"sub","sets":["kociol","palnik","co1","cwu","bufor","solary","magazyn"]}`. Serwer wysyła zmiany co ok. 250 ms: `{"t":"live","v":{"0x2001":66.9,...}}` i `{"t":"param","pid":"0x2003","v":65}` (po zapisie przez dowolnego klienta).

## 7. Moduł: sieć, nazwa, hasło (operacje serwisowe)
Wymagają wcześniejszego **odblokowania serwisowego** `POST /api/service/unlock` `{password}` → `{ok, valid_s:300}`; bez niego 403 `service_locked`.
- `GET /api/wifi` → `{ok, ssid, connected}`; `GET /api/wifi/scan` → `{ok, networks:[{ssid,rssi}]}`.
- `POST /api/wifi` `{ssid, pass}` → `{ok, apply_s:40}`; łączenie asynchronicznie, **bez IP w 40 s moduł wraca do poprzedniej sieci** (nowa zapisywana dopiero po sukcesie). Adres może się zmienić: znajdź urządzenie po `id`.
- `GET /api/ip` → `{ok, saved:{dhcp,ip,mask,gw,dns1,dns2}, current:{...}, pending}`; `POST /api/ip` `{dhcp, ip, mask, gw, dns1, dns2}` (przy `dhcp:true` pozostałe pomijane) → `{ok, confirm_within_s:60, new_ip}`. **Potwierdź z nowego adresu** `POST /api/net/confirm` w 60 s (wystarczy token, bez odblokowania serwisowego), inaczej powrót do poprzednich ustawień. Walidacja: maska ciągła /1‑/30, IP i brama w jednej podsieci, nie adres sieci/rozgłoszeniowy (400 `bad_ip`).
- `POST /api/device` `{name}` → zmiana nazwy (aktualizuje też mDNS); `POST /api/password` `{new}` (≥ 8 znaków; zrywa sesje z hasła, tokeny klientów zostają).
- `POST /api/restart` `{}`; `POST /api/factory_reset` `{confirm:"RESET"}` (kasuje hasło, PIN-y, Wi‑Fi, IP, nazwę, klientów, hasło DEV).
- Awaryjny AP: po > 3 min bez Wi‑Fi moduł włącza otwarty AP `TeklaPellet-<xxxxxx>` (tylko strona serwisowa; operacje wymagają hasła); wyłącza się po powrocie sieci.

## 8. Sparowani klienci (wymaga odblokowania serwisowego do zmian)
`GET /api/clients` → `{ok, clients:[{i,name}]}`; `DELETE /api/clients/{i}` (po numerze) lub `DELETE /api/clients` (wszystkich). Odwołany token przestaje działać natychmiast.

## 9. Aktualizacje firmware i konto DEV
- **OTA z pliku**: `POST /api/ota/fw` (odblokowanie serwisowe), ciało = obraz `.bin` + **64 bajty podpisu ECDSA P‑256 (r||s) na końcu**; nowy slot uruchamia się po poprawnym podpisie, a **bez zdrowego startu w ok. 30 s (HTTP + CAN) następuje automatyczny powrót do poprzedniego** firmware. Podpisywanie: `tools/ota_sign.py`; klucz prywatny poza repo, publiczny w `esp/main/ota_pubkey.pem` (**w repo jest placeholder: do czasu wstawienia własnego klucza aktualizacje są odrzucane**).
- **Konto DEV** (osobne hasło, min. 8 znaków): `GET /api/dev` → `{ok, configured, unlocked, beta_url}`; `POST /api/dev/set` `{password}` (wymaga odblokowania serwisowego; gdy już ustawione, także odblokowanego DEV); `POST /api/dev/unlock` `{password}` (5 min).
- **Kanał BETA** (po odblokowaniu DEV; pobieranie przez HTTPS z `beta_url`, domyślnie wydanie GitHub `beta`): `GET /api/dev/beta` → `{ok, version, current}`; `POST /api/dev/beta/update` (start pobierania `tekla_pellet_beta.bin` z podpisem, weryfikacja tym samym kluczem, restart); `GET /api/dev/beta/status` → `{ok, state, pct, msg}` (`state`: 0 bezczynny, 1 sprawdzanie, 2 pobieranie, 3 weryfikacja, 4 gotowe, 5 błąd); `POST /api/dev/beta/url` `{url}` (https, zmiana źródła). Publikacja BETA wymaga sekretu z kluczem podpisu w CI (patrz `.github/workflows/ci.yml`).

## 10. Błędy
| HTTP | `code` | znaczenie |
|---|---|---|
| 400 | `bad_request`, `bad_name`, `bad_ip`, `bad_url`, `out_of_range`, `confirm_required` | zły format/wartość |
| 401 | `unauthorized`, `bad_password` | brak/zły token, złe hasło |
| 403 | `service_locked`, `dev_locked`, `bad_pin`, `bad_password`, `level_too_low`, `blocked` | brak odblokowania/poziomu; parametr zablokowany |
| 404 | `unknown_pid`, `unknown_client`, `unknown_list` | nie ma takiego zasobu |
| 409 | `read_only_mode`, `not_writable`, `no_encoding`, `already_configured`, `too_many_clients`, `busy` | stan nie pozwala |
| 422 | `bad_signature`, `bad_pack` | błędny podpis/plik |
| 423/429 | `locked` | zbyt wiele błędnych prób |
| 502 | `can_timeout`, `verify_failed`, `http_*` | kocioł nie odpowiada / odczyt zwrotny się nie zgadza / pobieranie BETA |

## 11. Czego jeszcze nie ma / niezweryfikowane
Odczyt historii alarmów; wykrywanie konfliktu węzła CAN (domyślnie 95); HTTPS i parowanie bez przesyłania hasła jawnie (hasło idzie po HTTP w sieci lokalnej/VPN); wszystkie punkty niesprawdzone na sprzęcie (w tym zmiana IP/Wi‑Fi z cofnięciem, awaryjny AP, OTA BETA po HTTPS).
