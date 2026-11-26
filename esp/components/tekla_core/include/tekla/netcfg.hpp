// tekla_core: walidacja ustawien sieci (staly IP) i identyfikator urzadzenia z adresu MAC. Czyste funkcje, testowane na hoscie.
#pragma once
#include <cstdint>
#include <cstdio>
#include <cstddef>
namespace tekla {
// "a.b.c.d" -> liczba (a w najstarszym bajcie); false gdy zle
inline bool parse_ipv4(const char* s, uint32_t* out) {
  unsigned a, b, c, d; char x; if (!s || sscanf(s, "%u.%u.%u.%u%c", &a, &b, &c, &d, &x) != 4) return false;
  if (a > 255 || b > 255 || c > 255 || d > 255) return false; *out = (a << 24) | (b << 16) | (c << 8) | d; return true;
}
inline void format_ipv4(uint32_t v, char* out, size_t cap) { snprintf(out, cap, "%u.%u.%u.%u", (v >> 24) & 255, (v >> 16) & 255, (v >> 8) & 255, v & 255); }
inline bool mask_ok(uint32_t m) { if (!m || m > 0xFFFFFFFCu) return false; uint32_t inv = ~m; return (inv & (inv + 1)) == 0; }   // ciagle jedynki, prefiks 1..30
// Staly adres: maska poprawna, IP i brama w jednej podsieci, ani adres sieci/rozgloszeniowy, nie loopback/multicast, brama != IP.
inline bool static_ip_ok(uint32_t ip, uint32_t mask, uint32_t gw) {
  if (!mask_ok(mask)) return false; uint32_t net = ip & mask, bc = net | ~mask;
  auto host = [&](uint32_t a) { return a != 0 && (a >> 24) != 127 && (a >> 24) < 224 && (a & mask) == net && a != net && a != bc; };
  return host(ip) && host(gw) && gw != ip;
}
// Identyfikator urzadzenia z MAC: "tp-aabbccddeeff" (staly, unikalny dla ukladu) oraz nazwa hosta "teklapellet-ddeeff".
inline void device_id(const uint8_t mac[6], char* out, size_t cap) { snprintf(out, cap, "tp-%02x%02x%02x%02x%02x%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]); }
inline void device_host(const uint8_t mac[6], char* out, size_t cap) { snprintf(out, cap, "teklapellet-%02x%02x%02x", mac[3], mac[4], mac[5]); }
// Nazwa urzadzenia nadawana przez uzytkownika: 1-23 znaki, bez cudzyslowu, ukosnika odwrotnego i znakow sterujacych (trafia do JSON i mDNS).
inline bool name_ok(const char* s) { if (!s) return false; size_t n = 0; for (; s[n]; n++) { unsigned char c = (unsigned char)s[n]; if (c < 32 || c == '"' || c == '\\' || c == 127) return false; } return n >= 1 && n <= 23; }
}  // namespace tekla
