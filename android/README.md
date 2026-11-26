# Aplikacja Android (Kotlin + Jetpack Compose)

Natywna aplikacja (bez WebView), grafiki panelu wbudowane w `app/src/main/assets/ic/` (niezależna od sieci). Ten sam model ekranów co strona WWW: `assets/app.json` i `assets/model.min.json` są **kopiami** `ui/gen/*` (CI sprawdza zgodność; kopiuj po `python3 tools/gen_app_model.py`).

## Tryby
- **Demo (domyślny)**: dane przykładowe + cykl pracy palnika; Konfiguracja kotła (obwody, CWU, bufor, solary, kaskada, magazyn) zmienia menu i ekrany jak w oryginale. PIN-y demo: 1111/2222/3333.
- **ESP**: Ustawienia aplikacji → adres + hasło urządzenia; dane przez REST (`/api/state` co 1 s), zapisy przez `PUT /api/param/{pid}` (tryb „pełna kontrola” + PIN).

## Ekrany
Pulpit, Kocioł, Palnik, Ogrzewanie 1–4, Woda, Bufor, Solary, Magazyn, Kaskada (zależnie od konfiguracji), Nastawy (listy z tabel panelu), Konfiguracja, Harmonogramy (24 h × 7 dni), Alarmy, Informacje, Statystyki, Sieć, Test wyjść (podgląd), Ustawienia aplikacji.

## Status (uczciwie)
- Kompilacja: tylko w CI (`android-build`); lokalnie brak SDK. **Nie uruchamiane na telefonie.**
- Nie ma jeszcze: Wi‑Fi/hasło/OTA modułu (są w WWW), PIN aplikacji, odcisk palca, mDNS, zapis haseł.
- Zapis harmonogramów nie był sprawdzony na prawdziwym kotle.

## Budowa
`cd android && gradle assembleDebug` (Gradle 8.9, JDK 17, Android SDK 34). APK jest artefaktem CI (`tekla-android-debug`).
