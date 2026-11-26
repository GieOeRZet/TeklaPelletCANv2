# TeklaPelletCANv2 - zasady pracy

1. **Projekt samodzielny (monorepo)**: nie korzystamy z żadnego innego repozytorium. Cała analiza panelu, emulacja, dekompilacja, narzędzia, bitmapy i plik firmware sterownika są w `analiza/`; stary produkt v1 i dane sprzętowe PelletCAN w `archiwum_v1/` (tylko do odczytu, bez zgodności ze starym API v1).
2. **Bezpieczeństwo urządzenia**: kocioł jest prawdziwy. Domyślny tryb ESP = tylko odczyt. Żadnych testów zapisu na prawdziwym kotle bez właściciela. Zapis tylko dla PID z `device_model.json` (kodowanie `panel_write`), po odczycie zwrotnym.
3. **Sekret w repo = zakaz**: hasła, PIN-y poziomów, klucze, tokeny, SSID/hasła Wi-Fi nigdy nie trafiają do kodu ani commitów (repo publiczne). Wartości PIN-ów ustawia właściciel przy konfiguracji ESP.
4. **Dane**: `data/device_model.json` to kopia produktowa modelu wygenerowanego w `analiza/` (`analiza/analiza_archiwum/FW2213TK-2026-11-22/data/ui_model/device_model.json`); poprawki danych robimy w `analiza/` (narzędzia `analiza/tools/`), potem kopiujemy i regenerujemy (CI sprawdza zgodność kopii). Etykiety dowodów: MEASURED / STRONG / INFERENCE / OPEN / REJECTED.
5. **Minimalne zmiany**, bez refaktoryzacji przy okazji, weryfikacja po każdej zmianie, raport: WYKONANE / ZMODYFIKOWANE PLIKI / ZACHOWANE / WERYFIKACJA / SKUTKI UBOCZNE / DO SPRAWDZENIA PRZEZ UŻYTKOWNIKA.
6. Git: commit + push na koniec etapu (CI oszczędnie), PR-ów nie tworzyć bez polecenia. Język dokumentacji: polski.
7. Grafiki panelu (bitmapy producenta) i plik firmware sterownika `analiza/FW2213TK_kopia.bin` są na odpowiedzialność właściciela; wchodzą do firmware/aplikacji w czasie kompilacji (nie pobierane za każdym razem).
8. **ESP nie serwuje UI**: moduł to most CAN↔API + mała strona serwisowa; interfejs użytkownika robią klienci (Android, Windows, HA). Kontrakt API: `docs/API_ESP.md` (zmiana kontraktu = aktualizacja dokumentu i serwera-atrapy `tools/mock/`).
