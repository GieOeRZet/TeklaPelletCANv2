#!/usr/bin/env python3
"""Pakuje UI do partycji ui_x (format TPUI, patrz esp/components/tekla_core/include/tekla/ui_pack.hpp): index.html, app.css, app.js, model.min.json, assets/ic/*.png.
Pliki tekstowe gzip (-9). Uzycie: python3 tools/pack_ui.py OUT.bin   (limit 448 KB = partycja 0x70000)"""
import gzip, os, struct, sys, zlib
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "ui", "ui_pack.bin")
files = [("index.html", "ui/src/index.html"), ("app.css", "ui/src/app.css"), ("app.js", "ui/src/app.js"), ("model.min.json", "ui/gen/model.min.json")]
for f in sorted(os.listdir(os.path.join(ROOT, "ui", "assets", "ic"))): files.append(("assets/ic/" + f, "ui/assets/ic/" + f))
entries = []; blob = b""; tab = len(files) * 76 + 16
for path, src in files:
    d = open(os.path.join(ROOT, src), "rb").read(); gz = 0
    if not path.endswith(".png"): d = gzip.compress(d, 9, mtime=0); gz = 1
    entries.append((path.encode()[:63].ljust(64, b"\0"), tab + len(blob), len(d), gz)); blob += d; blob += b"\0" * (-len(blob) % 4)
table = b"".join(struct.pack("<64sIII", p, o, s, g) for p, o, s, g in entries); data = table + blob; total = 16 + len(data)
pack = b"TPUI" + struct.pack("<III", len(entries), total, zlib.crc32(data) & 0xFFFFFFFF) + data
open(out, "wb").write(pack); print("UI pack: %d plikow, %d B (limit %d B) -> %s" % (len(entries), len(pack), 0x70000, out))
assert len(pack) <= 0x70000, "paczka UI przekracza partycje"
