// tekla_core: format paczki UI w partycji ui_x: naglowek "TPUI" + tabela wpisow {sciezka[64], offset u32, rozmiar u32, flagi u32 (bit0 = gzip)} + dane.
// Tworzy tools/pack_ui.py. Odczyt bez kopiowania (wskaznik na pamiec partycji zmapowanej w flash).
#pragma once
#include <cstdint>
#include <cstring>
namespace tekla {
struct UiEntry { char path[64]; uint32_t off, size, flags; };
struct UiHeader { char magic[4]; uint32_t count; uint32_t total; uint32_t crc32; };
struct UiFile { const uint8_t* data; uint32_t size; bool gzip; const char* mime; };
inline const char* mime_for(const char* p) {
  const char* e = strrchr(p, '.'); if (!e) return "application/octet-stream";
  if (!strcmp(e, ".html")) return "text/html; charset=utf-8"; if (!strcmp(e, ".js")) return "text/javascript"; if (!strcmp(e, ".css")) return "text/css";
  if (!strcmp(e, ".png")) return "image/png"; if (!strcmp(e, ".json")) return "application/json"; if (!strcmp(e, ".svg")) return "image/svg+xml"; return "application/octet-stream";
}
inline bool ui_find(const uint8_t* base, uint32_t len, const char* path, UiFile* out) {
  if (len < sizeof(UiHeader)) return false; const UiHeader* h = (const UiHeader*)base; if (memcmp(h->magic, "TPUI", 4) != 0) return false;
  if (h->total > len || sizeof(UiHeader) + (uint64_t)h->count * sizeof(UiEntry) > h->total) return false;
  const UiEntry* t = (const UiEntry*)(base + sizeof(UiHeader));
  for (uint32_t i = 0; i < h->count; i++) if (strncmp(t[i].path, path, 64) == 0) { if ((uint64_t)t[i].off + t[i].size > h->total) return false; *out = {base + t[i].off, t[i].size, (t[i].flags & 1) != 0, mime_for(t[i].path)}; return true; }
  return false;
}
inline uint32_t crc32_ieee(const uint8_t* d, uint32_t n) { uint32_t c = 0xFFFFFFFFu; for (uint32_t i = 0; i < n; i++) { c ^= d[i]; for (int k = 0; k < 8; k++) c = (c >> 1) ^ (0xEDB88320u & (0u - (c & 1))); } return ~c; }
inline bool ui_verify(const uint8_t* base, uint32_t len) {          // CRC po czesci danych (za naglowkiem): wykrywa uszkodzona/niepelna partycje (healthy start)
  if (len < sizeof(UiHeader)) return false; const UiHeader* h = (const UiHeader*)base; if (memcmp(h->magic, "TPUI", 4) != 0 || h->total > len || h->total < sizeof(UiHeader)) return false;
  return crc32_ieee(base + sizeof(UiHeader), h->total - sizeof(UiHeader)) == h->crc32;
}
}  // namespace tekla
