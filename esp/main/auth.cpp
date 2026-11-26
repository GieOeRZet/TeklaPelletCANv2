#include "auth.hpp"
#include "config.hpp"
#include "tekla/b64.hpp"
#include "nvs.h"
#include "esp_random.h"
#include "esp_timer.h"
#include "mbedtls/pkcs5.h"
#include "mbedtls/sha256.h"
#include "tekla/netcfg.hpp"
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
static int verify_rl(const char* key, const char* pw, uint32_t ip) {       // sprawdzenie hasla z limitem prob na IP (wspolny licznik dla wszystkich hasel)
  int64_t now = esp_timer_get_time(); Fail* f = nullptr; for (auto& x : F) if (x.ip == ip) f = &x; if (!f) { f = &F[0]; for (auto& x : F) if (x.t < f->t) f = &x; *f = {ip, 0, 0}; }
  if (f->n >= cfg::kLoginMaxFails && now - f->t < (int64_t)cfg::kLoginLockMs * 1000) return 2;
  nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return 1; bool ok = check(h, key, pw); nvs_close(h);
  if (!ok) { f->n++; f->t = now; return 1; } f->n = 0; return 0;
}
int verify_limited(const char* pw, uint32_t key) { return verify_rl("pw", pw, key); }
int login(const char* pw, uint32_t ip, char* tok) {
  int rc = verify_rl("pw", pw, ip); if (rc) return rc; int64_t now = esp_timer_get_time();
  Session* s = nullptr; for (auto& x : S) if (!x.used) { s = &x; break; } if (!s) { s = &S[0]; for (auto& x : S) if (x.last_us < s->last_us) s = &x; }
  uint8_t r[24]; esp_fill_random(r, sizeof r); *s = {}; s->used = true; tekla::b64url(r, sizeof r, s->token, sizeof s->token); s->last_us = now; strcpy(tok, s->token); return 0;
}
struct Client { uint8_t hash[32]; char name[24]; };
static Client CL[8]; static int g_ncl = -1;
static void cl_load() { if (g_ncl >= 0) return; g_ncl = 0; nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) == ESP_OK) { size_t n = sizeof CL; if (nvs_get_blob(h, "cl", CL, &n) == ESP_OK) g_ncl = (int)(n / sizeof(Client)); nvs_close(h); } if (g_ncl > 8) g_ncl = 0; }
static bool cl_save() { nvs_handle_t h; if (nvs_open("auth", NVS_READWRITE, &h) != ESP_OK) return false; bool ok = nvs_set_blob(h, "cl", CL, g_ncl * sizeof(Client)) == ESP_OK && nvs_commit(h) == ESP_OK; nvs_close(h); return ok; }
static void tok_hash(const char* t, uint8_t out[32]) { mbedtls_sha256((const unsigned char*)t, strlen(t), out, 0); }
static Session* slot() { Session* s = nullptr; for (auto& x : S) if (!x.used) { s = &x; break; } if (!s) { s = &S[0]; for (auto& x : S) if (x.last_us < s->last_us) s = &x; } return s; }
Session* find(const char* token) {
  if (!token || !*token) return nullptr; int64_t now = esp_timer_get_time();
  for (auto& s : S) if (s.used && !strcmp(s.token, token)) {
    if (s.client < 0 && now - s.last_us > (int64_t)cfg::kSessionTtlMs * 1000) { s.used = false; return nullptr; }      // sesja z hasla wygasa; token sparowanego klienta nie (odwolywany recznie)
    if (s.level && now - s.level_us > (int64_t)cfg::kLevelAutoLockMs * 1000) s.level = 0;      // auto-blokada poziomu po 5 min bezczynnosci
    s.last_us = now; if (s.level) s.level_us = now; return &s;
  }
  if (strlen(token) == 43) {                                                                         // moze to token sparowanego klienta (32 B w base64url)
    cl_load(); uint8_t h[32]; tok_hash(token, h);
    for (int i = 0; i < g_ncl; i++) if (ct_eq(h, CL[i].hash, 32)) { Session* s = slot(); *s = {}; s->used = true; strlcpy(s->token, token, sizeof s->token); s->client = (int8_t)i; s->last_us = now; return s; }
  }
  return nullptr;
}
int pair(const char* pw, const char* name, uint32_t ip, char* tok) {
  if (!tekla::name_ok(name)) return 4; char tmp[48]; int rc = login(pw, ip, tmp); if (rc) return rc;
  if (Session* t = find(tmp)) t->used = false;                                                      // sesja pomocnicza z login() niepotrzebna
  cl_load(); if (g_ncl >= 8) return 3;
  uint8_t r[32]; esp_fill_random(r, sizeof r); char b[48]; if (!tekla::b64url(r, sizeof r, b, sizeof b)) return 3;
  Client& c = CL[g_ncl]; tok_hash(b, c.hash); memset(c.name, 0, sizeof c.name); strlcpy(c.name, name, sizeof c.name); g_ncl++;
  if (!cl_save()) { g_ncl--; return 3; } strcpy(tok, b); return 0;
}
int client_count() { cl_load(); return g_ncl; }
const char* client_name(int i) { cl_load(); return i >= 0 && i < g_ncl ? CL[i].name : ""; }
bool client_revoke(int i) { cl_load(); if (i < 0 || i >= g_ncl) return false; for (int k = i; k + 1 < g_ncl; k++) CL[k] = CL[k + 1]; g_ncl--; bool ok = cl_save(); for (auto& s : S) if (s.client >= 0) s.used = false; return ok; }
void client_revoke_all() { cl_load(); g_ncl = 0; cl_save(); for (auto& s : S) if (s.client >= 0) s.used = false; }
int service_unlock(Session* s, const char* pw, uint32_t ip) { char tmp[48]; int rc = login(pw, ip, tmp); if (rc) return rc; if (Session* t = find(tmp)) t->used = false; s->svc_us = esp_timer_get_time(); return 0; }
bool service_ok(Session* s) { return s && s->svc_us && esp_timer_get_time() - s->svc_us < 5LL * 60 * 1000000; }
int set_level(Session* s, const char* pin) {
  int64_t now = esp_timer_get_time(); if (now < g_pin_lock_until) return 2;
  nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return 1; int lv = 0; if (check(h, "pin3", pin)) lv = 3; else if (check(h, "pin2", pin)) lv = 2; else if (check(h, "pin1", pin)) lv = 1; nvs_close(h);
  if (!lv) { if (++g_pin_fails >= 5) { g_pin_lock_until = now + 300LL * 1000000; g_pin_fails = 0; } return 1; }
  g_pin_fails = 0; s->level = lv; s->level_us = now; return 0;
}
void drop_level(Session* s) { s->level = 0; }
tekla::Mode mode() { return g_mode; }
bool set_mode(tekla::Mode m, const char* pw) { nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return false; bool ok = check(h, "pw", pw); nvs_close(h); if (ok) g_mode = m; return ok; }
bool verify_password(const char* pw) { nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return false; bool ok = check(h, "pw", pw); nvs_close(h); return ok; }
bool set_password(const char* pw) { if (strlen(pw) < 8) return false; nvs_handle_t h; if (nvs_open("auth", NVS_READWRITE, &h) != ESP_OK) return false; bool ok = store(h, "pw", pw) && nvs_commit(h) == ESP_OK; nvs_close(h); for (auto& s : S) if (s.client < 0) s.used = false; return ok; }
void factory_reset() { nvs_handle_t h; if (nvs_open("auth", NVS_READWRITE, &h) == ESP_OK) { nvs_erase_all(h); nvs_commit(h); nvs_close(h); } if (nvs_open("wifi", NVS_READWRITE, &h) == ESP_OK) { nvs_erase_all(h); nvs_commit(h); nvs_close(h); } if (nvs_open("ip", NVS_READWRITE, &h) == ESP_OK) { nvs_erase_all(h); nvs_commit(h); nvs_close(h); } if (nvs_open("dev", NVS_READWRITE, &h) == ESP_OK) { nvs_erase_all(h); nvs_commit(h); nvs_close(h); } g_ncl = 0; for (auto& x : S) x.used = false; }
bool dev_configured() { nvs_handle_t h; if (nvs_open("auth", NVS_READONLY, &h) != ESP_OK) return false; size_t n = 0; bool ok = nvs_get_blob(h, "dev", nullptr, &n) == ESP_OK && n == 48; nvs_close(h); return ok; }
bool dev_set(const char* pw) { if (strlen(pw) < 8) return false; nvs_handle_t h; if (nvs_open("auth", NVS_READWRITE, &h) != ESP_OK) return false; bool ok = store(h, "dev", pw) && nvs_commit(h) == ESP_OK; nvs_close(h); return ok; }
int dev_unlock(Session* s, const char* pw, uint32_t ip) { int rc = verify_rl("dev", pw, ip); if (rc) return rc; s->dev_us = esp_timer_get_time(); return 0; }
bool dev_ok(Session* s) { return s && s->dev_us && esp_timer_get_time() - s->dev_us < 5LL * 60 * 1000000; }
}  // namespace auth
