# Test sprzętowy ESP32-C6 + Android — 2026-11-26

## Cel

Pierwszy rzeczywisty test po przejęciu projektu przez ChatGPT. Zakres ma zweryfikować najpierw sam moduł ESP i provisioning Androida, a dopiero potem komunikację CAN z kotłem.

## 1. Pierwsze flashowanie

Użyć webowego instalatora:

- web_installer/index.html
- obraz: tekla_pellet_flash.bin
- adres: 0x000000
- pełne kasowanie flash

Instalator przed flashowaniem pobiera manifest.json i sprawdza projekt, układ ESP32-C6, adres, tryb kasowania, rozmiar oraz SHA-256 obrazu.

Po flashowaniu nie zakładać, że urządzenie jest skonfigurowane. NVS jest wyczyszczone.

## 2. Pierwsza konfiguracja BLE

Na Androidzie:

1. Włącz Bluetooth.
2. Otwórz Ustawienia aplikacji → Nowy moduł (Bluetooth).
3. Wyszukaj TeklaPellet-xxxxxx.
4. Połącz się z urządzeniem i odczytaj INFO.
5. Dla nowego modułu podaj SSID, hasło Wi-Fi, hasło urządzenia i PIN-y poziomów 1–3.
6. Zapisz wygenerowany kod recovery.
7. Wyślij provisioning przez szyfrowane BLE.
8. Moduł powinien zapisać konfigurację, uruchomić Wi-Fi i zakończyć BLE po stabilnym połączeniu.

Jeżeli BLE nie działa, alternatywą jest AP TeklaPellet-xxxxxx / 192.168.4.1.

## 3. Automatyczne wykrycie i pairing

Po provisioning Android powinien:
1. wykryć urządzenie przez mDNS _teklapellet._tcp,
2. znaleźć je po stałym id,
3. pobrać /api/identity,
4. wykonać /api/pair,
5. zapisać stały token,
6. przełączyć aplikację na normalne REST/WebSocket.

## 4. Pierwszy test bez kotła

Nie podłączać jeszcze CAN do kotła.

Sprawdzić restart ESP, ponowne połączenie Wi-Fi, zachowanie konfiguracji po zaniku zasilania, ponowne uruchomienie Androida, token po ponownym uruchomieniu aplikacji oraz mDNS po restarcie.

## 5. Test CAN — tylko odczyt

Dopiero po poprawnym przejściu provisioning:
- podłączyć MCP2515 do magistrali CAN,
- 125 kbit/s,
- ESP node 95,
- kocioł node 4,
- nie wykonywać żadnego zapisu do kotła.

Pierwszy odczyt referencyjny: PID 0x2001. Porównać wartość z panelem kotła.

## 6. OTA

OTA nie jest pierwszym krokiem testu.

Najpierw musi działać stabilny firmware bazowy i provisioning. Następnie sprawdzić OTA do nowszego obrazu, zachowanie NVS, zachowanie Wi-Fi/hasła/PIN/tokenów, poprawne uruchomienie nowego slotu oraz rollback tylko na kontrolowanym teście.

## 7. Czego nie uznawać za potwierdzone

Do czasu fizycznego testu:
- BLE = CI/implementacja, nie HARDWARE PASS,
- mDNS = CI/implementacja, nie HARDWARE PASS,
- OTA rollback = CI/implementacja, nie HARDWARE PASS,
- CAN = niezweryfikowane,
- zapis parametrów kotła = nie wykonywać.

## Kryterium sukcesu pierwszego testu

Za sukces pierwszego etapu uznajemy:

flash USB → BLE discovery → BLE provisioning → Wi-Fi → mDNS → identity → pair → token → restart → ponowne połączenie Androida

bez wykonywania zapisu do kotła.
