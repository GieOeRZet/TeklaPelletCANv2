// Serwer HTTP + WebSocket (esp_http_server). REST wg docs/ESP_API_V2.md sekcja 8; /ws sekcja 9. NIEKOMPILOWANE w srodowisku agenta - do weryfikacji na sprzecie.
#include "web.hpp"
#include "auth.hpp"
#include "bus.hpp"
#include "config.hpp"
#include "ota.hpp"
#include "net.hpp"
#include "device.hpp"
#include "service_page.hpp"
#include "tekla/netcfg.hpp"
#include "tekla/guard.hpp"
#include "tekla/live.hpp"
#include "esp_http_server.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "cJSON.h"
#include "nvs.h"
#include "lwip/sockets.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_system.h"
#include <cstring>
#include <cstdlib>
using namespace tekla;
static const char* TAG = "web"; static httpd_handle_t g_srv; static void restart_later(void*);
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
  cJSON_Delete(j); if (ok) { jsend(r, "200 OK", "{\"ok\":true,\"reboot\":true}"); xTaskCreate(restart_later, "rst", 2048, nullptr, 5, nullptr); return ESP_OK; } return jerr(r, "400 Bad Request", "bad_request"); }
#define NEED_SESSION auth::Session* S = session(r); if (!S) return jerr(r, "401 Unauthorized", "unauthorized")
static esp_err_t h_mode(httpd_req_t* r) { NEED_SESSION; (void)S; cJSON* j = body_json(r); const cJSON* m = j ? cJSON_GetObjectItem(j, "mode") : nullptr; const cJSON* p = j ? cJSON_GetObjectItem(j, "password") : nullptr;
  bool ok = m && p && cJSON_IsString(m) && cJSON_IsString(p) && auth::set_mode(!strcmp(m->valuestring, "full") ? Mode::Full : Mode::ReadOnly, p->valuestring); cJSON_Delete(j);
  if (!ok) return jerr(r, "403 Forbidden", "bad_password"); char b[64]; snprintf(b, sizeof b, "{\"ok\":true,\"mode\":\"%s\"}", auth::mode() == Mode::Full ? "full" : "readonly"); return jsend(r, "200 OK", b); }
static esp_err_t h_level(httpd_req_t* r) { NEED_SESSION; if (r->method == HTTP_DELETE) { auth::drop_level(S); return jsend(r, "200 OK", "{\"ok\":true,\"level\":0}"); }
  cJSON* j = body_json(r); const cJSON* p = j ? cJSON_GetObjectItem(j, "pin") : nullptr; int rc = p && cJSON_IsString(p) ? auth::set_level(S, p->valuestring) : 1; cJSON_Delete(j);
  if (rc == 2) return jerr(r, "423 Locked", "locked"); if (rc) return jerr(r, "403 Forbidden", "bad_pin"); char b[48]; snprintf(b, sizeof b, "{\"ok\":true,\"level\":%d}", S->level); return jsend(r, "200 OK", b); }
