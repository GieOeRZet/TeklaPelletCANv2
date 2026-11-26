# esp - firmware ESP32-C6 (XIAO) + MCP2515

## Stan (uczciwie)
| Element | Weryfikacja |
|---|---|
| `components/tekla_core` (ramki SDO, tablice modelu GENEROWANE, reguly ikon/stanow, straznik zapisu, live, JSON, base64url, paczka UI TPUI, ECDSA r\|s->DER) | **testy natywne PASS**: `sh components/tekla_core/test/run.sh` |
| `components/mcp2515`, `main/` (bus, auth, ota, web, net, main) | **kompilacja ESP-IDF 5.4.1 (esp32c6) w GitHub Actions: PASS** (workflow `.github/workflows/ci.yml`, kontener `espressif/idf:v5.4.1`, jak w starym projekcie; run 2, artefakt `tekla-firmware`). **NIEURUCHAMIANE na sprzecie** - zadnej weryfikacji dzialania MCP2515/CAN/Wi-Fi/HTTP |

## Budowanie
CI buduje automatycznie przy zmianach w `esp/`, `ui/`, `tools/`, `data/` (artefakt do pobrania z zakladki Actions). Lokalnie (ESP-IDF 5.x):
```
cd esp && idf.py set-target esp32c6 && idf.py build
```
Wymaga zgodnie z `idf_component.yml`: `espressif/mdns`. Przed budowa: klucz OTA (`python3 tools/ota_sign.py keygen priv.pem pub.pem`, `pub.pem` -> `main/ota_pubkey.pem`) i paczka UI (`python3 tools/pack_ui.py`, wgrywana do partycji `ui_0`: `esptool.py write_flash 0x300000 ui/ui_pack.bin`).
Piny MCP2515 (XIAO ESP32-C6): `main/config.hpp` (SCK 19, MISO 20, MOSI 18, CS 1, INT 2 - do potwierdzenia z okablowaniem), kwarc 8 MHz (zmiana `kOsc16MHz`).

## Zachowanie
- Domyslnie tryb **tylko odczyt**; zapis tylko po `POST /api/mode` (haslo), poziomie PIN sesji, przez straznik `tekla::plan_write` (40 PID poza listami panelu = `WE_BLOCKED`), z odczytem zwrotnym po zapisie.
- Podpis OTA: **ECDSA P-256 + SHA-256** (zamiast Ed25519 z sekcji 13.2 specyfikacji: mbedtls w ESP-IDF ma ECDSA); brak/niepoprawny klucz publiczny = kazde OTA odrzucone.
- Slot FW oznaczany poprawnym po 30 s przy dzialajacych HTTP i CAN (pierwszy odczyt SDO nie jest warunkiem); slot UI wymaga `POST /api/ui/confirm`, po 2 nieudanych startach powrot do poprzedniego.
- Nie zaimplementowane jeszcze: historia alarmow (`/api/alarms` zwraca pusta liste), harmonogramy (`/api/schedule`), tabela modulow CAN (`/api/network`), `/api/sdo`, synchronizacja czasu, TLS.
