# Web Installer — TeklaPelletCANv2

## Cel

Projekt posiada webowy instalator pierwszego wgrania firmware, podobny w obsłudze do instalatorów ESPHome/WLED.

Instalator działa w przeglądarce przez **Web Serial** i korzysta z **esptool-js**. Nie wymaga osobnej aplikacji ani instalowania Pythona/esptool na komputerze.

## Co robi

1. wykrywa ESP32-C6 przez USB,
2. pobiera najnowszy obraz tekla_pellet_flash.bin z wydania installer-latest,
3. wykonuje pełne kasowanie pamięci flash,
4. zapisuje obraz **od adresu 0x000000**,
5. wykonuje reset ESP.

Obraz tekla_pellet_flash.bin jest wynikiem idf.py merge-bin i zawiera bootloader, tablicę partycji oraz firmware w jednym obrazie.

**Pełne kasowanie jest celowe.** Usuwa także NVS, konfigurację Wi-Fi, hasła, dane OTA i poprzednią konfigurację modułu. Po pierwszym wgraniu moduł powinien być traktowany jak świeży.

## Dostęp

Po uruchomieniu GitHub Pages adres będzie:

https://gieoerzet.github.io/TeklaPelletCANv2/installer/

Jeżeli repozytorium używa innego adresu publikacji, aktualny adres należy sprawdzić w Settings → Pages.

## Wymagania

- komputer z USB,
- XIAO ESP32-C6,
- aktualny Chrome lub Edge z obsługą Web Serial,
- internet podczas pobierania firmware i biblioteki esptool-js.

## Obraz firmware

Workflow .github/workflows/firmware-installer.yml buduje ESP-IDF i publikuje:

- tekla_pellet_flash.bin — obraz do pierwszego wgrania,
- manifest.json — SHA-256 i commit źródłowy.

Wydanie ma stały tag:

installer-latest

Instalator pobiera zawsze ten obraz, więc użytkownik nie musi szukać artefaktu w Actions.

## Ręczny BIN

Instalator zawiera także awaryjny wybór lokalnego pliku .bin. Każdy wskazany plik jest traktowany jako merged image i zapisywany od 0x0 po pełnym wymazaniu.

## Bezpieczeństwo

Instalator nie zna haseł, PIN-ów ani danych kotła. Nie komunikuje się z magistralą CAN. Jego jedynym zadaniem jest flashowanie modułu przez USB.

## Zmiana dokumentacji

Implementacja instalatora jest dodatkowym komponentem repozytorium. Główne instrukcje budowy niezależnej zawierają odnośnik do tego dokumentu.
