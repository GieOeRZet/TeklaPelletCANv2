# TeklaPelletCANv2 - zasady pracy

1. **Projekt niezależny** od `GieOeRZet/TeklaPelletCAN` (tam: tylko dane/analiza, do odczytu). Nie zachowujemy zgodności ze starym API v1.
2. **Bezpieczeństwo urządzenia**: kocioł jest prawdziwy. Domyślny tryb ESP = tylko odczyt. Żadnych testów zapisu na prawdziwym kotle bez właściciela. Zapis tylko dla PID z `device_model.json` (kodowanie `panel_write`), po odczycie zwrotnym.
3. **Sekret w repo = zakaz**: hasła, PIN-y poziomów, klucze, tokeny, SSID/hasła Wi-Fi nigdy nie trafiają do kodu ani commitów (repo publiczne). Wartości PIN-ów ustawia właściciel przy konfiguracji ESP.
4. **Dane**: `data/device_model.json` jest wygenerowany z projektu analizy; poprawki danych robimy tam, nie ręcznie tutaj. Etykiety dowodów: MEASURED / STRONG / INFERENCE / OPEN / REJECTED.
5. **Minimalne zmiany**, bez refaktoryzacji przy okazji, weryfikacja po każdej zmianie, raport: WYKONANE / ZMODYFIKOWANE PLIKI / ZACHOWANE / WERYFIKACJA / SKUTKI UBOCZNE / DO SPRAWDZENIA PRZEZ UŻYTKOWNIKA.
6. Git: commit + push na koniec etapu (CI oszczędnie), PR-ów nie tworzyć bez polecenia. Język dokumentacji: polski.
7. Grafiki panelu (bitmapy producenta) są na odpowiedzialność właściciela; wchodzą do firmware/aplikacji w czasie kompilacji (nie pobierane za każdym razem).
