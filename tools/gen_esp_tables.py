#!/usr/bin/env python3
"""Generator tablic dla ESP z data/device_model.json (zrodlo prawdy; NIE edytowac wynikow recznie).
Wyjscie: esp/components/tekla_core/gen/model_pids.inc, model_levels.inc (tablica PID: typ, tryb, zakres, kodowanie zapisu) oraz ui/gen/model.min.json (dane dla strony: PID-y, listy NASTAW, stany wyjsc).
Uzycie: python3 tools/gen_esp_tables.py   (z katalogu repo)"""
import json, os, re
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
m = json.load(open(os.path.join(ROOT, "data", "device_model.json"), encoding="utf-8"))
TY = {"u8": "TT_U8", "u16": "TT_U16", "s16": "TT_S16", "u32": "TT_U32", "u32?": "TT_U32"}
MO = {"setting": "PM_SETTING", "live": "PM_LIVE", "command": "PM_COMMAND", "mirror": "PM_MIRROR"}
def enc(pw):
    if not pw: return "WE_NONE", 1.0
    e = pw["encoding"]
    if e == "plain": return "WE_PLAIN", 1.0
    if e.startswith("raw = wyswietlana"): return "WE_SCALED", float(re.search(r"scale=([0-9.]+)", e).group(1))
    if e.startswith("indeks opcji"): return "WE_OPTION_INDEX", 1.0
    if e.startswith("przelacznik"): return "WE_SWITCH", 1.0
    return "WE_OTHER", 1.0
items = m["pids"]["items"]
BLOCK = {h["pid"].lower() for h in (m.get("firmware_row_table") or {}).get("rows_not_in_any_panel_list", []) if h.get("pid")}   # 42 parametry poza listami panelu: zapis ZABLOKOWANY (decyzja wlasciciela 2026-11-26)
lines = ["// GENEROWANE przez tools/gen_esp_tables.py z data/device_model.json - nie edytowac recznie",
         "// {pid, typ, tryb, min, max, kodowanie zapisu, skala}",
         "// WE_BLOCKED = zapis zabroniony (parametry poza listami panelu)",
         "// Odczyt stanu wyjsc: stan = (wartosc SDO & 0xFF) (patrz docs/ESP_API_V2.md 13.4)"]
rows = []
for p in sorted(items, key=lambda x: int(x, 16)):
    v = items[p]; e, sc = enc(v.get("panel_write"))
    if p.lower() in BLOCK: e = "WE_BLOCKED"
    rows.append("  {0x%04X, %s, %s, %dLL, %dLL, %s, %sf}, /* %s */" % (int(p, 16), TY.get(v["type"], "TT_U32"), MO.get(v["mode"], "PM_LIVE"), int(v["min"]), int(v["max"]), e, repr(sc), v["name_en"].replace("*/", "* /")))
GEN = os.path.join(ROOT, "esp", "components", "tekla_core", "gen")
os.makedirs(GEN, exist_ok=True)
open(os.path.join(GEN, "model_pids.inc"), "w", encoding="utf-8").write("\n".join(lines + rows) + "\n")
# poziomy PIN (level_min) per PID z list NASTAW
lv = {}
for k, rowsl in (m["lists"]["items"].items() if isinstance(m["lists"], dict) else []):
    for r in rowsl:
        if r.get("pid") and r.get("level_min") is not None: lv[int(r["pid"], 16)] = min(lv.get(int(r["pid"], 16), 9), int(r["level_min"]))
open(os.path.join(GEN, "model_levels.inc"), "w", encoding="utf-8").write("// GENEROWANE: {pid, level_min} z list NASTAW; PID bez wpisu = poziom 3\n" + "\n".join("  {0x%04X, %d}," % (p_, l) for p_, l in sorted(lv.items())) + "\n")
# ---- dane dla strony
rs = m.get("real_panel_screens", {})
bb = rs.get("bottom_bar_static_2026_11_25", {})
ui = {"firmware_sha256": m.get("firmware_sha256"),
      "pids": {p: [v["name_en"], v["type"], v["mode"], v["min"], v["max"]] for p, v in items.items()},
      "lists": m["lists"]["items"] if isinstance(m["lists"], dict) and "items" in m["lists"] else m["lists"],
      "outputs": bb.get("pary_test_stan", {}).get("pary", []),
      "status_tables": rs.get("status_tables_from_binary_2026_11_25", {}),
      "mixer": bb.get("KOCIOL", {}).get("cells", [])}
os.makedirs(os.path.join(ROOT, "ui", "gen"), exist_ok=True)
json.dump(ui, open(os.path.join(ROOT, "ui", "gen", "model.min.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
print("PID:", len(rows), "model.min.json:", os.path.getsize(os.path.join(ROOT, "ui", "gen", "model.min.json")), "B")
