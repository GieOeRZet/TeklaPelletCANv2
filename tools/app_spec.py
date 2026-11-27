"""Specyfikacja ekranow aplikacji (WWW + Android): jedno zrodlo, z ktorego tools/gen_app_model.py robi ui/gen/app.json.
Zrodla faktow: docs z analizy panelu (device_model.json -> presenter_widget_map, real_panel_screens). Ikony = numery bitmap panelu (ui/assets/ic/N.png).
Pola: t=tytul, list=klucz listy nastaw, big=[pid, jednostka], state=nazwa tablicy stanow, pill=PID wartosci zadanej, kv=[etykieta, pid, jednostka, miejsca, warunek-PID],
bar=komorki dolnej belki: k=pump|anim|mixer|grate, p=PID stanu (stan = mlodszy bajt SDO), fr=klatki, n=PID liczby nad ikona, nd=1 liczba /10 z jednym miejscem, hm=1 format g:mm."""
STATUS = {
    "burner": ["Wyłączony", "Rozpalanie", "Rozżarzanie", "Moc 1", "Moc 2", "Modulacja", "Wygaszanie", "Czyszczenie", "Podtrzymanie", "Postój", "Alarm"],
    "solar": ["Stop", "Praca", "Przepłukiwanie", "Zamarzanie", "Przegrzanie", "Woda max", "Wyłączone", "Chłodzenie", "Test", "Alarm"],
    "ch": ["", "Grzanie", "Wybieg", "Oczekiwanie", "Stop", "Ochrona kotła", "Alarm", "Lato", "Źródło blokuje"],
}
PUMP = [138, 139, 140, 141, 142, 143]
SCREENS = {
    "kociol": {"t": "Kocioł", "list": "0x00|KOCIOL|inst1", "big": ["0x2001", "°C"], "pill": "0x2004", "image": True,
               "kv": [["T. powrotu", "0x4000", "°C", 1, "0x350A"], ["Ciśnienie", "0x3333", "mbar", 0, "0x350D"], ["Energia", "0x3628", "kWh", 0]],
               "bar": [{"k": "pump", "p": "0x2277", "l": "Pompa"}, {"k": "anim", "p": "0x216A", "fr": [34, 35], "l": "Wyjście"}, {"k": "mixer", "p": "0x216F", "l": "Mieszacz"}], "sched": "kociol"},
    "palnik": {"t": "Palnik", "list": "0x0A|PALNIK|inst1", "big": ["0x2007", ""], "state": "burner",
               "kv": [["Spalanie", "0x3710", "kg/h", 1], ["Moc", "0x4005", "%", 0], ["Moc", "0x3706", "kW", 1], ["Jasność", "0x2250", "", 0], ["T. palnika", "0x226C", "°C", 1], ["Obroty", "0x299C", "", 0]],
               "bar": [{"k": "anim", "p": "0x2254", "fr": [8, 9, 10], "n": "0x2254", "l": "Dmuchawa"}, {"k": "anim", "p": "0x2251", "fr": [36, 37, 38], "n": "0x226D", "nd": 1, "l": "Podajnik zasobnika"},
                       {"k": "anim", "p": "0x2252", "fr": [40, 41, 42], "l": "Podajnik palnika"}, {"k": "anim", "p": "0x2253", "fr": [65, 66, 67], "n": "0x3711", "hm": 1, "l": "Zapalarka"}, {"k": "grate", "p": "0x2276", "l": "Ruszt"}]},
    "co1": {"t": "Ogrzewanie 1", "list": "0x01|OGRZEWANIE|inst1", "big": ["0x4001", "°C"], "pill": "0x22F5", "status": "0x22FD", "mode": "0x2064", "sched": "co1",
            "kv": [["Pokojowa", "0x2016", "°C", 1, "0x22A1"]], "bar": [{"k": "pump", "p": "0x2299", "l": "Pompa"}, {"k": "mixer", "p": "0x22B9", "l": "Mieszacz"}]},
    "co2": {"t": "Ogrzewanie 2", "list": "0x02|OGRZEWANIE|inst1", "big": ["0x4002", "°C"], "pill": "0x22F6", "status": "0x22FE", "mode": "0x2065", "sched": "co2",
            "kv": [["Pokojowa", "0x2017", "°C", 1, "0x22A2"]], "bar": [{"k": "pump", "p": "0x229A", "l": "Pompa"}, {"k": "mixer", "p": "0x22BA", "l": "Mieszacz"}]},
    "co3": {"t": "Ogrzewanie 3", "list": "0x03|OGRZEWANIE|inst1", "big": ["0x4003", "°C"], "pill": "0x22F7", "status": "0x22FF", "mode": "0x2066", "sched": "co3",
            "kv": [["Pokojowa", "0x2018", "°C", 1, "0x22A3"]], "bar": [{"k": "pump", "p": "0x229B", "l": "Pompa"}, {"k": "mixer", "p": "0x22BB", "l": "Mieszacz"}]},
    "co4": {"t": "Ogrzewanie 4", "list": "0x04|OGRZEWANIE|inst1", "big": ["0x4004", "°C"], "pill": "0x22F8", "status": "0x2300", "mode": "0x2067", "sched": "co4",
            "kv": [["Pokojowa", "0x2019", "°C", 1, "0x22A4"]], "bar": [{"k": "pump", "p": "0x229C", "l": "Pompa"}, {"k": "mixer", "p": "0x22BC", "l": "Mieszacz"}]},
    "cwu": {"t": "Ciepła woda", "list": "0x05|CWU|inst1", "big": ["0x2110", "°C"], "pill": "0x201A", "mode": "0x2014", "sched": "cwu",
            "kv": [["Potrzebna", "0x2111", "°C", 0]], "bar": [{"k": "pump", "p": "0x2278", "l": "Pompa"}, {"k": "pump", "p": "0x3749", "l": "Pompa 2"}]},
    "bufor": {"t": "Bufor", "list": "0x07|BUFOR|inst1", "big": ["0x2112", "°C"],
              "kv": [["Dół", "0x2113", "°C", 1], ["Góra zadana", "0x2280", "°C", 0], ["Dół zadana", "0x2115", "°C", 0], ["T1", "0x3171", "°C", 1], ["T2", "0x3172", "°C", 1], ["T3", "0x3173", "°C", 1], ["Energia", "0x3174", "%", 0]],
              "bar": [{"k": "pump", "p": "0x2277", "l": "Pompa"}]},
    "solary": {"t": "Solary", "list": "0x06|SOLARY|inst1", "big": ["0x2109", "°C"], "state": "solar", "stateP": "0x3120",
               "kv": [["Temp. 2", "0x2102", "°C", 1], ["Temp. 3", "0x2103", "°C", 1], ["Moc", "0x2101", "W", 0]], "bar": [{"k": "pump", "p": "0x311C", "l": "Pompa"}]},
    "magazyn": {"t": "Magazyn paliwa", "list": "0x09|MAGAZYN|inst1",
                "kv": [["Silnik 1", "0x3522", "", 0], ["Silnik 2", "0x3523", "", 0], ["Czujnik poziomu", "0x3524", "", 0], ["Czujnik rezerwy", "0x352A", "", 0]],
                "bar": [{"k": "anim", "p": "0x3522", "fr": [36, 37, 38], "l": "Silnik 1"}, {"k": "anim", "p": "0x3523", "fr": [40, 41, 42], "l": "Silnik 2"}]},
    "kaskada": {"t": "Kaskada", "list": "0x08|KASKADA|inst1",
                "kv": [["Poziom mocy", "0x3519", "", 0], ["Master", "0x3511", "", 0], ["Slave 1", "0x3512", "", 0], ["Slave 2", "0x3513", "", 0], ["Slave 3", "0x3514", "", 0]], "bar": []},
}
# tryby programu CO/CWU (ikony z tabeli panelu 0x0802DBE8 i listy "Program"): 0 wylaczony, 1 eko, 2 harmonogram, 3 komfort
PROGRAM_MODES = [{"v": 0, "label": "Wyłączony", "icon": 130}, {"v": 1, "label": "Eko", "icon": 126}, {"v": 2, "label": "Harmonogram", "icon": 132}, {"v": 3, "label": "Komfort", "icon": 124}]
DAYS = ["Pn", "Wt", "Sr", "Cz", "Pt", "Sb", "Nd"]
DAY_NAMES = ["Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela"]
# obraz kotla wg 0x370A (zmierzone w emulatorze: 1/2/3 -> bitmapa 11/12/13); 14 = szafka/bufor (karta)
BOILER_IMAGES = {"1": 11, "2": 12, "3": 13}
# demo: konfiguracja i wartosci jak w typowej instalacji (Estyma + obwody), cykl pracy palnika opisany w demo.cycle
DEMO = {
    "config": {"0x2105": 2, "0x2106": 1, "0x2107": 1, "0x2108": 1, "0x350C": 0, "0x3525": 0, "0x350A": 1, "0x350D": 1, "0x3600": 135, "0x3603": 0, "0x370A": 2},
    "values": {"0x2001": 62.0, "0x2004": 60, "0x2005": 1, "0x2002": 0, "0x2007": 0, "0x4000": 41.0, "0x3333": 1500, "0x3628": 12345, "0x215F": -3.5, "0x3721": -2.9, "0x3729": 0,
               "0x3601": 135, "0x2064": 2, "0x2065": 1, "0x2014": 2, "0x4005": 42, "0x3706": 0, "0x3710": 1.8, "0x2250": 0, "0x226C": 0, "0x299C": 0, "0x2277": 1, "0x2299": 1, "0x229A": 0, "0x2278": 1,
               "0x22B9": 0, "0x2254": 0, "0x2251": 0, "0x2252": 0, "0x2253": 0, "0x2276": 0, "0x226D": 2.1, "0x3711": 180,
               "0x4001": 44.3, "0x4002": 36.0, "0x22F5": 45, "0x22F6": 35, "0x22FD": 1, "0x22FE": 3, "0x2016": 21.5, "0x2017": 20.5, "0x22A1": 1, "0x22A2": 1, "0x2110": 51.2, "0x201A": 50, "0x2111": 50,
               "0x2112": 72.1, "0x2113": 48.5, "0x2280": 60, "0x2115": 50, "0x3171": 56, "0x3172": 48, "0x3173": 41, "0x3174": 63, "0x2109": 68.4, "0x2102": 62.0, "0x2103": 55.0, "0x2101": 0, "0x3120": 0},
    "cycle": {"period_s": 8, "burner_states": [0, 1, 2, 5, 5, 6, 7, 0], "note": "stan palnika zmienia sie co 8 s; ikony wyjsc wg stanu: wentylator 1-8, zapalarka 1, podajnik palnika 1 i 5, ruszt 7"},
}
