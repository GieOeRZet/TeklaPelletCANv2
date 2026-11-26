# Instrukcja ciągłości pracy agentów — TeklaPelletCANv2

Aktualizacja: 2026-11-26

## 1. Cel projektu
Budujemy niezależny system dla kotła Estyma igneoTouch.
- ESP32-C6 + MCP2515 jest bramką i sterownikiem między CAN kotła a klientami.
- Android i Home Assistant są klientami.
- ESP obsługuje pełny dwustronny odczyt i zapis: CAN/SDO, zapis parametrów, odczyt zwrotny i potwierdzenie.
- Zachowanie i dostępność funkcji wynikają z rzeczywistego modelu urządzenia i konfiguracji kotła.
- Nie kopiujemy UI Androida ani WWW jako architektury ESP.
- BLE służy do pierwszej konfiguracji i odzyskiwania; normalna komunikacja klientów odbywa się po Wi-Fi/API.
- Strona WWW w ESP jest stroną serwisową, nie głównym interfejsem kotła.

## 2. Obowiązujące decyzje właściciela
1. Brak poziomów dostępu/PIN 1-3. Nie projektować nowych funkcji zależnych od poziomu PIN.
2. Zapis do kotła ma być funkcjonalnie pełny, ale nadal ograniczony do PID-ów, które model oznacza jako zapisywalne i dla których istnieje znane kodowanie. Zawsze wykonuj odczyt zwrotny.
3. Logi mają być dostępne do eksportu.
4. Lista klientów ma pokazywać klientów sparowanych oraz informację, którzy są aktualnie podłączeni przez WebSocket.
5. Dokumentacja jest częścią implementacji: po każdym większym etapie aktualizuj status, kontrakt API i ten dokument.
6. Nie deklaruj TESTED dla sprzętu, jeżeli test był tylko w CI, emulatorze albo mocku.
7. Nie wracaj do pełnego reverse engineeringu bez konkretnej luki potrzebnej produktowi.

## 3. Kolejność zapoznania się z repozytorium
Nowy agent czyta dokładnie w tej kolejności:
1. README.md
2. docs/AGENT_CONTINUITY.md
3. docs/PRZEJECIE_PROJEKTU_2026-11-26.md
4. docs/STATUS.md
5. docs/ZADANIA.md
6. docs/API_ESP.md
7. docs/INSTRUKCJA_BUDOWY_NIEZALEZNEJ.md
8. data/device_model.json
9. data/menu_rows.json
10. odpowiednie źródła analiza/, tylko gdy implementowana funkcja wymaga ich dowodu
11. kod konkretnego modułu dopiero po ustaleniu kontraktu.
Przed zmianą należy przeczytać pełną treść plików bezpośrednio związanych z funkcją.

## 4. Źródła prawdy
- RE i dowody: analiza/
- model produktu: źródła analizy -> data/device_model.json
- konfiguracja/menu: data/menu_rows.json
- kontrakt ESP: docs/API_ESP.md
- stan projektu: docs/STATUS.md
- backlog: docs/ZADANIA.md
- klient referencyjny: ui/
- Android: android/
- HA: ha/

Nie wpisuj wiedzy do data/device_model.json, jeśli powinna zostać dodana do źródeł analizy i wygenerowana.

## 5. Jak pracować
Przed zmianą:
- sprawdź aktualny HEAD i gałąź;
- pobierz dokładne wersje plików, których dotkniesz;
- ustal, czy zmiana jest kodem, kontraktem API, generowanym artefaktem czy dokumentacją;
- nie mieszaj zmian niezwiązanych z zadaniem.
Po zmianie:
- uruchom dostępne testy lokalne;
- sprawdź kompilację/CI;
- jeżeli zmieniono kontrakt API, zaktualizuj docs/API_ESP.md i tools/mock/;
- jeżeli zmieniono model, uruchom generatory;
- zaktualizuj docs/STATUS.md;
- wpisz decyzje i skutki do tego dokumentu, jeżeli zmiana wpływa na przyszłych agentów;
- commit + push na domyślną gałąź projektu.

## 6. Statusy dowodów
- TESTED / CONFIRMED — rzeczywisty test/pomiar;
- CI PASS — test/kompilacja w CI;
- EMULATED — emulator;
- MOCK — serwer-atrapa;
- INFERENCE — wniosek;
- OPEN — nierozstrzygnięte.
Kompilacja nie oznacza działania na ESP.

## 7. ESP — obowiązująca architektura
esp/ jest aktywnym firmware.
Android/HA -> Wi-Fi/API -> ESP32-C6 -> MCP2515 -> CAN 125 kbit/s -> Estyma node 4
ESP ma własny model, walidację, kodowanie, polling, SDO, zapis i odczyt zwrotny.
Stałe przyjęte przez projekt: ESP node 95, kocioł node 4, CAN 125 kbit/s, XIAO ESP32-C6 + MCP2515. Bieżące piny są w esp/main/config.hpp; nie kopiuj ich z archiwum_v1.

## 8. Zapis do kotła
Nie dodawaj surowego zapisu omijającego model.
Prawidłowy przepływ: klient -> autoryzacja -> PID/model -> zakres -> kodowanie -> CAN/SDO -> odczyt zwrotny -> potwierdzony stan -> WebSocket.
Dla błędu CAN lub rozbieżności read-back klient dostaje błąd, a nie fałszywe potwierdzenie.
Poziomy PIN nie są elementem tego przepływu.

## 9. Logi
Logi mają być użyteczne diagnostycznie i eksportowalne. Każdy agent dodający istotne zdarzenie powinien użyć istniejącego mechanizmu logowania.

## 10. Klienci
Rozróżniaj klienta sparowanego, który posiada trwały token, oraz klienta aktywnie podłączonego przez WebSocket. Lista klientów ma pokazywać oba stany.

## 11. Czego nie robić
- Nie przenoś starego ESPHome jako podstawy obecnego firmware.
- Nie przywracaj UI do ESP jako głównego interfejsu.
- Nie kopiuj Androida/WWW do firmware.
- Nie wymyślaj PID-ów, kodowania, zakresów ani konfiguracji.
- Nie oznaczaj funkcji jako działającej na sprzęcie bez fizycznego testu.
- Nie umieszczaj sekretów, PIN-ów, haseł, SSID ani tokenów w repo.
- Nie twórz nowych poziomów dostępu.

## 12. Raport końcowy każdego etapu
**WYKONANE** — ...
**ZMIENIONE PLIKI** — ...
**ZACHOWANE** — ...
**WERYFIKACJA** — ...
**SKUTKI UBOCZNE** — ...
**OTWARTE** — ...
**NASTĘPNY KROK** — ...

Ten dokument ma sprawić, że kolejny agent kontynuuje pracę, a nie odtwarza historię rozmów.