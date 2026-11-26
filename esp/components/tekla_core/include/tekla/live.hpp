// tekla_core: zestawy danych "na zywo" i konwersja surowej odpowiedzi SDO -> wartosc wyswietlana (konwencja /ws: jednostki wyswietlane, wyjscia jako stan).
#pragma once
#include <cstdint>
#include <cstddef>
#include <cstdio>
#include <cstring>
namespace tekla {
enum LiveKind : uint8_t { LK_PLAIN, LK_DC /*s16 dC -> C*/, LK_STATE /*mlodszy bajt*/, LK_D10 /*u/s16 /10*/, LK_U16, LK_S16 };
struct LiveDef { uint16_t pid; LiveKind kind; uint16_t period_ms; uint8_t sets; };   // sets: bit0 = zestaw podstawowy (zawsze), bit1.. = ekran (kolejne bity wg LiveScreen)
enum LiveScreen : uint8_t { LS_BASE = 1, LS_KOCIOL = 2, LS_PALNIK = 4, LS_CO = 8, LS_CWU = 16, LS_BUFOR = 32, LS_SOLAR = 64, LS_MAGAZYN = 128 };
// Zrodlo: docs/ESP_API_V2.md 13.4 i data/device_model.json -> presenter_widget_map. Temperatury 1 Hz (1000 ms), stany wyjsc 1-2 s.
static const LiveDef kLive[] = {
  {0x2001, LK_DC, 1000, LS_BASE}, {0x2004, LK_PLAIN, 2000, LS_BASE}, {0x2005, LK_PLAIN, 2000, LS_BASE}, {0x2002, LK_PLAIN, 2000, LS_BASE}, {0x2007, LK_PLAIN, 1000, LS_BASE},
  {0x4000, LK_DC, 1000, LS_BASE | LS_KOCIOL}, {0x350A, LK_PLAIN, 30000, LS_KOCIOL}, {0x350D, LK_PLAIN, 30000, LS_KOCIOL}, {0x3333, LK_U16, 2000, LS_KOCIOL}, {0x3628, LK_PLAIN, 10000, LS_KOCIOL},
  {0x215F, LK_DC, 5000, LS_BASE}, {0x3721, LK_DC, 5000, LS_BASE}, {0x3729, LK_PLAIN, 60000, LS_BASE},
  {0x3600, LK_PLAIN, 30000, LS_BASE}, {0x3601, LK_PLAIN, 5000, LS_BASE}, {0x3603, LK_PLAIN, 30000, LS_BASE},
  {0x2064, LK_PLAIN, 5000, LS_BASE}, {0x2065, LK_PLAIN, 5000, LS_BASE}, {0x2014, LK_PLAIN, 5000, LS_BASE},
  {0x4005, LK_PLAIN, 1000, LS_BASE | LS_PALNIK}, {0x3706, LK_D10, 1000, LS_PALNIK}, {0x3710, LK_D10, 1000, LS_PALNIK}, {0x2250, LK_PLAIN, 1000, LS_PALNIK}, {0x226C, LK_DC, 1000, LS_PALNIK}, {0x299C, LK_PLAIN, 1000, LS_PALNIK},
  {0x2277, LK_STATE, 1000, LS_BASE | LS_KOCIOL | LS_BUFOR}, {0x2299, LK_STATE, 1000, LS_BASE | LS_CO}, {0x2278, LK_STATE, 1000, LS_BASE | LS_CWU}, {0x3749, LK_STATE, 1000, LS_CWU},
  {0x216F, LK_STATE, 1000, LS_KOCIOL}, {0x216A, LK_STATE, 1000, LS_KOCIOL}, {0x22B9, LK_STATE, 1000, LS_CO},
  {0x2254, LK_STATE, 1000, LS_PALNIK}, {0x2251, LK_STATE, 1000, LS_PALNIK}, {0x2252, LK_STATE, 1000, LS_PALNIK}, {0x2253, LK_STATE, 1000, LS_PALNIK}, {0x2276, LK_STATE, 1000, LS_PALNIK},
  {0x226D, LK_D10, 2000, LS_PALNIK}, {0x3711, LK_PLAIN, 10000, LS_PALNIK},
  {0x2110, LK_DC, 1000, LS_CWU}, {0x201A, LK_PLAIN, 2000, LS_CWU}, {0x2111, LK_PLAIN, 5000, LS_CWU},
  {0x2112, LK_DC, 1000, LS_BUFOR}, {0x2113, LK_DC, 1000, LS_BUFOR},
  {0x2109, LK_DC, 1000, LS_SOLAR}, {0x311C, LK_STATE, 1000, LS_SOLAR}, {0x3120, LK_PLAIN, 1000, LS_SOLAR},
  {0x3522, LK_STATE, 1000, LS_MAGAZYN}, {0x3523, LK_STATE, 1000, LS_MAGAZYN}, {0x3524, LK_STATE, 1000, LS_MAGAZYN},
};
static const size_t kLiveCount = sizeof(kLive) / sizeof(kLive[0]);
inline double live_value(LiveKind k, uint32_t raw) {
  switch (k) {
    case LK_DC: return (int16_t)(raw & 0xFFFF) / 10.0;
    case LK_D10: return (int16_t)(raw & 0xFFFF) / 10.0;
    case LK_STATE: return (double)(raw & 0xFF);
    case LK_U16: return (double)(raw & 0xFFFF);
    case LK_S16: return (double)(int16_t)(raw & 0xFFFF);
    default: return (double)(raw & 0xFF) == 0 && raw > 255 ? (double)(raw & 0xFFFF) : (double)(raw & 0xFFFF);
  }
}
// JSON {"t":"live","v":{"0x2001":62.1,...}} do bufora; zwraca dlugosc (0 = brak miejsca). Bez alokacji.
struct LiveItem { uint16_t pid; double v; };
inline size_t json_live(const LiveItem* it, size_t n, char* out, size_t cap) {
  size_t k = 0; auto put = [&](const char* s) { size_t l = strlen(s); if (k + l + 1 > cap) return false; memcpy(out + k, s, l); k += l; return true; };
  if (!put("{\"t\":\"live\",\"v\":{")) return 0;
  for (size_t i = 0; i < n; i++) { char b[48]; snprintf(b, sizeof b, "%s\"0x%04X\":%.1f", i ? "," : "", it[i].pid, it[i].v); if (!put(b)) return 0; }
  if (!put("}}")) return 0; out[k] = 0; return k;
}
}  // namespace tekla
