# TeklaPelletCANv2 — dokumentacja programu i kontrakt implementacyjny

**Status:** dokument bazowy implementacji Androida v2.  
**Źródło prawdy funkcjonalnej:** reverse-engineering FW2213TK + dane `data/device_model.json`, `data/menu_rows.json`, `docs/interfejs_kompletny.json` oraz kanoniczne wyniki w `analiza/analiza_archiwum/FW2213TK-2026-11-22/data/`.  
**Źródło prawdy wyglądu klienta:** uzgodniony przez właściciela styl Modern Glass i aktualny układ Androida; firmware jest źródłem zawartości, kolejności, zależności, trybów, PIN i zachowania.

## 1. Cel

Aplikacja ma być natywnym klientem Android dla TeklaPelletCANv2. Ma obsługiwać:
- tryb demo bez urządzenia,
- urządzenie ESP32-C6 przez API,
- wiele urządzeń i wykrywanie,
- pulpit dynamiczny wg konfiguracji kotła,
- ekrany Kocioł, Palnik, CO1..CO4, CWU, Bufor, Solary, Kaskada, Magazyn,
- menu oryginalnego panelu,
- nastawy i harmonogramy,
- Test wyjść,
- alarmy, informacje, statystyki, sieć i moduł ESP,
- poziomy dostępu zgodne z panelem,
- zapis tylko przez kontrakt API/model PID i odczyt zwrotny.

## 2. Zasada nadrzędna

**Nie wymyślamy zawartości urządzenia.**

Modern Glass zmienia wyłącznie prezentację. Nie zmienia:
- nazw i znaczenia pozycji,
- kolejności pozycji,
- dostępności zależnej od konfiguracji,
- poziomów PIN,
- trybów enum,
- PID-ów,
- zakresów,
- jednostek,
- ekranów i ich zależności,
- logiki Testu wyjść,
- struktury Nastaw.

Jeżeli reverse-engineering nie potwierdza elementu, oznaczamy go OPEN i nie implementujemy jako faktu.

## 3. HOME — obowiązujący układ

Pionowy ekran telefonu zachowuje ustalony układ. Nie wolno go przeprojektowywać przy implementacji funkcjonalności.

1. Górna belka bez przycisku Menu.
2. Jeden duży kafel Kocioł na górze.
   - grafika kotła,
   - temperatura aktualna,
   - zadana,
   - jasność płomienia,
   - spalanie kg/h,
   - stan pracy.
   - kliknięcie otwiera pełny ekran Kocioł.
3. Bezpośrednio pod nim dynamiczna belka wskaźników wyjść z ekranu Palnik.
   - tylko wyjścia istniejące w danej konfiguracji,
   - aktywne świecą,
   - nieaktywne są wygaszone jak diody na fizycznym panelu,
   - nie jest to nawigacja.
4. Poniżej małe, jednakowej wielkości kafle Glass.
   - generowane z konfiguracji,
   - kolejność: CO1, CO2, CO3, CO4, CWU, Paliwo, Bufor, Solary, Kaskada, Magazyn i tylko elementy faktycznie obecne,
   - jeden wspólny szablon,
   - aktywny moduł może mieć własny delikatny kolor aktywności,
   - nieaktywny pozostaje przygaszony.
5. Jeżeli kafle nie mieszczą się pionowo, następna strona HOME; nie zmniejszamy kafli.
6. Dół: kropki stron.
7. Na samym dole stała dolna nawigacja. Jest zawsze widoczna i zawsze przyklejona do dołu obszaru aplikacji, nad systemowym paskiem Androida.

### Konfiguracja HOME

- `0x2105`: liczba obwodów CO, 0..4.
- `0x2106`: CWU.
- `0x2107`: Bufor.
- `0x2108`: Solary.
- `0x350C`: Kaskada.
- `0x3525`: dodatkowy magazyn paliwa.
- pozostałe zależności wynikają z modelu i menu, nie z ręcznie zakodowanej listy.

## 4. Nawigacja dolna

Nawigacja jest dynamiczna, ale kompaktowa. Bazowa kolejność:
**Kocioł → Palnik → CO1/CO2/... → CWU → Power → Menu**.

