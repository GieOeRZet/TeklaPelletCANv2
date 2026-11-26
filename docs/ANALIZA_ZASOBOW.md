# Analiza zasobów: co mamy, co się nada, czego brakuje (2026-11-26)

Przejrzane: `TeklaPelletCAN` (stary projekt: firmware v1, strona WWW, karty HA, integracja HA, aplikacja Android WebView, dokumentacja), `PelletCAN` (ESPHome, sprzęt i tożsamość węzła), dane analizy panelu (`device_model.json`, atlas ekranów, bitmapy). Projekt v2 jest niezależny (bez zgodności z API v1) - bierzemy pomysły i sprawdzone rozwiązania, nie kod 1:1.

## 1. Co jest w starym projekcie i co z tego bierzemy

| Obszar | Stan w starym projekcie | Decyzja dla v2 |
|---|---|---|
| **Strona urządzenia** (`web/settings.html`) | sekcje: Urządzenie, Tryb pracy, Wygląd (grafika kotła), Sieć Wi-Fi (skan + status), Aktualizacja oprogramowania (+ rollback), Dziennik i diagnostyka, Hasło urządzenia, Sparowani klienci, Serwis (restart, reset fabryczny, eksport) | **bierzemy zakres funkcji**: ekran „Moduł” (Wi-Fi ze skanem, hasło, tryb, OTA + podpis, dziennik, restart, reset fabryczny, informacje o CAN/węźle). Parowanie klientów **pomijamy** (v2: jedno hasło + sesje, decyzja wlasciciela). |
| **Portal pierwszego uruchomienia** (`web/portal.html`, AP + captive) | AP `TeklaPelletCAN-XXXX`, wybór sieci, hasło urządzenia, stałe IP | **bierzemy**: pierwsza konfiguracja `POST /api/setup` + AP otwarty tylko do konfiguracji; dopisać skan sieci i stały IP w v2. |
| **Pulpit / instalacja / nastawy / alarmy / wszystkie parametry** (`web/js/views.js`, Android `ui`) | pulpit z grafiką kotła, schemat instalacji (zbiornik→palnik→kocioł→CO/CWU), nastawy z trybami, alarmy, generowana lista wszystkich parametrów z poziomami (kłódki) | **bierzemy układ**: pulpit + schemat instalacji + „wszystkie parametry" (generowane z modelu). Mamy już pulpit, ekrany z dolnymi belkami, listy nastaw (wierniejsze: z tabeli panelu). |
| **Aplikacja Android** (WebView + natywny proxy Kotlin + JNI z rdzeniem firmware) | PIN aplikacji (PBKDF2, 5 prób), odcisk palca, blokada zapisu 5 min, poziomy kotła 1/2, mDNS + ręczny adres, offline z ostatnimi danymi, tryb demo (rdzeń C++ przez JNI), aktualizacje | **bierzemy zakres funkcji, nie architekturę**: v2 = natywna aplikacja Kotlin + Jetpack Compose (bez WebView), grafiki wbudowane, tryb demo własny w Kotlinie. PIN aplikacji, offline, mDNS, poziomy PIN - do przeniesienia. |
| **Oryginalna karta panelu** (`tekla-panel-card.js`, 97 KB; `teklapelletclassic-engine.js`, 146 KB; 11 języków) | replika wyglądu panelu: Konfiguracja, Test wyjść, Statystyki, Sieć, harmonogramy, grafiki 189 PNG | **wzorzec wyglądu i zakresu ekranów**; grafiki - ten sam zestaw bitmap panelu (u nas 56 w paczce UI, w aplikacji pełny potrzebny zestaw). |
| **Integracja HA** (`custom_components/teklapelletcan`) i karty HA | config flow z zeroconf, encje z `/points`, update entity, karty classic/modern | **poza zakresem tego etapu** (CLAUDE.md: ESP/HA/Android po rewersie; HA dopiero po stabilnym API v2). |
| **Aktualizacje** (manifest, podpis ECDSA, rollback, zdrowy start) | w pelni opisane (`docs/ARCHITEKTURA.md` 12a) | **zgodne z v2**: ECDSA P-256, rollback, healthy start - mamy w firmware; brakuje manifestu/aktualizatora po stronie klienta. |
| **Sprzęt i tożsamość węzła** (PelletCAN ESPHome) | piny, 16 MHz, SPI 1 MHz, heartbeat, SDO 0x1000/0x1008/0x100A/0x1018, antena | **zrobione** (firmware v2). |