static esp_err_t h_info(httpd_req_t* r) { NEED_SESSION; char b[360]; snprintf(b, sizeof b, "{\"ok\":true,\"id\":\"%s\",\"name\":\"%s\",\"fw\":\"%s\",\"api\":%d,\"node\":%d,\"can\":\"%s\",\"uptime\":%d,\"mode\":\"%s\",\"level\":%d,\"client\":%s}", device::id(), device::name(), cfg::kVersion, cfg::kApiVersion, cfg::kNodeId, bus::can_ok() ? "ok" : "brak odpowiedzi", (int)(esp_timer_get_time() / 1000000), auth::mode() == Mode::Full ? "full" : "readonly", S->level, S->client >= 0 ? "true" : "false"); return jsend(r, "200 OK", b); }
static esp_err_t h_state(httpd_req_t* r) { NEED_SESSION; (void)S; LiveItem it[64]; size_t n = bus::snapshot(it, 64); static char out[1400]; if (!n || !tekla::json_live(it, n, out, sizeof out)) return jsend(r, "200 OK", "{\"ok\":true,\"v\":{}}"); return jsend(r, "200 OK", out); }
static esp_err_t h_params(httpd_req_t* r) { NEED_SESSION; (void)S; char q[512] = {}, key[96] = {}; httpd_req_get_url_query_str(r, q, sizeof q); if (httpd_query_key_value(q, "pids", key, sizeof key) == ESP_OK) { urldecode(key); httpd_resp_set_type(r, "application/json"); httpd_resp_sendstr_chunk(r, "{\"ok\":true,\"values\":{"); bool first = true; char* sp = nullptr; for (char* t = strtok_r(key, ",", &sp); t; t = strtok_r(nullptr, ",", &sp)) { unsigned pid = 0; if (sscanf(t, "0x%x", &pid) != 1) continue; uint32_t raw; const PidEntry* e = find_pid((uint16_t)pid); char b[48]; if (e && bus::read((uint16_t)pid, &raw)) { snprintf(b, sizeof b, "%s\"0x%04X\":%g", first ? "" : ",", pid, decode_raw(e, raw)); first = false; httpd_resp_sendstr_chunk(r, b); } } httpd_resp_sendstr_chunk(r, "}}"); return httpd_resp_sendstr_chunk(r, nullptr); } if (httpd_query_key_value(q, "list", key, sizeof key) != ESP_OK) return jerr(r, "400 Bad Request", "bad_request"); urldecode(key);
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
// ---- modul: Wi-Fi, haslo, restart, reset, dziennik, siec
static char g_log[12][72]; static int g_logn;
static void logx(const char* m) { snprintf(g_log[g_logn++ % 12], sizeof g_log[0], "%ds %s", (int)(esp_timer_get_time() / 1000000), m); }
#define NEED_SERVICE if (!auth::service_ok(S)) return jerr(r, "403 Forbidden", "service_locked")
static void restart_later(void*) { vTaskDelay(pdMS_TO_TICKS(800)); esp_restart(); }
static const cJSON* jstr(const cJSON* j, const char* k) { const cJSON* x = j ? cJSON_GetObjectItem(j, k) : nullptr; return x && cJSON_IsString(x) ? x : nullptr; }
static esp_err_t h_identity(httpd_req_t* r) { char b[320]; snprintf(b, sizeof b, "{\"ok\":true,\"id\":\"%s\",\"name\":\"%s\",\"model\":\"TeklaPellet\",\"fw\":\"%s\",\"api\":%d,\"node\":%d,\"setup_needed\":%s}", device::id(), device::name(), cfg::kVersion, cfg::kApiVersion, cfg::kNodeId, auth::configured() ? "false" : "true"); return jsend(r, "200 OK", b); }
static esp_err_t h_pair(httpd_req_t* r) { cJSON* j = body_json(r); const cJSON* p = jstr(j, "password"); const cJSON* n = jstr(j, "name"); if (!p || !n) { cJSON_Delete(j); return jerr(r, "400 Bad Request", "bad_request"); }
  char tok[48]; int rc = auth::pair(p->valuestring, n->valuestring, peer_ip(r), tok); cJSON_Delete(j);
  if (rc == 2) return jerr(r, "429 Too Many Requests", "locked"); if (rc == 1) return jerr(r, "401 Unauthorized", "bad_password"); if (rc == 3) return jerr(r, "409 Conflict", "too_many_clients"); if (rc == 4) return jerr(r, "400 Bad Request", "bad_name");
  char b[200]; snprintf(b, sizeof b, "{\"ok\":true,\"token\":\"%s\",\"id\":\"%s\"}", tok, device::id()); logx("sparowano klienta"); return jsend(r, "200 OK", b); }
static esp_err_t h_unlock(httpd_req_t* r) { NEED_SESSION; cJSON* j = body_json(r); const cJSON* p = jstr(j, "password"); int rc = p ? auth::service_unlock(S, p->valuestring, peer_ip(r)) : 1; cJSON_Delete(j);
  if (rc == 2) return jerr(r, "429 Too Many Requests", "locked"); if (rc) return jerr(r, "403 Forbidden", "bad_password"); logx("serwis odblokowany"); return jsend(r, "200 OK", "{\"ok\":true,\"valid_s\":300}"); }
static esp_err_t h_clients(httpd_req_t* r) { NEED_SESSION; if (r->method == HTTP_DELETE) { NEED_SERVICE; auth::client_revoke_all(); return jsend(r, "200 OK", "{\"ok\":true}"); }
  static char out[520]; size_t k = snprintf(out, sizeof out, "{\"ok\":true,\"clients\":["); for (int i = 0; i < auth::client_count(); i++) k += snprintf(out + k, sizeof out - k, "%s{\"i\":%d,\"name\":\"%s\"}", i ? "," : "", i, auth::client_name(i)); snprintf(out + k, sizeof out - k, "]}"); return jsend(r, "200 OK", out); }
static esp_err_t h_client_del(httpd_req_t* r) { NEED_SESSION; NEED_SERVICE; int i = -1; if (sscanf(r->uri, "/api/clients/%d", &i) != 1) return jerr(r, "400 Bad Request", "bad_request"); if (!auth::client_revoke(i)) return jerr(r, "404 Not Found", "unknown_client"); logx("odwolano klienta"); return jsend(r, "200 OK", "{\"ok\":true}"); }
static esp_err_t h_device(httpd_req_t* r) { NEED_SESSION; NEED_SERVICE; cJSON* j = body_json(r); const cJSON* n = jstr(j, "name"); bool ok = n && device::set_name(n->valuestring); cJSON_Delete(j); if (!ok) return jerr(r, "400 Bad Request", "bad_name"); net::refresh_identity(); return jsend(r, "200 OK", "{\"ok\":true}"); }
static void ip_json(char* b, size_t cap, const net::IpCfg& c) { char a[16], m[16], g[16], d1[16], d2[16]; format_ipv4(c.ip, a, sizeof a); format_ipv4(c.mask, m, sizeof m); format_ipv4(c.gw, g, sizeof g); format_ipv4(c.dns1, d1, sizeof d1); format_ipv4(c.dns2, d2, sizeof d2);
  snprintf(b, cap, "{\"dhcp\":%s,\"ip\":\"%s\",\"mask\":\"%s\",\"gw\":\"%s\",\"dns1\":\"%s\",\"dns2\":\"%s\"}", c.dhcp ? "true" : "false", a, m, g, d1, d2); }
static esp_err_t h_ip(httpd_req_t* r) { NEED_SESSION;
  if (r->method == HTTP_POST) { NEED_SERVICE; cJSON* j = body_json(r); if (!j) return jerr(r, "400 Bad Request", "bad_request"); net::IpCfg c; const cJSON* dh = cJSON_GetObjectItem(j, "dhcp"); c.dhcp = !dh || !cJSON_IsFalse(dh); bool ok = true;
    if (!c.dhcp) { const cJSON *a = jstr(j, "ip"), *m = jstr(j, "mask"), *g = jstr(j, "gw"), *d1 = jstr(j, "dns1"), *d2 = jstr(j, "dns2"); ok = a && m && g && parse_ipv4(a->valuestring, &c.ip) && parse_ipv4(m->valuestring, &c.mask) && parse_ipv4(g->valuestring, &c.gw) && static_ip_ok(c.ip, c.mask, c.gw);
      if (ok && d1 && *d1->valuestring) ok = parse_ipv4(d1->valuestring, &c.dns1); if (ok && d2 && *d2->valuestring) ok = parse_ipv4(d2->valuestring, &c.dns2); }
    cJSON_Delete(j); if (!ok) return jerr(r, "400 Bad Request", "bad_ip"); if (!net::ip_apply(c)) return jerr(r, "409 Conflict", "busy");
    char a[16], b[100]; format_ipv4(c.ip, a, sizeof a); snprintf(b, sizeof b, "{\"ok\":true,\"confirm_within_s\":60,\"new_ip\":\"%s\"}", a); logx("zmiana IP"); return jsend(r, "200 OK", b); }
  net::IpCfg sv, cu; bool pend; net::ip_get(&sv, &cu, &pend); static char b[400]; char s1[160], s2[160]; ip_json(s1, sizeof s1, sv); ip_json(s2, sizeof s2, cu); snprintf(b, sizeof b, "{\"ok\":true,\"saved\":%s,\"current\":%s,\"pending\":%s}", s1, s2, pend ? "true" : "false"); return jsend(r, "200 OK", b); }
static esp_err_t h_net_confirm(httpd_req_t* r) { NEED_SESSION; (void)S; net::ip_confirm(); return jsend(r, "200 OK", "{\"ok\":true}"); }
static esp_err_t h_wifi(httpd_req_t* r) { NEED_SESSION;
  if (r->method == HTTP_POST) { NEED_SERVICE; cJSON* j = body_json(r); const cJSON* a = jstr(j, "ssid"); const cJSON* b = jstr(j, "pass"); bool ok = a && b && net::wifi_apply(a->valuestring, b->valuestring); cJSON_Delete(j); if (ok) logx("Wi-Fi: zmiana sieci"); return ok ? jsend(r, "200 OK", "{\"ok\":true,\"apply_s\":40}") : jerr(r, "400 Bad Request", "bad_request"); }
  char ss[40]; net::current_ssid(ss, sizeof ss); char b[120]; snprintf(b, sizeof b, "{\"ok\":true,\"ssid\":\"%s\",\"connected\":%s}", ss, net::connected() ? "true" : "false"); return jsend(r, "200 OK", b); }
static esp_err_t h_wifi_scan(httpd_req_t* r) { NEED_SESSION; (void)S; static char out[700]; net::scan_json(out, sizeof out); return jsend(r, "200 OK", out); }
static esp_err_t h_password(httpd_req_t* r) { NEED_SESSION; NEED_SERVICE; cJSON* j = body_json(r); const cJSON* n = jstr(j, "new"); bool ok = n && auth::set_password(n->valuestring); cJSON_Delete(j); if (ok) logx("haslo zmienione"); return ok ? jsend(r, "200 OK", "{\"ok\":true}") : jerr(r, "400 Bad Request", "bad_request"); }
static esp_err_t h_restart(httpd_req_t* r) { NEED_SESSION; NEED_SERVICE; logx("restart"); jsend(r, "200 OK", "{\"ok\":true}"); xTaskCreate(restart_later, "rst", 2048, nullptr, 5, nullptr); return ESP_OK; }
static esp_err_t h_factory(httpd_req_t* r) { NEED_SESSION; NEED_SERVICE; cJSON* j = body_json(r); const cJSON* c = jstr(j, "confirm"); bool ok = c && !strcmp(c->valuestring, "RESET"); cJSON_Delete(j); if (!ok) return jerr(r, "400 Bad Request", "confirm_required");
  auth::factory_reset(); jsend(r, "200 OK", "{\"ok\":true}"); xTaskCreate(restart_later, "rst", 2048, nullptr, 5, nullptr); return ESP_OK; }
static esp_err_t h_log(httpd_req_t* r) { NEED_SESSION; (void)S; static char out[1000]; size_t k = snprintf(out, sizeof out, "{\"ok\":true,\"lines\":["); int n = g_logn < 12 ? g_logn : 12; for (int i = 0; i < n; i++) k += snprintf(out + k, sizeof out - k, "%s\"%s\"", i ? "," : "", g_log[(g_logn - n + i) % 12]); snprintf(out + k, sizeof out - k, "]}"); return jsend(r, "200 OK", out); }
static esp_err_t h_network(httpd_req_t* r) { NEED_SESSION; (void)S; char b[260]; snprintf(b, sizeof b, "{\"ok\":true,\"nodes\":[{\"node\":%d,\"name\":\"Kocioł\",\"state\":\"%s\",\"age_s\":%d},{\"node\":%d,\"name\":\"TeklaPellet (ten moduł)\",\"state\":\"pracuje\"}]}", cfg::kBoilerNode, bus::can_ok() ? "pracuje" : "brak odpowiedzi", (int)(bus::last_reply_age_ms() / 1000), cfg::kNodeId); return jsend(r, "200 OK", b); }
#define NEED_DEV if (!auth::dev_ok(S)) return jerr(r, "403 Forbidden", "dev_locked")
static esp_err_t h_dev(httpd_req_t* r) { NEED_SESSION; char url[160]; ota::beta_get_url(url, sizeof url); char b[300]; snprintf(b, sizeof b, "{\"ok\":true,\"configured\":%s,\"unlocked\":%s,\"beta_url\":\"%s\"}", auth::dev_configured() ? "true" : "false", auth::dev_ok(S) ? "true" : "false", auth::dev_ok(S) ? url : ""); return jsend(r, "200 OK", b); }
static esp_err_t h_dev_unlock(httpd_req_t* r) { NEED_SESSION; cJSON* j = body_json(r); const cJSON* p = jstr(j, "password"); int rc = p ? auth::dev_unlock(S, p->valuestring, peer_ip(r)) : 1; cJSON_Delete(j);
  if (rc == 2) return jerr(r, "429 Too Many Requests", "locked"); if (rc) return jerr(r, "403 Forbidden", "bad_password"); logx("DEV odblokowany"); return jsend(r, "200 OK", "{\"ok\":true,\"valid_s\":300}"); }
static esp_err_t h_dev_set(httpd_req_t* r) { NEED_SESSION; NEED_SERVICE; if (auth::dev_configured() && !auth::dev_ok(S)) return jerr(r, "403 Forbidden", "dev_locked");
  cJSON* j = body_json(r); const cJSON* p = jstr(j, "password"); bool ok = p && auth::dev_set(p->valuestring); cJSON_Delete(j); if (ok) logx("haslo DEV ustawione"); return ok ? jsend(r, "200 OK", "{\"ok\":true}") : jerr(r, "400 Bad Request", "bad_request"); }
static esp_err_t h_beta(httpd_req_t* r) { NEED_SESSION; NEED_DEV; char v[48] = {}, e[32] = {}; if (!ota::beta_check(v, sizeof v, e, sizeof e)) { char b[96]; snprintf(b, sizeof b, "{\"ok\":false,\"code\":\"%s\"}", e); return jsend(r, "502 Bad Gateway", b); }
  char b[160]; snprintf(b, sizeof b, "{\"ok\":true,\"version\":\"%s\",\"current\":\"%s\"}", v, cfg::kVersion); return jsend(r, "200 OK", b); }
static esp_err_t h_beta_update(httpd_req_t* r) { NEED_SESSION; NEED_DEV; if (!ota::beta_start()) return jerr(r, "409 Conflict", "busy"); logx("OTA BETA start"); return jsend(r, "200 OK", "{\"ok\":true}"); }
static esp_err_t h_beta_status(httpd_req_t* r) { NEED_SESSION; NEED_DEV; ota::BetaStatus st = ota::beta_status(); char b[160]; snprintf(b, sizeof b, "{\"ok\":true,\"state\":%d,\"pct\":%d,\"msg\":\"%s\"}", st.state, st.pct, st.msg); return jsend(r, "200 OK", b); }
static esp_err_t h_beta_url(httpd_req_t* r) { NEED_SESSION; NEED_DEV; cJSON* j = body_json(r); const cJSON* u = jstr(j, "url"); bool ok = u && ota::beta_set_url(u->valuestring); cJSON_Delete(j); return ok ? jsend(r, "200 OK", "{\"ok\":true}") : jerr(r, "400 Bad Request", "bad_url"); }
static esp_err_t h_ota_fw(httpd_req_t* r) { NEED_SESSION; NEED_SERVICE; return ota::handle_fw(r); }
static esp_err_t h_page(httpd_req_t* r) { httpd_resp_set_type(r, "text/html; charset=utf-8"); httpd_resp_set_hdr(r, "Cache-Control", "no-cache"); return httpd_resp_send(r, kServicePage, sizeof kServicePage - 1); }
bool web::start() {
  httpd_config_t c = HTTPD_DEFAULT_CONFIG(); c.uri_match_fn = httpd_uri_match_wildcard; c.max_uri_handlers = 56; c.stack_size = 8192; c.lru_purge_enable = true; c.max_open_sockets = 10;
  if (httpd_start(&g_srv, &c) != ESP_OK) return false;
  auto reg = [&](const char* u, httpd_method_t m, esp_err_t (*h)(httpd_req_t*), bool ws = false) { httpd_uri_t x{}; x.uri = u; x.method = m; x.handler = h; x.is_websocket = ws; httpd_register_uri_handler(g_srv, &x); };
  reg("/ws", HTTP_GET, h_ws, true); reg("/api/login", HTTP_POST, h_login); reg("/api/setup", HTTP_POST, h_setup); reg("/api/mode", HTTP_POST, h_mode); reg("/api/level", HTTP_POST, h_level); reg("/api/level", HTTP_DELETE, h_level);
  reg("/api/info", HTTP_GET, h_info); reg("/api/state", HTTP_GET, h_state); reg("/api/params", HTTP_GET, h_params); reg("/api/param/*", HTTP_PUT, h_param_put); reg("/api/alarms", HTTP_GET, h_alarms);
  reg("/api/wifi", HTTP_GET, h_wifi); reg("/api/wifi", HTTP_POST, h_wifi); reg("/api/wifi/scan", HTTP_GET, h_wifi_scan); reg("/api/password", HTTP_POST, h_password); reg("/api/restart", HTTP_POST, h_restart); reg("/api/factory_reset", HTTP_POST, h_factory); reg("/api/log", HTTP_GET, h_log); reg("/api/network", HTTP_GET, h_network);
  reg("/api/ota/fw", HTTP_POST, h_ota_fw); reg("/api/identity", HTTP_GET, h_identity); reg("/api/pair", HTTP_POST, h_pair); reg("/api/service/unlock", HTTP_POST, h_unlock);
  reg("/api/clients", HTTP_GET, h_clients); reg("/api/clients", HTTP_DELETE, h_clients); reg("/api/clients/*", HTTP_DELETE, h_client_del); reg("/api/device", HTTP_POST, h_device);
  reg("/api/ip", HTTP_GET, h_ip); reg("/api/ip", HTTP_POST, h_ip); reg("/api/net/confirm", HTTP_POST, h_net_confirm); reg("/api/dev", HTTP_GET, h_dev); reg("/api/dev/unlock", HTTP_POST, h_dev_unlock); reg("/api/dev/set", HTTP_POST, h_dev_set); reg("/api/dev/beta", HTTP_GET, h_beta); reg("/api/dev/beta/update", HTTP_POST, h_beta_update); reg("/api/dev/beta/status", HTTP_GET, h_beta_status); reg("/api/dev/beta/url", HTTP_POST, h_beta_url);
  reg("/", HTTP_GET, h_page); reg("/index.html", HTTP_GET, h_page);
  xTaskCreate(ws_task, "ws", 4096, nullptr, 4, nullptr); ESP_LOGI(TAG, "serwer HTTP/WS uruchomiony"); return true;
}
