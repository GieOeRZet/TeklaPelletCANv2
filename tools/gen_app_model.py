#!/usr/bin/env python3
"""Wspolny model aplikacji (WWW + Android): ui/gen/app.json oraz android/app/src/main/assets/app.json.
Zrodla: data/device_model.json (konfiguracja kotla, harmonogramy, listy), data/menu_rows.json (menu z warunkami), tools/app_spec.py (ekrany, demo).
Uzycie: python3 tools/gen_app_model.py"""
import json, os, sys
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import app_spec as S
m = json.load(open(os.path.join(ROOT, "data", "device_model.json"), encoding="utf-8"))
menu = json.load(open(os.path.join(ROOT, "data", "menu_rows.json"), encoding="utf-8"))["rows"]
cfg_rows = []
for r in m["lists"]["items"]["0x10|KONFIGURACJA|inst1"]:
    if not r.get("pid") or not r.get("label_pl"): continue
    opts = r.get("options")
    if opts:
        o = [{"v": int(k), "label": v if v is not None else ("Wyłączone" if k == "0" else "Włączone")} for k, v in sorted(opts.items(), key=lambda kv: int(kv[0]))]
    elif r["kind"] == "switch":
        o = [{"v": 0, "label": "Wyłączone"}, {"v": 1, "label": "Włączone"}]
    else:
        o = None
    cfg_rows.append({"pid": r["pid"], "label": r["label_pl"].strip(), "level_min": r["level_min"], "kind": r["kind"], "min": r["min"], "max": r["max"], "default": r["default"], "options": o})
# etykiety przelacznikow z dwoma nazwami (np. glowny modul IO BIG/STD) maja juz options; reszta tak/nie
prog = m["program"]["pids_by_circuit"]
sched = [{"id": "kociol", "title": "Kocioł", "pids": prog["KOCIOL"]}] + [{"id": "co%d" % i, "title": "Ogrzewanie %d" % i, "pids": prog["OGRZEWANIE%d" % i]} for i in range(1, 5)] + [{"id": "cwu", "title": "Ciepła woda", "pids": prog["CWU"]}]
out = {
    "version": 1, "evidence": "GENEROWANE z data/device_model.json + data/menu_rows.json + tools/app_spec.py; nie edytowac recznie",
    "menu": menu, "screens": S.SCREENS, "status": S.STATUS, "config": cfg_rows,
    "schedules": {"circuits": sched, "days": S.DAYS, "day_names": S.DAY_NAMES, "default_mask": 4194240, "bits": 24, "comfort_bit": 1, "note": "maska 24-bit na dzien: bit h = godzina h:00-h+1:00; 1 = komfort, 0 = eko; zapis 42 idx_lo idx_hi 01 u32LE"},
    "program_modes": S.PROGRAM_MODES, "boiler_images": S.BOILER_IMAGES, "pump_frames": S.PUMP, "demo": S.DEMO,
}
for p in (os.path.join(ROOT, "ui", "gen", "app.json"), os.path.join(ROOT, "android", "app", "src", "main", "assets", "app.json")):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(out, open(p, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
print("app.json: menu %d, ekrany %d, konfiguracja %d, harmonogramy %d, %d B" % (len(menu), len(S.SCREENS), len(cfg_rows), len(sched), os.path.getsize(os.path.join(ROOT, "ui", "gen", "app.json"))))
