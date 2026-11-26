# tools

- `gen_esp_tables.py` - z `data/device_model.json` generuje `esp/components/tekla_core/gen/*.inc` (tablice PID/poziomów/list dla firmware) oraz `ui/gen/model.min.json`. Wyniki GENEROWANE - nie edytować; po zmianie `data/device_model.json` uruchomić ponownie.
- `gen_app_model.py` (+ `app_spec.py`) - z modelu, `data/menu_rows.json` i opisu ekranów generuje `ui/gen/app.json` i kopię `android/app/src/main/assets/app.json`.
- `build_interface_spec.py` - składa `docs/interfejs_kompletny.json` (komplet danych do zbudowania interfejsu).
- `android_shots.sh` - zrzuty ekranów aplikacji w emulatorze (używa CI).
- `mock/` - serwer-atrapa API modułu + test e2e klienta WWW (patrz `mock/README.md`: po co i czego NIE sprawdza).
- Narzędzia analizy panelu (emulacja, dekompilacja, budowa modelu): `../analiza/tools/` (opis: `../analiza/README.md`).
