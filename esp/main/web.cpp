// Serwer HTTP + WebSocket (esp_http_server). REST wg docs/ESP_API_V2.md sekcja 8; /ws sekcja 9. NIEKOMPILOWANE w srodowisku agenta - do weryfikacji na sprzecie.
#include "web.hpp"
#include "auth.hpp"
#include "bus.hpp"
#include "config.hpp"
#include "ota.hpp"
#include "net.hpp"
#include "tekla/guard.hpp"
#include "tekla/live.hpp"
#include "tekla/ui_pack.hpp"
#include "esp_http_server.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "cJSON.h"
#include "nvs.h"
#include "lwip/sockets.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <cstring>
#include <cstdlib>
using namespace tekla;
static const char* TAG = "web"; static httpd_handle_t g_srv;
// ---- pomocnicze
static esp_err_t jsend(httpd_req_t* r, const char* status, const char* body) { httpd_resp_set_status(r, status); httpd_resp_set_type(r, "application/json"); httpd_resp_set_hdr(r, "Cache-Control", "no-store"); return httpd_resp_sendstr(r, body); }
static esp_err_t jerr(httpd_req_t* r, const char* status, const char* code) { char b[96]; snprintf(b, sizeof b, "{\"ok\":false,\"code\":\"%s\"}", code); return jsend(r, status, b); }
static cJSON* body_json(httpd_req_t* r) { if (r->content_len == 0 || r->content_len > 1024) return nullptr; char buf[1025]; int n = httpd_req_recv(r, buf, r->content_len); if (n <= 0) return nullptr; buf[n] = 0; return cJSON_Parse(buf); }
static auth::Session* session(httpd_req_t* r) { char v[80] = {}; if (httpd_req_get_hdr_value_str(r, "Authorization", v, sizeof v) != ESP_OK || strncmp(v, "Bearer ", 7)) return nullptr; return auth::find(v + 7); }
static uint32_t peer_ip(httpd_req_t* r) { int fd = httpd_req_to_sockfd(r); struct sockaddr_in6 a; socklen_t l = sizeof a; if (getpeername(fd, (struct sockaddr*)&a, &l) != 0) return 0; uint32_t x; memcpy(&x, &a.sin6_addr.un.u32_addr[3], 4); return x; }
static void urldecode(char* s) { char* o = s; for (; *s; s++) { if (*s == '%' && s[1] && s[2]) { char h[3] = {s[1], s[2], 0}; *o++ = (char)strtol(h, nullptr, 16); s += 2; } else if (*s == '+') *o++ = ' '; else *o++ = *s; } *o = 0; }
static const char* werr(WriteErr e, const char** st) {
  switch (e) { case WriteErr::ReadOnlyMode: *st = "409 Conflict"; return "read_only_mode"; case WriteErr::UnknownPid: *st = "404 Not Found"; return "unknown_pid"; case WriteErr::NotWritable: *st = "409 Conflict"; return "not_writable";
    case WriteErr::Blocked: *st = "403 Forbidden"; return "blocked"; case WriteErr::NoEncoding: *st = "409 Conflict"; return "no_encoding"; case WriteErr::OutOfRange: *st = "400 Bad Request"; return "out_of_range";
    case WriteErr::LevelTooLow: *st = "403 Forbidden"; return "level_too_low"; default: *st = "500 Internal Server Error"; return "error"; }
}
static double decode_raw(const PidEntry* e, uint32_t raw) { double v = e->type == TT_S16 ? (int16_t)(raw & 0xFFFF) : e->type == TT_U16 ? (raw & 0xFFFF) : e->type == TT_U8 ? (raw & 0xFF) : raw; return e->enc == WE_SCALED ? v * e->scale : v; }
// ---- WebSocket
struct Cli { int fd; bool auth; uint8_t mask; }; static Cli C[8];
static uint8_t screen_mask(const char* n) { if (!strcmp(n, "kociol")) return LS_KOCIOL; if (!strcmp(n, "palnik")) return LS_PALNIK; if (n[0] == 'c' && n[1] == 'o' && n[2] >= '1' && n[2] <= '4') return LS_CO; if (!strcmp(n, "cwu")) return LS_CWU; if (!strcmp(n, "bufor")) return LS_BUFOR; if (!strcmp(n, "solary")) return LS_SOLAR; if (!strcmp(n, "magazyn")) return LS_MAGAZYN; return 0; }
static Cli* cli(int fd) { for (auto& c : C) if (c.fd == fd) return &c; for (auto& c : C) if (!c.fd) { c = {fd, false, 0}; return &c; } return nullptr; }
static void update_mask() { uint8_t m = 0; for (auto& c : C) if (c.fd && c.auth) m |= c.mask; bus::set_screen_mask(m); }
static void ws_send(int fd, const char* s) { httpd_ws_frame_t f{}; f.type = HTTPD_WS_TYPE_TEXT; f.payload = (uint8_t*)s; f.len = strlen(s); httpd_ws_send_frame_async(g_srv, fd, &f); }
static esp_err_t h_ws(httpd_req_t* r) {
  if (r->method == HTTP_GET) return ESP_OK;                                      // handshake
  httpd_ws_frame_t f{}; if (httpd_ws_recv_frame(r, &f, 0) != ESP_OK) return ESP_FAIL; if (f.len == 0 || f.len > 512) return ESP_OK;
  uint8_t buf[513]; f.payload = buf; if (httpd_ws_recv_frame(r, &f, f.len) != ESP_OK) return ESP_FAIL; buf[f.len] = 0;
  cJSON* j = cJSON_Parse((char*)buf); if (!j) return ESP_OK; int fd = httpd_req_to_sockfd(r); Cli* c = cli(fd); const cJSON* t = cJSON_GetObjectItem(j, "t");
  if (c && t && cJSON_IsString(t)) {
    if (!strcmp(t->valuestring, "auth")) { const cJSON* tk = cJSON_GetObjectItem(j, "token"); c->auth = tk && cJSON_IsString(tk) && auth::find(tk->valuestring); ws_send(fd, c->auth ? "{\"t\":\"hello\"}" : "{\"t\":\"err\"}");
      if (c->auth) { LiveItem it[64]; char out[1400]; size_t n = bus::snapshot(it, 64); if (n && tekla::json_live(it, n, out, sizeof out)) ws_send(fd, out); } }
    else if (c->auth && !strcmp(t->valuestring, "sub")) { const cJSON* a = cJSON_GetObjectItem(j, "sets"); uint8_t m = 0; cJSON* e; cJSON_ArrayForEach(e, a) if (cJSON_IsString(e)) m |= screen_mask(e->valuestring); c->mask = m; update_mask(); }
  }
  cJSON_Delete(j); return ESP_OK;
}
static void ws_task(void*) {                                                      // push zmian co 250 ms
  for (;;) { vTaskDelay(pdMS_TO_TICKS(250)); LiveItem it[64]; size_t n = bus::changes(it, 64); if (!n) continue; static char out[1400]; if (!tekla::json_live(it, n, out, sizeof out)) continue;
    for (auto& c : C) if (c.fd && c.auth) { if (httpd_ws_get_fd_info(g_srv, c.fd) != HTTPD_WS_CLIENT_WEBSOCKET) { c = {}; continue; } ws_send(c.fd, out); } }
}
static void ws_broadcast_param(uint16_t pid, double v) { char b[64]; snprintf(b, sizeof b, "{\"t\":\"param\",\"pid\":\"0x%04X\",\"v\":%g}", pid, v); for (auto& c : C) if (c.fd && c.auth) ws_send(c.fd, b); }
// ---- REST
static esp_err_t h_login(httpd_req_t* r) { cJSON* j = body_json(r); const cJSON* p = j ? cJSON_GetObjectItem(j, "password") : nullptr; if (!p || !cJSON_IsString(p)) { cJSON_Delete(j); return jerr(r, "400 Bad Request", "bad_request"); }
  char tok[48]; int rc = auth::login(p->valuestring, peer_ip(r), tok); cJSON_Delete(j); if (rc == 2) return jerr(r, "429 Too Many Requests", "locked"); if (rc) return jerr(r, "401 Unauthorized", "bad_password");
  char b[160]; snprintf(b, sizeof b, "{\"ok\":true,\"token\":\"%s\",\"mode\":\"%s\",\"level\":0}", tok, auth::mode() == Mode::Full ? "full" : "readonly"); return jsend(r, "200 OK", b); }