Elementy pojawiają się tylko, gdy odpowiadają rzeczywistej konfiguracji i zasadom oryginalnego panelu. Bufor/Solary/Kaskada/Magazyn nie są automatycznie dopisywane do dolnej belki tylko dlatego, że istnieją.

Belka:
- zawsze widoczna,
- zawsze na dole,
- nie przewija się z treścią,
- nie jest zastępowana przez zawartość ekranu.

## 5. Ekrany modułów

Kliknięcie dużego Kocioł lub małego kafla otwiera odpowiadający ekran modułu.

Nie kopiujemy dużego kafla HOME do ekranu szczegółowego.

Każdy ekran ma:
- nagłówek,
- obszar głównych wartości/widgetów zgodny z reverse,
- tryb/program, jeżeli występuje,
- ustawienia/NASTAWY zgodne z oryginalną klasą,
- zachowane poziomy PIN,
- stałą dolną nawigację.

Przykładowe klasy:
- 0x00 Kocioł,
- 0x0A Palnik,
- 0x01..0x04 Ogrzewanie,
- 0x05 CWU,
- 0x06 Solary,
- 0x07 Bufor,
- 0x08 Kaskada,
- 0x09 dodatkowy magazyn,
- 0x10 Konfiguracja,
- 0x11 Statystyki.

## 6. Programy CO/CWU

Potwierdzone tryby programu:
- 0 — Wyłączony,
- 1 — Eko,
- 2 — Harmonogram,
- 3 — Komfort.

Ikony programu z modelu:
- Wyłączony: bitmap 130,
- Eko: bitmap 126,
- Harmonogram: bitmap 132,
- Komfort: bitmap 124.

Nie używać innych nazw trybów jako zamiennika.

## 7. Test wyjść — kontrakt

Test wyjść jest poziomu dostępu **2 (PIN 0999 w znanym panelowym modelu)**.

Dla zwykłych wyjść:
- 0 — Automat,
- 1 — Wyłączony,
- 2 — Włączony.

Dla rusztu i mieszaczy:
- 0 — Automat,
- 1 — Close / Zamknij,
- 2 — Open / Otwórz.

Potwierdzona lista 21 pozycji i PID:

| Pozycja | Nazwa | PID | Opcje |
|---|---|---|---|
| 1 | Pompa CWU | 0x2296 | Automat / Wyłączony / Włączony |
| 2 | Pompa kotłowa | 0x2286 | Automat / Wyłączony / Włączony |
| 3 | Pompa solarna | 0x311F | Automat / Wyłączony / Włączony |
| 4 | Podajnik | 0x2259 | Automat / Wyłączony / Włączony |
| 5 | Ruszt | 0x2279 | Automat / Close / Open |
| 6 | Wymiennik | 0x2285 | Automat / Wyłączony / Włączony |
| 7 | Zapalarka | 0x2284 | Automat / Wyłączony / Włączony |
| 8 | Podajnik palnika | 0x2295 | Automat / Wyłączony / Włączony |
| 9 | Magazyn paliwa transporter | 0x351E | Automat / Wyłączony / Włączony |
| 10 | Magazyn paliwa podajnik | 0x351F | Automat / Wyłączony / Włączony |
| 11 | Dmuchawa | 0x2283 | Automat / Wyłączony / Włączony + moc testowa |
| 12 | WentWyc | 0x2293 | Automat / Wyłączony / Włączony + moc testowa |
| 13 | Pompa CO1 | 0x229D | Automat / Wyłączony / Włączony |
| 14 | Pompa CO2 | 0x229E | Automat / Wyłączony / Włączony |
| 15 | Pompa CO3 | 0x229F | Automat / Wyłączony / Włączony |
| 16 | Pompa CO4 | 0x22A0 | Automat / Wyłączony / Włączony |
| 17 | Mieszacz CO1 | 0x214A | Automat / Close / Open |
| 18 | Mieszacz CO2 | 0x214B | Automat / Close / Open |
| 19 | Mieszacz CO3 | 0x214C | Automat / Close / Open |
| 20 | Mieszacz CO4 | 0x214D | Automat / Close / Open |
| 21 | Mieszacz powrotu | 0x216D | Automat / Close / Open |

