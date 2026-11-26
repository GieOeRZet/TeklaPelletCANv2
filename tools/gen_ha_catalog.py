#!/usr/bin/env python3
"""Generuje katalog PID-ow dla integracji Home Assistant (ha/custom_components/teklapelletcanv2/catalog.json) z ui/gen/model.min.json i listy 'na zywo' z firmware.
Uzycie: python3 tools/gen_ha_catalog.py   (po zmianie modelu danych)"""
import json, re, pathlib
R = pathlib.Path(__file__).resolve().parent.parent
mm = json.load(open(R / 'ui/gen/model.min.json'))
live_src = (R / 'esp/components/tekla_core/include/tekla/live.hpp').read_text()
live = sorted({int(x, 16) for x in re.findall(r'\{(0x[0-9A-Fa-f]{4}), LK_', live_src)})
out = {}
def unit_of(label):
    m = re.search(r'\[([^\]]+)\]\s*$', label.strip()); return m.group(1) if m else None
def clean(label): return re.sub(r'\s*\[[^\]]+\]\s*$', '', label).strip()
for key, rows in mm['lists'].items():
    for r in rows:
        pid = r.get('pid')
        if not pid: continue
        pid = '0x%04X' % int(pid, 16); lab = r.get('label_pl') or ''
        if pid not in out or r.get('level_min', 0) < out[pid]['level_min']:
            out[pid] = {'pid': pid, 'name': clean(lab) or pid, 'unit': unit_of(lab), 'level_min': r.get('level_min', 0), 'live': False, 'list': key.split('|')[1] if '|' in key else key}
KNOWN = {0x2001: ('Temperatura kotła', '°C'), 0x2110: ('Temperatura CWU', '°C'), 0x2112: ('Bufor góra', '°C'), 0x2113: ('Bufor dół', '°C'), 0x215F: ('Temperatura zewnętrzna', '°C'), 0x2007: ('Stan palnika', None), 0x3601: ('Paliwo w zasobniku', 'kg')}   # tylko pewne nazwy; reszta wg tabeli panelu (EN)
for p in live:
    pid = '0x%04X' % p; e = out.setdefault(pid, {'pid': pid, 'name': (mm['pids'].get(pid) or [pid])[0], 'unit': None, 'level_min': 0, 'live': True, 'list': None}); e['live'] = True
    if p in KNOWN: e['name'], e['unit'] = KNOWN[p][0], KNOWN[p][1] or e.get('unit')
cat = sorted(out.values(), key=lambda e: e['pid'])
dst = R / 'ha/custom_components/teklapelletcanv2/catalog.json'; dst.parent.mkdir(parents=True, exist_ok=True)
json.dump({'note': 'Generowane przez tools/gen_ha_catalog.py; nazwy wg list panelu (PL).', 'pids': cat}, open(dst, 'w'), ensure_ascii=False, indent=0, separators=(',', ':'))
print(len(cat), 'PID-ow ->', dst)