static esp_err_t h_setup(httpd_req_t* r) { if (auth::configured()) return jerr(r, "409 Conflict", "already_configured"); cJSON* j = body_json(r); if (!j) return jerr(r, "400 Bad Request", "bad_request");
  auto s = [&](const char* k) { const cJSON* x = cJSON_GetObjectItem(j, k); return x && cJSON_IsString(x) ? x->valuestring : ""; };
  bool ok = auth::setup(s("password"), s("pin1"), s("pin2"), s("pin3")); const char* ss = s("ssid"); if (ok && *ss) { nvs_handle_t h; if (nvs_open("wifi", NVS_READWRITE, &h) == ESP_OK) { nvs_set_str(h, "ssid", ss); nvs_set_str(h, "pass", s("wifi_pass")); nvs_commit(h); nvs_close(h); } }
  cJSON_Delete(j); return ok ? jsend(r, "200 OK", "{\"ok\":true,\"reboot\":true}") : jerr(r, "400 Bad Request", "bad_request"); }
#define NEED_SESSION auth::Session* S = session(r); if (!S) return jerr(r, "401 Unauthorized", "unauthorized")
static esp_err_t h_mode(httpd_req_t* r) { NEED_SESSION; (void)S; cJSON* j = body_json(r); const cJSON* m = j ? cJSON_GetObjectItem(j, "mode") : nullptr; const cJSON* p = j ? cJSON_GetObjectItem(j, "password") : nullptr;
  bool ok = m && p && cJSON_IsString(m) && cJSON_IsString(p) && auth::set_mode(!strcmp(m->valuestring, "full") ? Mode::Full : Mode::ReadOnly, p->valuestring); cJSON_Delete(j);
  if (!ok) return jerr(r, "403 Forbidden", "bad_password"); char b[64]; snprintf(b, sizeof b, "{\"ok\":true,\"mode\":\"%s\"}", auth::mode() == Mode::Full ? "full" : "readonly"); return jsend(r, "200 OK", b); }
