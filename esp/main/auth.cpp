#include "auth.hpp"
#include "config.hpp"
#include "tekla/b64.hpp"
#include "nvs.h"
#include "esp_random.h"
#include "esp_timer.h"
#include "mbedtls/pkcs5.h"
#include <cstring>
namespace auth {
static Session S[cfg::kMaxSessions]; static tekla::Mode g_mode = tekla::Mode::ReadOnly;
struct Fail { uint32_t ip; int n; int64_t t; }; static Fail F[8]; static int g_pin_fails; static int64_t g_pin_lock_until;
static bool hash(const char* pw, const uint8_t* salt, uint8_t* out) { return mbedtls_pkcs5_pbkdf2_hmac_ext(MBEDTLS_MD_SHA256, (const unsigned char*)pw, strlen(pw), salt, 16, cfg::kPbkdf2Iter, 32, out) == 0; }
static bool ct_eq(const uint8_t* a, const uint8_t* b, size_t n) { uint8_t d = 0; for (size_t i = 0; i < n; i++) d |= a[i] ^ b[i]; return d == 0; }
static bool store(nvs_handle_t h, const char* key, const char* secret) { uint8_t blob[48]; esp_fill_random(blob, 16); if (!hash(secret, blob, blob + 16)) return false; return nvs_set_blob(h, key, blob, 48) == ESP_OK; }
static bool check(nvs_handle_t h, const char* key, const char* secret) { uint8_t blob[48], hv[32]; size_t n = 48; if (nvs_get_blob(h, key, blob, &n) != ESP_OK || n != 48) return false; return hash(secret, blob, hv) && ct_eq(hv, blob + 16, 32); }
bool configured() { nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return false; size_t n = 0; bool ok = nvs_get_blob(h, "pw", nullptr, &n) == ESP_OK && n == 48; nvs_close(h); return ok; }
bool setup(const char* pw, const char* p1, const char* p2, const char* p3) {
  if (configured() || strlen(pw) < 8) return false; nvs_handle_t h; if (nvs_open("auth", NVS_READWRITE, &h) != ESP_OK) return false;
  bool ok = store(h, "pw", pw) && store(h, "pin1", p1) && store(h, "pin2", p2) && store(h, "pin3", p3) && nvs_commit(h) == ESP_OK; nvs_close(h); return ok;
}
int login(const char* pw, uint32_t ip, char* tok) {
  int64_t now = esp_timer_get_time(); Fail* f = nullptr; for (auto& x : F) if (x.ip == ip) f = &x; if (!f) { f = &F[0]; for (auto& x : F) if (x.t < f->t) f = &x; *f = {ip, 0, 0}; }
  if (f->n >= cfg::kLoginMaxFails && now - f->t < (int64_t)cfg::kLoginLockMs * 1000) return 2;
  nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return 1; bool ok = check(h, "pw", pw); nvs_close(h);
  if (!ok) { f->n++; f->t = now; return 1; } f->n = 0;
  Session* s = nullptr; for (auto& x : S) if (!x.used) { s = &x; break; } if (!s) { s = &S[0]; for (auto& x : S) if (x.last_us < s->last_us) s = &x; }
  uint8_t r[24]; esp_fill_random(r, sizeof r); *s = {}; s->used = true; tekla::b64url(r, sizeof r, s->token, sizeof s->token); s->last_us = now; strcpy(tok, s->token); return 0;
}
Session* find(const char* token) {
  if (!token || !*token) return nullptr; int64_t now = esp_timer_get_time();
  for (auto& s : S) if (s.used && !strcmp(s.token, token)) {
    if (now - s.last_us > (int64_t)cfg::kSessionTtlMs * 1000) { s.used = false; return nullptr; }
    if (s.level && now - s.level_us > (int64_t)cfg::kLevelAutoLockMs * 1000) s.level = 0;      // auto-blokada poziomu po 5 min bezczynnosci
    s.last_us = now; if (s.level) s.level_us = now; return &s;
  }
  return nullptr;
}
int set_level(Session* s, const char* pin) {
  int64_t now = esp_timer_get_time(); if (now < g_pin_lock_until) return 2;
  nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return 1; int lv = 0; if (check(h, "pin3", pin)) lv = 3; else if (check(h, "pin2", pin)) lv = 2; else if (check(h, "pin1", pin)) lv = 1; nvs_close(h);
  if (!lv) { if (++g_pin_fails >= 5) { g_pin_lock_until = now + 300LL * 1000000; g_pin_fails = 0; } return 1; }
  g_pin_fails = 0; s->level = lv; s->level_us = now; return 0;
}
void drop_level(Session* s) { s->level = 0; }
tekla::Mode mode() { return g_mode; }
bool set_mode(tekla::Mode m, const char* pw) { nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return false; bool ok = check(h, "pw", pw); nvs_close(h); if (ok) g_mode = m; return ok; }
}  // namespace auth