Moc testowa:
- Dmuchawa: PID 0x2288, procent,
- WentWyc: PID 0x2298, procent.

Wizualnie pozycje nie muszą być numerowane. Numeracja powyżej jest dokumentacją identyfikacyjną, nie wymaganiem UI.

## 8. Nastawy — poziomy

Model reverse potwierdza poziomy 0/1/2. Poziom 3 w konstruktorze list jest odpowiednikiem poziomu 2.

### Kocioł
Poziom 0:
- 0x2003 zadana ręczna,
- 0x2208 histereza załączenia,
- 0x2291 histereza wyłączenia.

Poziom 1 dodaje m.in.:
- 0x2207 minimalna temperatura pompy,
- 0x3708 wybieg pompy,
- 0x3508/0x3509 godziny ciszy,
- 0x216C minimalny powrót,
- 0x216E czas otwarcia mieszacza,
- 0x3604 rezerwa paliwa,
- 0x3129/0x312A min/max ciśnienia hydraulicznego.

### CO1..CO4
Poziom 0 obejmuje m.in.:
- komfort,
- eko,
- histerezę pokojową,
- kalibrację,
- tryb,
- zadaną ręczną,
- krzywą pogodową,
- lato,
- nazwę obwodu.

Poziom 1 dodaje m.in.:
- źródło,
- delta źródła,
- typ czujnika pokojowego,
- max temperaturę,
- użycie czujnika,
- korektę,
- obniżenie,
- minimum pompy,
- czas mieszacza,
- stałą pompę,
- obniżenie energetyczne.

### CWU
Poziom 0:
- komfort,
- eko,
- Zagrzej teraz,
- histereza,
- antylegionella,
- priorytet,
- czasy cyrkulacji.

Poziom 1:
- źródło,
- delta źródła,
- delta min temperatury,
- max temperaturę.

### Solary
Poziom 0:
- przepływ,
- delta ON,
- delta OFF.

Poziom 1:
- ciepło właściwe cieczy,
- max temp. wody,
- alarm max,
- alarm min.

### Bufor
Poziom 0:
- góra zadana,
- dół zadana.

Poziom 1:
- min temperatura pompy,
- automatyczna temperatura góry,
- pozostałe pozycje modelu klasy 0x07.

### Palnik
Poziom 0:
- korekta dmuchawy,
- moc min,
- moc max,
- wartość opałowa,
- wydajność podajnika.

Poziom 1 dodaje pełny blok rozpalania/spalania/czyszczenia i regulacji.

Poziom 2 dodaje:
- bezwładność,
- ruch rusztu w modulacji,
- udział podajnika palnika,
- okres podajnika zasobnika.

### Kaskada / Magazyn
Widoczne wyłącznie przy odpowiednich flagach konfiguracji i z listami wynikającymi z modelu.

## 9. Menu

21 pozycji głównego menu ma potwierdzone bitmapy i warunki. Bazowo:
1. Kocioł
2. Palnik
3. Ogrzewanie 1
4. Ogrzewanie 2
5. Ogrzewanie 3
6. Ogrzewanie 4
7. Ciepła woda
8. Solary
9. Bufor
10. Kaskada
11. Dodatkowy magazyn paliwa
12. Historia alarmów
13. Informacje o programie
14. Konfiguracja
15. Poziom menu (PIN)
16. Język — poziom >0
17. Alarm dźwiękowy — poziom >0
18. Test wyjść — poziom >0
19. Statystyki — poziom >0
20. Sieć — poziom >0
21. Internet — poziom >0

Warunki:
- CO: 0x2105 >= numer obwodu,
- CWU: 0x2106 != 0,
- Solary: 0x2108 != 0,
- Bufor: 0x2107 != 0,
- Kaskada: 0x350C != 0,
- Magazyn: 0x3525 != 0.

## 10. Paliwo

- pojemność: PID 0x3600,
- poziom paliwa: PID 0x3601,
- rezerwa: PID 0x3603.

