# Home Assistant: integracja TeklaPelletCANv2 + karta TeklaPelletCAN-card

Jedna paczka (`ha/custom_components/teklapelletcanv2/`): **integracja** (config flow, wykrywanie, encje) i **karta** panelu 1:1 (ładowana automatycznie, bez ręcznego dodawania zasobu). API modułu: `docs/API_ESP.md` (wersja 2).

## Instalacja
- HACS (repozytorium niestandardowe) albo ręcznie: skopiuj `ha/custom_components/teklapelletcanv2/` do `/config/custom_components/`, zrestartuj HA.
- *Ustawienia → Urządzenia i usługi*: moduł pojawia się sam (zeroconf `_teklapellet._tcp`) → **Sparuj** (hasło urządzenia, raz; HA zapamiętuje token klienta, nie hasło) albo „Dodaj integrację → TeklaPelletCANv2” z adresem ręcznym. Zmiana adresu IP modułu aktualizuje wpis sama (po `id`).

## Konfigurator (Opcje integracji)
- **Poziom dostępu 0-3** (jak PIN-y poziomów w panelu; PIN służy tylko do sprawdzenia i **nie jest zapisywany**) - decyduje, które parametry można wybrać jako encje.
- **Wybór encji**: domyślnie powstaje tylko `binary_sensor` **Połączenie** (atrybuty: fw, api, can, tryb, id). Reszta (≈190 parametrów z `catalog.json`) tylko na Twoje życzenie, jako czujniki. Odznaczenie usuwa encję.
- Odświeżanie 2-60 s. Offline: czujnik połączenia = wyłączony, encje „niedostępne”.

## Karta
```yaml
type: custom:teklapelletcan-card
# entry_id: ...     # tylko gdy masz kilka modułów
# level: 1          # opcjonalnie; domyślnie poziom z konfiguratora
```
Ekrany panelu to obrazy wyrenderowane z oprogramowania panelu FW2213TK (`analiza/`), więc układ jest identyczny z panelem; **na żywo** są pola tekstowe powiązane z PID (temperatury, kg w zasobniku, zegar HA itd.), a ikony/wskaźniki i niepowiązane teksty pokazują stan z renderu (OPEN: pełne wskaźniki na żywo wymagają kompozycji bitmap wg `home_indicators.json`). Menu i listy nastaw to prosty widok (nie piksel w piksel); wartości list czytane z kotła na żądanie. **Karta jest tylko do odczytu** - niczego nie zapisuje do kotła. Karta nie wymaga tworzenia encji (dane z koordynatora integracji).

## Czego NIE sprawdzono
Integracja i karta **nie były uruchomione w prawdziwym Home Assistancie ani z prawdziwym modułem** (karta: test w Chromium z atrapą `hass`; Python: tylko kompilacja). Zapisy do kotła z HA nie są zaimplementowane (decyzja bezpieczeństwa; tryb zapisu jest globalny w module i wymaga hasła).

## Odtworzenie katalogu PID-ów
`python3 tools/gen_ha_catalog.py` (z `ui/gen/model.min.json` i listy „na żywo” z firmware).
