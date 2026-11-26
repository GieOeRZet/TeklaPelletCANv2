# zrodla - zamrożone dowody z analizy panelu (do odczytu)

Skopiowane ze starego repo `GieOeRZet/TeklaPelletCAN` (stan 2026-11-26), żeby v2 było samowystarczalne. **Nie edytuj ręcznie**: poprawki danych robi się w projekcie analizy, a tu trafia nowa kopia.

- `model_ekranow/` - dane z analizy (menu, listy, ekrany, widgety, alarmy, harmonogramy, ramki zapisu...). Opis: `model_ekranow/README.md`.
- `bitmapy/` - pełny zestaw bitmap panelu (PNG + `manifest.csv`). Prawa do grafik: odpowiedzialność właściciela.
- `atlas_panelu/` - zrzuty prawdziwych ekranów panelu.
- `dokumenty/` - raporty analizy (metodyka, audyt, backlog, drzewo menu, widgety, poziomy dostępu).
- `narzedzia_analizy/` - skrypty, które zbudowały model (działają tylko w układzie starego repo + z plikiem firmware FW2213TK; tu jako dokumentacja pochodzenia).

Instrukcja użycia całości: `../docs/INSTRUKCJA_BUDOWY_NIEZALEZNEJ.md`.