static esp_err_t h_level(httpd_req_t* r) { NEED_SESSION; if (r->method == HTTP_DELETE) { auth::drop_level(S); return jsend(r, "200 OK", "{\"ok\":true,\"level\":0}"); }
  cJSON* j = body_json(r); const cJSON* p = j ? cJSON_GetObjectItem(j, "pin") : nullptr; int rc = p && cJSON_IsString(p) ? auth::set_level(S, p->valuestring) : 1; cJSON_Delete(j);
  if (rc == 2) return jerr(r, "423 Locked", "locked"); if (rc) return jerr(r, "403 Forbidden", "bad_pin"); char b[48]; snprintf(b, sizeof b, "{\"ok\":true,\"level\":%d}", S->level); return jsend(r, "200 OK", b); }
static esp_err_t h_info(httpd_req_t* r) { NEED_SESSION; char b[256]; snprintf(b, sizeof b, "{\"ok\":true,\"fw\":\"0.1\",\"ui\":\"0.1\",\"node\":%d,\"can\":\"%s\",\"uptime\":%d,\"mode\":\"%s\",\"level\":%d}", cfg::kNodeId, bus::can_ok() ? "ok" : "brak odpowiedzi", (int)(esp_timer_get_time() / 1000000), auth::mode() == Mode::Full ? "full" : "readonly", S->level); return jsend(r, "200 OK", b); }
static esp_err_t h_state(httpd_req_t* r) { NEED_SESSION; (void)S; LiveItem it[64]; size_t n = bus::snapshot(it, 64); static char out[1400]; if (!n || !tekla::json_live(it, n, out, sizeof out)) return jsend(r, "200 OK", "{\"ok\":true,\"v\":{}}"); return jsend(r, "200 OK", out); }
static esp_err_t h_params(httpd_req_t* r) { NEED_SESSION; (void)S; char q[160] = {}, key[96] = {}; httpd_req_get_url_query_str(r, q, sizeof q); if (httpd_query_key_value(q, "list", key, sizeof key) != ESP_OK) return jerr(r, "400 Bad Request", "bad_request"); urldecode(key);
  const ListDef* L = nullptr; for (size_t i = 0; i < kListCount; i++) if (!strcmp(kLists[i].key, key)) L = &kLists[i]; if (!L) return jerr(r, "404 Not Found", "unknown_list");
  httpd_resp_set_type(r, "application/json"); httpd_resp_sendstr_chunk(r, "{\"ok\":true,\"values\":{"); for (uint16_t i = 0; i < L->n; i++) { uint32_t raw; const PidEntry* e = find_pid(L->pids[i]); char b[48] = ""; if (e && bus::read(L->pids[i], &raw)) snprintf(b, sizeof b, "%s\"0x%04X\":%g", i ? "," : "", L->pids[i], decode_raw(e, raw)); if (*b) httpd_resp_sendstr_chunk(r, b); }
  httpd_resp_sendstr_chunk(r, "}}"); return httpd_resp_sendstr_chunk(r, nullptr); }
