// tekla_core: base64url (tokeny sesji; bez dopelnienia '=').
#pragma once
#include <cstddef>
#include <cstdint>
namespace tekla {
inline size_t b64url(const uint8_t* in, size_t n, char* out, size_t cap) {
  static const char* A = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"; size_t k = 0;
  for (size_t i = 0; i < n; i += 3) {
    uint32_t v = in[i] << 16 | (i + 1 < n ? in[i + 1] << 8 : 0) | (i + 2 < n ? in[i + 2] : 0); int chars = (int)(n - i >= 3 ? 4 : (n - i) + 1);
    for (int c = 0; c < chars; c++) { if (k + 2 > cap) return 0; out[k++] = A[(v >> (18 - 6 * c)) & 63]; }
  }
  if (k + 1 > cap) return 0; out[k] = 0; return k;
}
}  // namespace tekla
