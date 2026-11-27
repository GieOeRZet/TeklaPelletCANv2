# TeklaPelletCANv2 — przewodnik implementacyjny

## Kanoniczne źródła

Przed zmianą UI sprawdzaj w tej kolejności:

1. `docs/PROGRAM_SPEC.md` — uzgodniony kontrakt aplikacji.
2. `docs/interfejs_kompletny.json` — scalony model interfejsu.
3. `data/device_model.json` — PID, typ, zakres, zapis, skala.
4. `data/menu_rows.json` — menu i warunki.
5. `analiza/analiza_archiwum/FW2213TK-2026-11-22/data/levels_0_1_2_spec.csv` — poziomy nastaw.
6. `.../data/panel_lists_by_level.md` — pełne listy Nastaw.
7. `.../data/menu_rows_21.csv` — bitmapy menu.
8. `archiwum_v1/web/js/views.js` — istniejąca implementacja referencyjna funkcji i zachowania; traktowana jako pomocniczy zapis wyników reverse, nie jako projekt nowego UI.
9. `archiwum_v1/web/teklapelletclassic-engine.js` — zachowanie i układ funkcjonalny starego klienta.
10. `analiza/analiza_archiwum/.../ui_model/` — dane z emulatora firmware.

## Klasy ekranów

| Klasa | Ekran |
|---|---|
| 0x00 | Kocioł |
| 0x01..0x04 | Ogrzewanie 1..4 |
| 0x05 | CWU |
| 0x06 | Solary |
| 0x07 | Bufor |
| 0x08 | Kaskada |
| 0x09 | Dodatkowy magazyn |
| 0x0A | Palnik |
| 0x10 | Konfiguracja |
| 0x11 | Statystyki |

## Reguła implementacji ekranu

Każdy ekran składa się z:
- nagłówka,
- widgetów głównych wynikających z modelu ekranu,
- programu/trybu, jeśli jest potwierdzony,
- aktywnych wyjść,
- wejścia do NASTAW,
- listy NASTAW generowanej z klasy + poziomu,
- stałej dolnej nawigacji.

Nie kopiować całej listy parametrów do głównego obszaru. Lista NASTAW jest osobnym wejściem/rozwijanym obszarem, zgodnie z zachowaniem panelu.

## Poziomy PIN

Poziom jest właściwością sesji UI.

- L0: podstawowe funkcje.
- L1: menu i nastawy poziomu 1.
- L2: nastawy poziomu 2 i Test wyjść.
- L3 w danych konstruktora jest równoważny L2.

Po blokadzie po czasie poziom ma zostać ponownie wymagany.

PIN-y nie są zapisywane w repo.

## Test wyjść — implementacja

Definicja musi być jedna, nie duplikowana w kilku ekranach.

Zwykły enum:
`0 Automat / 1 Wyłączony / 2 Włączony`.

Ruszt i mieszacze:
`0 Automat / 1 Close / 2 Open`.

PID-y (22 pozycji, w kolejności panelu):
`0x2296,0x374A,0x2286,0x311F,0x2259,0x2279,0x2285,0x2284,0x2295,0x351E,0x351F,0x2283,0x2293,0x229D,0x229E,0x229F,0x22A0,0x214A,0x214B,0x214C,0x214D,0x216D`.

Moc:
- 0x2288 Dmuchawa,
- 0x2298 WentWyc.

W UI nie numerować pozycji. Każdy wiersz ma ikonę, nazwę i właściwe opcje. Wybór aktywnej opcji jest wizualnie wyróżniony.

## Dolna nawigacja — implementacja

Dolny pasek jest poza kontenerem przewijanej treści.

Schemat Compose:
- root Box/Scaffold,
- content z własnym scroll state,
- bottomBar nieruchomy,
- systemBars padding.

Nie umieszczać bottom nav w `verticalScroll`.

## HOME — implementacja

HOME jest specjalnym ekranem, nie zwykłym `ScreenView`.

Komponenty:
1. `HomeHeader`
2. `BoilerHeroCard`
3. `BurnerIndicatorStrip`
4. `HomeTileGrid`
5. `HomePageDots`
6. stały `BottomNav`.

`HomeTileGrid` otrzymuje listę modułów wynikającą z konfiguracji. Nie ma stałych pustych miejsc.

## Aktywność modułu

Kolor aktywności jest stanem wizualnym, nie nowym sterowaniem.

Przykład:
- CO pracuje → kafel CO ma subtelny akcent,
- CWU pompa pracuje → CWU ma subtelny akcent,
- Bufor pompa pracuje → Bufor ma subtelny akcent,
- Solary pompa pracuje → Solary ma subtelny akcent,
- brak pracy → przygaszony Glass.

Źródłem jest rzeczywisty stan PID/wyjścia. Nie wolno wyprowadzać aktywności z samego istnienia modułu.

## Paliwo

`0x3600` = pojemność.
`0x3601` = ilość.
`0x3603` = rezerwa.

Render zbiornika:
- wypełnienie od dołu,
- progi reverse,
- stan rezerwy jako alarmowy,
- brak klasycznego poziomego progress bara.

## Nastawy

Nie hardcodować list w komponencie UI, jeżeli istnieje lista w modelu.

Mechanizm:
1. określ klasę ekranu,
2. odczytaj aktualny poziom,
3. pobierz pozycje klasy dostępne dla poziomu,
4. zastosuj warunki konfiguracji,
5. renderuj kolejność z modelu,
6. dla każdego wiersza zastosuj typ: number/switch/enum,
7. po edycji wykonaj zapis + odczyt zwrotny.

Wyjątkiem są potwierdzone elementy programu/harmonogramu, których widgety są opisane osobno w modelu.

## Harmonogram

Nie używać dowolnego kalendarza.

Panelowy model:
- 7 dni,
- 24 bity,
- 1 = komfort,
- 0 = eko,
- osobny PID maski dla dnia i obwodu.

## Demo

Demo musi przechodzić przez te same composable i ten sam model co urządzenie. Nie tworzyć osobnej wersji layoutu tylko dla demo.

## Testy

Po każdej zmianie:
- kompilacja Android,
- emulator/screenshot,
- test modelu UI,
- test DataSource,
- jeżeli zmieniono model: regeneracja `app.json` i `model.min.json`.

Przed deklaracją „gotowe” sprawdzić:
- HOME 412x915,
- Kocioł,
- Palnik,
- CO1,
- CWU,
- Bufor,
- Solary,
- Menu,
- Test wyjść,
- poziom 0/1/2,
- wariant konfiguracji z wyłączonymi modułami.

## Zakazane skróty

- wymyślone PID-y,
- wymyślone tryby,
- wymyślone pozycje Testu,
- numerowanie Testu wyjść jako element UI,
- stałe kafle nieobecnych modułów,
- duplikowanie dużego kafla HOME na ekranie Kocioł,
- przenoszenie bottom nav do scrolla,
- zastępowanie modelu listą „na oko”,
- zmiana pionowego układu HOME bez osobnej decyzji właściciela,
- dodawanie nowych przycisków do top bara bez decyzji,
- używanie zwykłych ikon systemowych, gdy istnieje potwierdzona bitmapa panelu.

## Raport po etapie

Każdy etap kończymy:
- wykonane,
- pliki,
- zachowane założenia,
- testy,
- ograniczenia,
- następny etap.

Jeżeli czegoś nie da się potwierdzić z reverse, wpisujemy OPEN zamiast zgadywać.