Wizualizacja HOME: zbiornik/wypełnienie od dołu, nie zwykły prostokątny progress bar. Progi z reverse: ok. 8/25/38/57/74/87%.

## 11. Alarmy

Aplikacja ma pokazywać alarm aktywny i historię. Dane pochodzą z modelu alarmów i API ESP. Potwierdzenie alarmu jest operacją logiczną „ack”, nie kasowaniem aktywnej przyczyny.

Historia firmware: 20 wpisów.

## 12. Harmonogram

Harmonogram:
- 7 dni,
- maska 24-bit na dzień,
- bit godziny = 1 komfort, 0 eko,
- osobne PID-y dla Kocioł, CO1..CO4 i CWU,
- zapis maski jest kontrolowany i potwierdzany.

## 13. Architektura Android

Warstwy:
1. Compose UI.
2. Model aplikacyjny.
3. DataSource:
   - DemoSource,
   - RemoteSource.
4. API ESP.
5. ESP ↔ CAN.

UI nie może omijać DataSource przy zapisie.

ESP jest pełną kontrolą dla autoryzowanego klienta; poziomy PIN są logiką klienta i wynikają z oryginalnego panelu.

## 14. Modern Glass

Stałe:
- pionowy ekran Android,
- ciemny granat/grafit,
- półprzezroczyste karty,
- cienka obwódka,
- subtelna głębia,
- Tekla orange jako akcent,
- kolor aktywności modułu może być użyty delikatnie,
- brak przesadnego neon/glow,
- brak czarnych płaskich bloków,
- ikony z zasobów panelu, gdy istnieje odpowiednik.

**Układ nie jest elementem do dowolnej reinterpretacji.** Przy kolejnych pracach zmieniamy tylko to, co wynika z konkretnego zgłoszenia.

## 15. Stała dolna belka

Każdy ekran poza stanami startowymi ma dolną nawigację nad systemowym paskiem Androida. Treść ekranu może się przewijać pod warunkiem, że belka pozostaje nieruchoma.

Dla długich ekranów (Test wyjść, Nastawy, Statystyki) przewija się środkowa zawartość, nie cała aplikacja.

## 16. Bezpieczeństwo zapisu

Przed zapisem:
1. sprawdź poziom PIN,
2. sprawdź PID w modelu,
3. sprawdź `panel_write`,
4. sprawdź zakres/encoding,
5. wykonaj zapis przez API,
6. wykonaj odczyt zwrotny,
7. pokaż błąd, jeśli weryfikacja nie przejdzie.

Nie testować zapisu na prawdziwym kotle bez świadomej zgody właściciela.

## 17. Tryb demo

Demo ma być osobnym źródłem danych. Konfiguracja demo musi pokazywać dynamiczność UI, np.:
- CO1 + CO2 + CWU,
- inne warianty konfiguracji przez istniejącą konfigurację kotła.

Demo nie może wprowadzać fikcyjnych nazw PID/trybów.

## 18. Kryterium ukończenia UI

Za gotowe uznajemy dopiero wtedy, gdy:
- HOME zachowuje uzgodniony pionowy układ,
- mały kafel nieistniejącego modułu nie jest pokazywany,
- aktywny moduł zmienia wizualny stan kafla,
- duży Kocioł otwiera szczegółowy Kocioł bez duplikowania HOME,
- moduły mają realne widgety i nastawy z reverse,
- Test wyjść ma dokładne 21 pozycji, PID-y i tryby,
- PIN-y są respektowane,
- dolna belka jest zawsze na dole,
- długie listy przewijają się wewnątrz obszaru treści,
- aplikacja buduje się,
- demo uruchamia się,
- zrzuty emulatora są sprawdzone.

## 19. Zasada utrzymania dokumentacji

Każda zmiana funkcjonalna ma aktualizować ten dokument albo dopisać aneks. Nie przenosimy ustaleń wyłącznie do rozmowy.

Jeżeli reverse-engineering zostanie rozszerzony, dokument należy aktualizować na podstawie źródła, a nie na podstawie pamięci rozmowy.
