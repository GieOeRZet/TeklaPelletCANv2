# Przejęcie projektu — 2026-11-26

## Cel dokumentu

Dokument jest punktem wejścia dla kolejnego agenta po przejęciu projektu TeklaPelletCANv2. Opisuje stan zastany na 2026-11-26, źródła prawdy, poziom pewności oraz kolejność dalszych prac. Nie zastępuje dokumentacji technicznej poszczególnych modułów.

## Stan bazowy

- Repozytorium: `GieOeRZet/TeklaPelletCANv2`
- Gałąź robocza: `ccr-be8a1bcb-5t76j0`
- Stan bazowy analizowany przy przejęciu: commit `6c87be70c1de4710c7e3e7b239174ff8e994de40`.
- Firmware źródłowy: `FW2213TK_kopia.bin`, 2 031 616 B, SHA-256 `1d679d6810cfb44a4c1d862d67552b7220f3edeb3a2807f3067d46185d0c5071`.
- Model urządzenia: 655 PID-ów; audyt UI wykazał 154 PID-y czytane przez UI i brak brakujących opisów w modelu.
- Główny kontrakt komunikacyjny ESP: API v2 opisane w `docs/API_ESP.md`.
- `data/device_model.json` jest kopią produktową modelu generowaną ze źródeł analizy; nie należy ręcznie dopisywać do niego wiedzy, która powinna trafić do `analiza/`.

## Źródła prawdy

1. **Reverse engineering / wiedza o panelu i firmware:** `analiza/` oraz jej dokumentacja.
2. **Model danych produktu:** źródła modelu w `analiza/`; `data/device_model.json` jest artefaktem produktowym.
3. **Kontrakt ESP:** `docs/API_ESP.md`.
4. **Stan produktu:** `docs/STATUS.md`.
5. **Backlog produktu:** `docs/ZADANIA.md`.
6. **Backlog reverse engineeringu:** `analiza/NewEstyma/BACKLOG.md`.
7. **Klient referencyjny WWW:** `ui/`.
8. **Klient Android:** `android/`.
9. **Integracja Home Assistant:** `ha/`.
10. **Emulator i narzędzia reverse engineeringu:** dokumentacja i narzędzia w `analiza/NewEstyma/` oraz `analiza/tools/`.

## Architektura

```
Kocioł / CAN node 4
        |
     CAN 125 kbit/s
        |
     MCP2515
        |
     ESP32-C6 / node 95
        |
   REST + WebSocket + mDNS
      /       |       \
 Android     WWW       HA
```

ESP: XIAO ESP32-C6 + MCP2515 16 MHz; SPI 1 MHz; SCK GPIO19, MISO GPIO20, MOSI GPIO18, CS GPIO17; INT obecnie niewykorzystany.

## Klasy pewności

W dalszej pracy należy rozróżniać:

- **TESTED / CONFIRMED** — potwierdzone rzeczywistym testem lub pomiarem.
- **CI PASS** — potwierdzone automatyzacją CI, bez dowodu działania na sprzęcie.
- **EMULATED** — zachowanie potwierdzone w emulatorze.
- **MOCK** — potwierdzone tylko przez serwer-atrapę.
- **INFERENCE** — wniosek z reverse engineeringu.
- **OPEN** — nierozstrzygnięte.

Nie wolno awansować statusu tylko dlatego, że kod się kompiluje.

## Stan modułów

### Reverse engineering

Zakres jest wystarczająco zamknięty, aby rozwijać produkt bez ponownego rozpoczynania pełnego RE. Zamknięto m.in. model PID, listy, alarmy, prezentery i znaczną część modelu UI. Pozostają punkty otwarte opisane w backlogu RE, m.in. wybrane pozycje widgetów, dynamika INTERNET, semantyka części ikon pogodowych/sezonowych, część PID-ów poza listami panelu oraz szczegóły czasowe.

Emulator został wcześniej rozbudowany m.in. o model wyjątków Cortex-M, SysTick/PendSV/SVC, przełączanie FreeRTOS i wstrzykiwanie przerwań CAN. Nie należy go zastępować ani budować od nowa bez konkretnej potrzeby.

### ESP

Kod rdzenia obejmuje SDO, model PID, strażnika zapisu, reguły stanów, identyfikację, JSON/live, kodowanie, walidację IP, ECDSA i generowane tabele. Native tests przechodzą, a firmware buduje się w CI.

