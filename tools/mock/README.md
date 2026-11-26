# tools/mock - serwer‑atrapa modułu ESP (narzędzie deweloperskie)

**Po co istnieje:** nikt pracujący nad klientami (Android, WWW, Windows, HA) nie ma w CI ani w piaskownicy prawdziwego kotła ani modułu ESP. Atrapa udaje moduł **zgodnie z kontraktem `docs/API_ESP.md`** (tożsamość, parowanie tokenem, odczyt/zapis PID, tryby i PIN-y, odblokowanie serwisowe, klienci, IP, Wi‑Fi, DEV/BETA, WebSocket `/ws`, symulowany cykl pracy palnika). Dzięki niej:
- test e2e klienta WWW (`e2e.js`, Playwright) działa automatycznie bez sprzętu,
- klient (np. Android przeciw `http://<host>:8099`) można rozwijać i sprawdzić **przepływ** (parowanie, przełączanie urządzeń, błędy) zanim będzie sprzęt,
- zmiany kontraktu API da się najpierw wypróbować tanio.

**Czym NIE jest:** nie sprawdza CAN, Wi‑Fi, mDNS, pamięci NVS, OTA ani niczego, co zależy od sprzętu; zachowuje stan tylko w pamięci i upraszcza część zachowań. **Zielony test na atrapie nie znaczy, że moduł działa**; do tego służy lista kontrolna ze sprzętem (`docs/ZADANIA.md` pkt 8). Nie trafia do firmware ani do aplikacji (Android ma własny wbudowany tryb demo).

Uruchomienie: `npm install && node server.js` (port 8099, hasło `demo`, PIN-y `1111/2222/3333`, hasło DEV ustawiasz sam przez `/api/dev/set`). Test: `NODE_PATH=$(npm root -g) node e2e.js` (zrzuty w `/tmp/tp_shots`). Każda zmiana `docs/API_ESP.md` powinna iść w parze ze zmianą atrapy.