static esp_err_t h_param_put(httpd_req_t* r) { NEED_SESSION; unsigned pid = 0; if (sscanf(r->uri, "/api/param/0x%x", &pid) != 1) return jerr(r, "400 Bad Request", "bad_request");
  cJSON* j = body_json(r); const cJSON* v = j ? cJSON_GetObjectItem(j, "value") : nullptr; if (!v || !cJSON_IsNumber(v)) { cJSON_Delete(j); return jerr(r, "400 Bad Request", "bad_request"); } double dv = v->valuedouble; cJSON_Delete(j);
  WriteResult w = plan_write(auth::mode(), S->level, cfg::kBoilerNode, (uint16_t)pid, dv); if (w.err != WriteErr::Ok) { const char* st; const char* c = werr(w.err, &st); return jerr(r, st, c); }
  const PidEntry* e = find_pid((uint16_t)pid); if (!bus::write((uint16_t)pid, w.raw)) return jerr(r, "502 Bad Gateway", "can_timeout");
  uint32_t back; if (!bus::read((uint16_t)pid, &back) || (e->type == TT_U8 ? (back & 0xFF) != (w.raw & 0xFF) : e->type == TT_U32 ? back != w.raw : (back & 0xFFFF) != (w.raw & 0xFFFF))) return jerr(r, "502 Bad Gateway", "verify_failed");   // odczyt zwrotny po zapisie
  double shown = decode_raw(e, back); char b[64]; snprintf(b, sizeof b, "{\"ok\":true,\"v\":%g}", shown); ws_broadcast_param((uint16_t)pid, shown); return jsend(r, "200 OK", b); }
