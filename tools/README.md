# tools

## gen_esp_tables.py
Generuje z `data/device_model.json`: `esp/main/gen/model_pids.inc` (tablica 655 PID: typ, tryb, zakres, kodowanie zapisu, skala) oraz `ui/gen/model.min.json` (dane dla strony: PID-y, listy NASTAW, 21 par wyjsc test->stan, tabele stanow, mieszacz; ok. 93 KB, 14 KB gzip). Wyniki sa GENEROWANE - nie edytowac recznie; po kazdej zmianie `data/device_model.json` uruchomic ponownie.