## 2. Czego brakuje (uczciwie)

1. **Harmonogramy**: kodowanie z panelu mamy (maski 24-bit na dzień, PID-y per obwód: `program_pids.json`, ramki zapisu zmierzone w emulatorze), **ale żaden zapis harmonogramu nie był sprawdzony na prawdziwym kotle**. UI (WWW i Android) zrobimy z pełną edycją, w trybie `full` i po PIN-ie; do testu na kotle potrzebna Twoja zgoda.
2. **Test wyjść**: wiemy które PID-y testowe zapisać (A/OFF/ON, mieszacze A/CLOSE/OPEN), ale **nie wiemy jak sterownik reaguje i czy sam wraca na Auto**; stary firmware dodawał powrót po 120 s. W v2: ekran tylko do podglądu stanów, zapis testów po osobnej zgodzie.
3. **Statystyki** (lista 0x11, liczniki tylko do odczytu): PID-y znane, brak ekranu.
4. **Tabela modułów CAN / Sieć** (węzły, klasa, wersje): dane w `siec_modules.json`; brak odczytu na ESP (wymaga skanu SDO 0x1000/0x1018 węzłów).
5. **Historia alarmów**: kody i teksty zweryfikowane (71), rejestry historii znane (`0x4011-0x404B`), brak implementacji odczytu w ESP.
6. **Czas**: panel ma RTC; ESP nie zapisuje czasu (rozjazd ~2 min). Decyzja: ustawianie czasu tylko na żądanie.
7. **Obrazy kotła**: 4 warianty (bitmapy 11-14, wybór `0x370A`), mamy PNG; **brak wyższej rozdzielczości** (oryginał ma tylko to, co jest w panelu).
8. **Zachowanie zależne od konfiguracji** (ilość obwodów 0x2105, CWU 0x2106, bufor 0x2107, solary 0x2108, kaskada 0x350C, magazyn 0x3525, czujnik powrotu 0x350A, ciśnienie 0x350D): reguły z emulatora (`screens_config.json`, 11 kombinacji) - zaimplementujemy widoczność ekranów i kafelków wg nich; nie mamy pomiaru na prawdziwym panelu poza konfiguracją wlasciciela.
9. **Kolor/tempo animacji, okres pollera**: wymagają pomiaru na panelu.
10. **Prawdziwy telefon i kocioł**: nic z v2 nie było uruchomione na sprzęcie (ESP, aplikacja). Aplikacja: kompilacja w CI, brak testu na urządzeniu.

## 3. Plan wykonania (w tej kolejności)

1. **Wspólny model aplikacji** (`tools/gen_app_model.py` -> `ui/gen/app.json` i `android/.../assets/app.json`): konfiguracja kotła (wiersze KONFIGURACJA z opcjami), reguły widoczności ekranów, harmonogramy, ekrany/komórki dolnych belek, tabele stanów, obrazy kotła, dane demo. Jedno źródło dla WWW i Androida.
2. **WWW v0.2**: ekran „Moduł” (Wi-Fi, hasło, OTA, dziennik, restart, reset), „Konfiguracja kotła” jak w oryginale, harmonogramy (edytor 24 h), obraz kotła wg `0x370A`, ekrany zależne od konfiguracji, Statystyki, Sieć, Alarmy, Test wyjść (podgląd).
3. **Android (Kotlin + Compose)**: te same ekrany, grafiki w aplikacji, **tryb demo** z przykładowymi danymi i **konfiguracją kotła w ustawieniach** (obwody, bufor, CWU, solary...), połączenie z ESP (REST + WebSocket) jako druga opcja; budowa i testy w CI.
4. **Dopracowanie wyglądu**: porównanie ekranów WWW/Android z atlasem panelu (`ui_model/atlas`), poprawki.
