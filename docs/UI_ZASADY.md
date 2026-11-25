# Zasady interfejsu (WWW i Android)

1. **Nic nie jest zasłaniane.** Interfejs działa wyłącznie wewnątrz bezpiecznego obszaru ekranu: pasek stanu u góry i pasek nawigacji systemu na dole (także trzyprzyciskowy, ok. 48 dp) nie zakrywają ani nagłówka, ani dolnych zakładek, ani ostatniej karty.
   - Android (Compose): `enableEdgeToEdge()` + `WindowInsets.systemBars` / `safeDrawing` jako padding całego ekranu (także `displayCutout` w poziomie).
   - WWW: `viewport-fit=cover` + `env(safe-area-inset-*)` i wysokość z `visualViewport`/`100dvh`, nie `100vh`.
2. **Pulpit w pionie mieści się bez przewijania** na ekranie ok. 412×915 dp po odjęciu pasków systemowych (sprawdzone w makiecie `ui/mockups/main_page.html`, hash `#frame`). Pozostałe ekrany mogą się przewijać, ale dolny pasek zakładek zawsze jest nad paskiem systemowym.
3. **Poziomo**: tryb pełnoekranowy z uwzględnieniem pasków systemowych i wycięcia (cutout); nawigacja zwężona do ikon.
4. Motywy: domyślny ciemny czarno-pomarańczowy, drugi jasny minimalistyczny; kolory ze zmiennych (CSS / tokeny Compose).
