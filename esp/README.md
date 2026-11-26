# esp - firmware ESP32-C6 (XIAO) + MCP2515

Cienki most CAN <-> API (REST + WebSocket) z malą wbudowaną stroną serwisową. **UI nie jest serwowane z ESP** (robią to klienci). Kontrakt: `../docs/API_ESP.md`. Stan prac: `../docs/STATUS.md`.

## Stan (uczciwie)
| Element | Weryfikacja |
|---|---|
| `components/tekla_core` (ramki SDO, tablice modelu GENEROWANE, reguły ikon/stanów, strażnik zapisu, live, JSON, base64url, ECDSA r\|s->DER, walidacja IP, ID z MAC) | **testy natywne PASS**: `sh components/tekla_core/test/run.sh` |
| `components/mcp2515`, `main/` (bus, auth, ota, web, net, device, service_page, main) | **kompilacja ESP-IDF 5.4.1 (esp32c6) w GitHub Actions: PASS** (kontener `espressif/idf:v5.4.1`). **NIEURUCHAMIANE na sprzęcie**: żadnej weryfikacji MCP2515/CAN/Wi-Fi/mDNS/HTTP/OTA |

## Pliki w `main/`
`main.cpp` start · `bus.*` kolejka i poller CAN, tożsamość węzła, heartbeat · `web.cpp` REST/WS + strona serwisowa · `service_page.hpp` wbudowana strona (pierwsza konfiguracja, Wi-Fi, IP, nazwa, hasło, klienci, firmware, DEV/BETA) · `auth.*` hasło, sesje, parowanie tokenem (SHA-256), PIN-y poziomów, odblokowanie serwisowe, konto DEV · `net.*` Wi-Fi/AP, zmiana sieci i IP z cofnięciem, awaryjny AP, mDNS · `device.*` ID z MAC, nazwa, host · `ota.*` OTA z pliku i z kanału BETA (podpis ECDSA P-256) · `config.hpp` stałe (piny, węzły, TTL, wersja API).

## Budowanie
CI buduje przy zmianach w `esp/`, `ui/`, `tools/`, `data/`, `android/` (artefakt `tekla-firmware`). Lokalnie (ESP-IDF 5.4.x): `cd esp && idf.py set-target esp32c6 && idf.py build`. Wymaga komponentu `espressif/mdns` (`main/idf_component.yml`). Partycje: `partitions.csv` (nvs, otadata, phy, ota_0, ota_1, store; brak partycji UI). Piny MCP2515: `main/config.hpp` (SCK 19, MISO 20, MOSI 18, CS 17, INT nieużywany, kwarc 16 MHz, SPI 1 MHz).
**Klucz OTA:** w `main/ota_pubkey.pem` jest placeholder; do czasu wstawienia własnego klucza publicznego (`python3 ../tools/ota_sign.py keygen priv.pem pub.pem`, klucz prywatny poza repo) każda aktualizacja jest odrzucana.

## Zachowanie (skrót)
Domyślnie tryb **tylko odczyt** (po restarcie zawsze). Zapis do kotła: tryb `full` (hasło) + poziom PIN + strażnik `tekla::plan_write` (PID poza listami panelu zablokowane) + odczyt zwrotny. Operacje na module (Wi-Fi, IP, hasło, klienci, OTA, restart, reset) wymagają odblokowania serwisowego hasłem (5 min). Slot firmware oznaczany poprawnym po 30 s przy działających HTTP i CAN, inaczej rollback. Hasło, PIN-y, Wi-Fi i tokeny klientów są w NVS (przeżywają restart, zanik zasilania i aktualizacje; kasuje je tylko reset fabryczny).
Nie zaimplementowane: historia alarmów, wykrywanie konfliktu węzła CAN, synchronizacja czasu, TLS.