Najważniejsza luka: **brak potwierdzenia na fizycznym sprzęcie**. Nie wykonano jeszcze rzeczywistego odczytu z kotła.

### Android

Kotlin/Compose; dashboard, ekrany i tryb demo działają w CI/emulatorze. Zrzuty 1080×2400 przechodzą.

Brakujące względem docelowego przepływu: mDNS, lista wielu urządzeń, parowanie tokenem i przejście na Bearer token jako podstawowy mechanizm klienta.

### WWW

Klient `ui/` i testy Playwright stanowią referencyjny klient produktu; obecnie pracują z serwerem-atrapą.

### Home Assistant

Fundament integracji istnieje, ale nie ma walidacji z rzeczywistym ESP i kotłem.

### Web installer

`web_installer/` jest istniejącym elementem projektu. Obraz `tekla_pellet_flash.bin` jest pełnym obrazem pierwszego flashowania; późniejsze aktualizacje mogą używać obrazu OTA.

## Najważniejsze ryzyko

Głównym ryzykiem projektu nie jest obecnie brak kolejnego dużego fragmentu reverse engineeringu. Jest nim brak dowodu, że implementacja działa z rzeczywistym łańcuchem:

**ESP32-C6 → MCP2515 → CAN → kocioł**

oraz że klienci poprawnie obsługują rzeczywiste odpowiedzi tego łańcucha.

## Zasady bezpieczeństwa testów

- Do kotła nie wykonujemy zapisu bez osobnej, świadomej zgody właściciela.
- Pierwszy test sprzętowy powinien być read-only.
- Preferowany pierwszy odczyt: PID `0x2001`.
- Przed każdym testem zapisu trzeba podać PID, wartość obecną, proponowaną, kodowanie, poziom dostępu, spodziewany efekt, sposób read-back i plan przerwania.
- Wyniki testów sprzętowych mają zostać wpisane do `docs/STATUS.md`.

## Kolejność dalszej pracy

1. Dokończyć przejęcie dokumentacyjne.
2. Zaimplementować Android: mDNS → identity → pair → token → state → WebSocket, bez naruszania trybu demo.
3. Zaktualizować ekran modułu Android do operacji serwisowych zgodnych z API v2.
4. Przeprowadzić pierwszy rzeczywisty test ESP/CAN wyłącznie odczytowy.
5. Po potwierdzeniu CAN podłączyć Android do rzeczywistego ESP.
6. Następnie domknąć Windows i Home Assistant.
7. Dopiero po stabilizacji klientów rozszerzać walidację OTA, sieci, alarmów i operacji zapisu.

## Reguły współpracy agentów

- Każda zmiana kodu, modelu, narzędzia lub dokumentacji musi być opisana w dokumentacji projektu.
- Nie należy wykonywać szerokiego ponownego skanowania reverse engineeringu bez konkretnego pytania lub luki.
- Zmiany w modelu danych wymagają regeneracji artefaktów zgodnie z `docs/ZADANIA.md`.
- Zmiana kontraktu API wymaga równoczesnej aktualizacji `docs/API_ESP.md` i serwera-atrapy.
- Każdy etap kończy się raportem: wykonane, zmodyfikowane pliki, zachowane, weryfikacja, skutki uboczne, wymagane działania użytkownika.
- Dokumentacja ma umożliwić nowemu agentowi rozpoczęcie pracy bez odtwarzania całej historii rozmów.

## Dokumenty wejściowe do przeczytania przed dalszym kodowaniem

- `docs/STATUS.md`
- `docs/ZADANIA.md`
- `docs/API_ESP.md`
- `analiza/NewEstyma/BACKLOG.md`
- `NewEstyma/METODYKA_EMULATOR.md` (jeżeli dalsza praca dotyczy emulatora/RE)
- `NewEstyma/PLAN_ZAMKNIECIA_I_PORZADKU.md` (jeżeli dalsza praca dotyczy porządkowania RE)
- `NewEstyma/ZADANIE_CHATGPT_SIEC_INFO_INTERNET.md` i `NewEstyma/ZADANIE_CHATGPT_RENDER_DIALOGI.md` tylko przy kontynuowaniu tych konkretnych prac.

## Status przejęcia

Przejęcie uznano za zakończone na poziomie analizy. Kolejny etap jest opisany poniżej w backlogu i wymaga osobnej decyzji przed rozpoczęciem zmian implementacyjnych.
