// tekla_core: podpis ECDSA P-256 w postaci surowej r||s (64 B) -> DER (dla mbedtls_pk_verify). Podpis OTA (docs/ESP_API_V2.md 13.2, wariant ECDSA zamiast Ed25519: mbedtls w ESP-IDF).
#pragma once
#include <cstddef>
#include <cstdint>
#include <cstring>
namespace tekla {
inline size_t sig_rs_to_der(const uint8_t rs[64], uint8_t* out, size_t cap) {
  uint8_t tmp[2][33]; size_t len[2];
  for (int i = 0; i < 2; i++) { const uint8_t* p = rs + 32 * i; size_t skip = 0; while (skip < 31 && p[skip] == 0) skip++; size_t n = 32 - skip; size_t k = 0; if (p[skip] & 0x80) tmp[i][k++] = 0; memcpy(tmp[i] + k, p + skip, n); len[i] = n + k; }
  size_t total = 2 + len[0] + 2 + len[1]; if (total + 2 > cap) return 0;
  size_t o = 0; out[o++] = 0x30; out[o++] = (uint8_t)total; for (int i = 0; i < 2; i++) { out[o++] = 0x02; out[o++] = (uint8_t)len[i]; memcpy(out + o, tmp[i], len[i]); o += len[i]; } return o;
}
}  // namespace tekla
