#!/usr/bin/env python3
"""Buduje JEDEN plik ze wszystkim, co potrzebne do zbudowania interfejsu (dane i zachowanie, bez wygladu):
docs/interfejs_kompletny.json. Zrodla: data/, ui/gen/app.json, analiza/.../ui_model, analiza/.../bitmaps/manifest.csv."""
import csv, json, pathlib
R = pathlib.Path(__file__).resolve().parents[1]
UI = R / "analiza/analiza_archiwum/FW2213TK-2026-11-22/data/ui_model"
BM = R / "analiza/analiza_archiwum/FW2213TK-2026-11-22/data/bitmaps/manifest.csv"
J = lambda p: json.load(open(p, encoding="utf-8"))
dm = J(R / "data/device_model.json"); app = J(R / "ui/gen/app.json")
bitmaps = [{"id": int(r["id"]), "width": int(r["width"]), "height": int(r["height"]), "alpha": r["alpha"] not in ("0", "", "False")} for r in csv.DictReader(open(BM, encoding="utf-8"))]
spec = {
 "_opis": {
  "nazwa": "Komplet danych do zbudowania interfejsu kotla Estyma igneoTouch (sterownik FW2213TK)",
  "wygenerowano": "tools/build_interface_spec.py z data/device_model.json, data/menu_rows.json, ui/gen/app.json i analiza/",
  "firmware_sha256": dm["firmware_sha256"],
  "etykiety_dowodow": {"MEASURED": "zmierzone w emulatorze/na CAN", "STRONG": "mocna analiza kodu", "INFERENCE": "wnioskowanie (ostroznie)", "OPEN": "nierozstrzygniete", "REJECTED": "obalone"},
  "sekcje": {
   "protokol": "ramki CAN/SDO, wezly, sprzet modulu",
   "reguly": "zachowanie wartosci: skala, bajt stanu, dioda palnika, ikona paliwa, mieszacz, poziomy PIN",
   "device_model": "PEŁNY model urzadzenia: pids (655: nazwa, typ, zakres, skala, tryb, panel_write), lists (listy nastaw), configuration, screens (drzewo wezlow), statuses, alarms, modules, program, outputs_test, home_indicators, real_panel_screens, presenter_widget_map, firmware_row_table, info_screen, internet_screen",
   "menu_tree": "drzewo menu panelu (wezly, krawedzie, poziomy PIN, warunki), z emulatora",
   "menu_rows_panel": "21 wierszy menu glownego: tytul, cel, poziom, warunki widocznosci (PID+operator+wartosc), numery bitmap",
   "etykiety_opcji": "teksty opcji list radio (enum_labels) i polskie etykiety list (list_labels_pl)",
   "ekrany_dane": "ktore PID-y/stany/wyjscia nalezą do ktorego ekranu (bez wygladu): wartosc glowna, wiersze, wyjscia z ramkami animacji, tryby, harmonogram",
   "konfiguracja_kotla": "wiersze konfiguracji z opcjami (ilosc obwodow, CWU, bufor, solary, kaskada, magazyn...)",
   "harmonogramy": "obwody, PID-y masek na dzien, format maski",
   "bitmapy": "indeks bitmap panelu (id, rozmiar, alfa); obrazy: analiza/.../data/bitmaps/png/<id>.png"},
  "jak_czytac_pid": "wartosc wyswietlana = surowa * format.scale; zapis tylko gdy pid.panel_write istnieje; po zapisie odczyt zwrotny; poziom PIN wiersza w lists/menu",
 },
 "protokol": {
  "magistrala": "CAN 125 kbit/s, CANopen",
  "wezly": {"kociol": 4, "modul_esp_domyslny": 95},
  "sdo_odczyt": {"id_zapytania": "0x600+wezel (kociol: 0x604)", "dane": "40 idx_lo idx_hi 01 00 00 00 00", "id_odpowiedzi": "0x580+wezel (0x584)", "odpowiedz": "4 bajty danych; stan wyjscia = mlodszy bajt"},
  "sdo_zapis": {"dane": "42 idx_lo idx_hi 01 <u32 little endian>", "uwaga": "tylko PID z panel_write; kodowanie w pids.items[pid].panel_write.encoding; zawsze odczyt zwrotny"},
  "tozsamosc_wezla": {"heartbeat_nmt_ms": 1000, "sdo": ["0x1000", "0x1008", "0x100A", "0x1018"]},
  "sprzet_modulu": {"plytka": "Seeed XIAO ESP32-C6", "kontroler_can": "MCP2515 16 MHz, SPI 1 MHz", "piny": {"SCK": 19, "MISO": 20, "MOSI": 18, "CS": 17, "INT": None}, "antena": {"przelacznik_GPIO": 3, "zewnetrzna_GPIO": 14}},
 },
 "reguly": {
  "stan_palnika": {"tabela": app["status"]["burner"], "dioda_palnika_swieci_w_stanach": [1, 2, 3, 4, 5, 6, 7, 8], "evidence": "MEASURED"},
  "stany_obwodu_co": app["status"]["ch"], "stany_solarow": app["status"]["solar"],
  "wyjscia": {"stan": "mlodszy bajt odpowiedzi SDO; 0 = wylaczone (ikona/wskaznik niewidoczny w panelu)", "mieszacz": {"1": "zamykanie", "2": "otwieranie"}, "ruszt": "tylko w ruchu; liczba = odliczanie czyszczenia"},
  "ikona_paliwa": {"kropki_wg_promili_pojemnosci": [[80, 2], [250, 5], [380, 9], [570, 14], [740, 19], [870, 24]], "litera_E": "gdy rezerwa(0x3603) > 0 i paliwo(0x3601) <= rezerwa", "pid_paliwo": "0x3601", "pid_pojemnosc": "0x3600", "pid_rezerwa": "0x3603", "evidence": "MEASURED"},
  "pora_roku": {"pid": "0x3729", "wartosc_1": "bitmapa 180", "inaczej": "bitmapa 181"},
  "obraz_kotla": {"pid": "0x370A", "wartosc_na_bitmape": app["boiler_images"], "evidence": "MEASURED"},
  "poziomy_pin": {"0": "domyslny", "1..3": "po wpisaniu PIN-u; PIN-y ustawia wlasciciel (brak w repo)", "auto_blokada_po_bezczynnosci_min_w_module_ESP": 5},
  "zapis": "tylko PID z panel_write; 42 parametry spoza list panelu sa zablokowane (firmware_row_table.rows_not_in_any_panel_list)",
 },
 "device_model": dm,
 "menu_tree": J(UI / "menu_tree.json"),
 "menu_rows_panel": J(R / "data/menu_rows.json"),
 "etykiety_opcji": {"enum_labels": J(UI / "enum_labels.json"), "list_labels_pl": J(UI / "list_labels_pl.json")},
 "ekrany_dane": app["screens"],
 "konfiguracja_kotla": app["config"],
 "harmonogramy": {**app["schedules"], "tryby_programu": app["program_modes"]},
 "bitmapy": {"indeks": bitmaps, "zbior_uzywany_przez_klientow": "ui/assets/ic/<id>.png (156 plikow)", "pelny_zbior": "analiza/analiza_archiwum/FW2213TK-2026-11-22/data/bitmaps/png/<id>.png", "ramki_pompy": app["pump_frames"]},
}
out = R / "docs/interfejs_kompletny.json"
json.dump(spec, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(out, out.stat().st_size, "B; sekcje:", ", ".join(k for k in spec if k != "_opis"))
