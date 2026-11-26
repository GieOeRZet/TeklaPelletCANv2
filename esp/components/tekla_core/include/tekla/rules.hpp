// tekla_core: reguly interpretacji wartosci (z analizy binarki panelu i pomiarow). Wszystko deterministyczne, bez ESP-IDF.
#pragma once
#include <cstdint>
namespace tekla {
// Stan wyjscia (pompa, podajnik, dmuchawa...) = mlodszy bajt odpowiedzi SDO (starszy bajt = 0x01). MEASURED: pompy 0x0101 = wlaczone.
inline uint8_t output_state(uint32_t sdo_value) { return (uint8_t)(sdo_value & 0xFF); }
enum MixerIcon : uint8_t { MIX_NONE = 0, MIX_CLOSE_BLUE_106 = 1, MIX_OPEN_RED_107 = 2 };   // 0x216F, 0x22B9..0x22BC
inline MixerIcon mixer_icon(uint32_t sdo_value) { uint8_t s = output_state(sdo_value); return s == 1 ? MIX_CLOSE_BLUE_106 : s == 2 ? MIX_OPEN_RED_107 : MIX_NONE; }
// Dioda palnika na HOME: widoczna dla stanow 1..8 (0 wylaczony, 9 postoj, 10 alarm = ukryta). STRONG (setter 0x0802D344).
inline bool burner_led_on(uint8_t burner_state) { return burner_state >= 1 && burner_state <= 8; }
inline const char* burner_state_name(uint8_t s) {
  static const char* n[] = {"Wylaczony", "Rozpalanie", "Rozzarzanie", "Moc 1", "Moc 2", "Modulacja", "Wygaszanie", "Czyszczenie", "Podtrzymanie", "Postoj", "Alarm"};
  return s <= 10 ? n[s] : "?";
}
inline const char* solar_state_name(uint8_t s) {
  static const char* n[] = {"Stop", "Praca", "Przeplukiwanie", "Zamarzanie", "Przegrzanie", "Woda max", "Wylaczone", "Chlodzenie", "Test", "Alarm"};
  return s <= 9 ? n[s] : "?";
}
inline const char* ch_status_name(uint8_t s) {
  static const char* n[] = {"", "Grzanie", "Wybieg", "Oczekiwanie", "Stop", "Ochrona kotla", "Alarm", "Lato", "Zrodlo blokuje"};
  return s <= 8 ? n[s] : "?";
}
// Ikona paliwa na HOME: liczba kropek zasobnika (0,2,5,9,14,19,24) wg PROCENTU napelnienia (progi ok. 8,25,38,57,74,87 %); 'E' gdy rezerwa>0 i paliwo<=rezerwa.
struct FuelIcon { int dots; bool empty_E; };
inline FuelIcon fuel_icon(int fuel_kg, int capacity_kg, int reserve_kg) {
  FuelIcon r{0, false}; if (capacity_kg <= 0) return r;
  if (reserve_kg > 0 && fuel_kg <= reserve_kg) { r.empty_E = true; return r; }
  int pm = fuel_kg * 1000 / capacity_kg;
  static const int th[] = {80, 250, 380, 570, 740, 870}; static const int dots[] = {2, 5, 9, 14, 19, 24};
  for (int i = 5; i >= 0; i--) if (pm >= th[i]) { r.dots = dots[i]; return r; }
  return r;
}
// Temperatura w dC (s16) -> stopnie C
inline double temp_c(int16_t dC) { return dC / 10.0; }
// Format czasu 'g:mm' dla 0x3711 (minuty do czyszczenia rusztu)
inline void format_hmm(uint32_t minutes, char* out, int n) { int h = minutes / 60, m = minutes % 60; int k = 0; if (n < 8) return; out[k++] = '0' + (h / 10 % 10 ? h / 10 % 10 : 0); if (h < 10) k = 0; out[k++] = '0' + h % 10; out[k++] = ':'; out[k++] = '0' + m / 10; out[k++] = '0' + m % 10; out[k] = 0; }
}  // namespace tekla