static esp_err_t h_alarms(httpd_req_t* r) { NEED_SESSION; (void)S; return jsend(r, "200 OK", "{\"ok\":true,\"alarms\":[]}"); }          // TODO: historia alarmow (rejestry 0x4011-0x404B)
static esp_err_t h_ui_confirm(httpd_req_t* r) { NEED_SESSION; (void)S; ota::confirm_ui(); return jsend(r, "200 OK", "{\"ok\":true}"); }
static esp_err_t h_ota_fw(httpd_req_t* r) { NEED_SESSION; (void)S; if (auth::mode() != Mode::Full) return jerr(r, "409 Conflict", "read_only_mode"); return ota::handle_fw(r); }
static esp_err_t h_ota_ui(httpd_req_t* r) { NEED_SESSION; (void)S; if (auth::mode() != Mode::Full) return jerr(r, "409 Conflict", "read_only_mode"); return ota::handle_ui(r); }
// ---- statyczne UI z partycji ui_x (paczka TPUI)
static const char* RECOVERY = "<!doctype html><meta charset=utf-8><title>TeklaPellet</title><body style='font-family:sans-serif;padding:2em'><h2>Brak poprawnego UI</h2><p>Wgraj paczke UI (POST /api/ota/ui, podpisana).</p>";
static esp_err_t h_static(httpd_req_t* r) {
  const char* p = r->uri; if (!strcmp(p, "/")) p = "/index.html"; p++; char path[64]; strlcpy(path, p, sizeof path); char* q = strchr(path, '?'); if (q) *q = 0; if (!strcmp(path, "gen/model.min.json")) strcpy(path, "model.min.json");
  static const esp_partition_t* part; static const void* mem; static esp_partition_mmap_handle_t mh; if (!mem) { part = ota::ui_active(); if (part && esp_partition_mmap(part, 0, part->size, ESP_PARTITION_MMAP_DATA, &mem, &mh) != ESP_OK) mem = nullptr; }
  UiFile f; if (!mem || !tekla::ui_find((const uint8_t*)mem, part->size, path, &f)) { if (!strcmp(path, "index.html") || !mem) { httpd_resp_set_type(r, "text/html; charset=utf-8"); return httpd_resp_sendstr(r, RECOVERY); } httpd_resp_send_404(r); return ESP_OK; }
  httpd_resp_set_type(r, f.mime); if (f.gzip) httpd_resp_set_hdr(r, "Content-Encoding", "gzip"); httpd_resp_set_hdr(r, "Cache-Control", strncmp(path, "assets/", 7) ? "no-cache" : "max-age=86400"); return httpd_resp_send(r, (const char*)f.data, f.size);
}
bool web::start() {
  httpd_config_t c = HTTPD_DEFAULT_CONFIG(); c.uri_match_fn = httpd_uri_match_wildcard; c.max_uri_handlers = 24; c.stack_size = 8192; c.lru_purge_enable = true; c.max_open_sockets = 10;
  if (httpd_start(&g_srv, &c) != ESP_OK) return false;
  auto reg = [&](const char* u, httpd_method_t m, esp_err_t (*h)(httpd_req_t*), bool ws = false) { httpd_uri_t x{}; x.uri = u; x.method = m; x.handler = h; x.is_websocket = ws; httpd_register_uri_handler(g_srv, &x); };
  reg("/ws", HTTP_GET, h_ws, true); reg("/api/login", HTTP_POST, h_login); reg("/api/setup", HTTP_POST, h_setup); reg("/api/mode", HTTP_POST, h_mode); reg("/api/level", HTTP_POST, h_level); reg("/api/level", HTTP_DELETE, h_level);
  reg("/api/info", HTTP_GET, h_info); reg("/api/state", HTTP_GET, h_state); reg("/api/params", HTTP_GET, h_params); reg("/api/param/*", HTTP_PUT, h_param_put); reg("/api/alarms", HTTP_GET, h_alarms);
  reg("/api/ui/confirm", HTTP_POST, h_ui_confirm); reg("/api/ota/fw", HTTP_POST, h_ota_fw); reg("/api/ota/ui", HTTP_POST, h_ota_ui); reg("/*", HTTP_GET, h_static);
  xTaskCreate(ws_task, "ws", 4096, nullptr, 4, nullptr); ESP_LOGI(TAG, "serwer HTTP/WS uruchomiony"); return true;
}
