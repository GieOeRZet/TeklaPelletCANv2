# analiza/ - kompletne środowisko analizy panelu Estyma (FW2213TK)

Samodzielna kopia projektu analizy (dawniej osobne repo `GieOeRZet/TeklaPelletCAN`). **Układ katalogów jest taki jak w starym repo, więc wszystkie narzędzia działają bez zmian ścieżek** - uruchamiaj je z tego katalogu (`ROOT` = `analiza/`, patrz `tools/paths.py`).

| Ścieżka | Zawartość |
|---|---|
| `FW2213TK_kopia.bin` | plik firmware sterownika (kopia właściciela, 2 MB, SHA‑256 `1d679d68…c5071`) - wejście dla emulacji i dekompilacji. **Własność producenta; odpowiedzialność za jego użycie/udostępnianie ma właściciel.** |
| `firmware/FW2213TK-ghidra/` | wynik dekompilacji Ghidry: `decompiled.c`, `functions.txt`, `strings.txt` (adres w decompiled.c = adres runtime − 0x10000) |
| `tools/` | ~230 narzędzi: emulacja (`emu_*`, `ui_session`, `cortexm_rtos`), skany statyczne, budowa modelu (`build_device_model.py`, `build_program_pids.py`, `build_write_frames.py`), audyty (`audit_bin_vs_model.py`, `validate_ui_model.py`), `paths.py` |
| `software/re_harness/`, `software/model/` | harness replay/ślady/watchpointy oraz silnik parametrów |
| `analiza_archiwum/FW2213TK-2026-11-22/` | **główny przebieg analizy**: `data/` (CSV/JSON dowody, `data/ui_model/` = modele ekranów, `data/bitmaps/` = bitmapy panelu, `data/atlas`), raporty z kolejnych przejść (`AUTONOMOUS-PASS-*.md`), `screens/` |
| `analiza_archiwum/FW2213TK-2026-11-18…24/` | wcześniejsze przebiegi |
| `NewEstyma/` | dokumentacja procesu: `00_AGENT_START_HERE.md`, `02_WORKFLOW`, `03_TOOLING`, `METODYKA_EMULATOR`, `05_PLAN_1TO1`, `RAPORT_POSTEPU`, `BACKLOG`, audyty |
| `rejestry/` | rejestry CAN/panelu, przewodnik Ghidry, katalogi parametrów i alarmów |
| `ui_model/`, `eksport_grafik/`, `prototyp/` | starsze modele, eksporty grafik, prototypy (historyczne) |
| `docs/`, `DOKUMENTACJA/` | dokumenty starego projektu (architektura, pomiary) |
| `README_stary_projekt.md`, `CLAUDE_stary_projekt.md` | historyczne: opis i zasady starego repo (nie obowiązują w v2; obowiązuje `../CLAUDE.md`) |

## Uruchamianie
```
cd analiza
pip install -r requirements.txt
python3 tools/validate_ui_model.py          # walidacja modelu ekranow (powinno: PASS)
python3 tools/audit_bin_vs_model.py         # audyt: binarka vs model
```
Zacznij od `NewEstyma/00_AGENT_START_HERE.md`, potem `NewEstyma/METODYKA_EMULATOR.md` i `NewEstyma/03_TOOLING.md`. Etykiety dowodów: MEASURED / STRONG / INFERENCE / OPEN / REJECTED.

## Zależność od reszty v2
Produktowa kopia modelu to `../data/device_model.json`. Źródłem jest `analiza_archiwum/FW2213TK-2026-11-22/data/ui_model/device_model.json` (generowany przez `tools/build_device_model.py`). Po każdej zmianie modelu: wygeneruj w `analiza/`, skopiuj do `../data/`, uruchom `python3 ../tools/gen_esp_tables.py` i `python3 ../tools/gen_app_model.py`. CI sprawdza, że obie kopie są identyczne.
