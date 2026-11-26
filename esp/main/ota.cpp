#include "ota.hpp"
#include "config.hpp"
#include "esp_ota_ops.h"
#include "esp_http_client.h"
#include "esp_crt_bundle.h"
#include "cJSON.h"
#include "nvs.h"
#include "esp_log.h"
#include "esp_app_desc.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <cstring>
namespace ota {
static const char* TAG = "ota"; static volatile bool g_http, g_can;
// Bez klucza/podpisu: obraz to zwykly plik .bin. Ochrona = haslo (odblokowanie serwisowe / konto DEV) + kontrola formatu obrazu przez esp_ota_end (naglowek, suma, SHA-256)
// + zgodnosc nazwy projektu z dzialajacym firmware + automatyczny rollback, gdy nowe firmware nie wstanie (HTTP + CAN w ok. 30 s).
static bool same_project(const esp_partition_t* p) { esp_app_desc_t d; const esp_app_desc_t* run = esp_app_get_description(); return run && esp_ota_get_partition_description(p, &d) == ESP_OK && !strncmp(d.project_name, run->project_name, sizeof d.project_name); }
static esp_err_t fail(httpd_req_t* r, const char* st, const char* code) { httpd_resp_set_status(r, st); httpd_resp_set_type(r, "application/json"); char b[80]; snprintf(b, sizeof b, "{\"ok\":false,\"code\":\"%s\"}", code); return httpd_resp_sendstr(r, b); }
esp_err_t handle_fw(httpd_req_t* r) {
  size_t total = r->content_len; if (total < 100000) return fail(r, "400 Bad Request", "bad_stream");
  const esp_partition_t* p = esp_ota_get_next_update_partition(nullptr); esp_ota_handle_t h = 0; if (!p || total > p->size || esp_ota_begin(p, OTA_WITH_SEQUENTIAL_WRITES, &h) != ESP_OK) return fail(r, "500 Internal Server Error", "ota_begin");
  static uint8_t buf[2048]; size_t got = 0;
  while (got < total) { int n = httpd_req_recv(r, (char*)buf, sizeof buf < total - got ? sizeof buf : total - got); if (n <= 0 || esp_ota_write(h, buf, n) != ESP_OK) { esp_ota_abort(h); return fail(r, "400 Bad Request", "bad_stream"); } got += n; }
  if (esp_ota_end(h) != ESP_OK) return fail(r, "422 Unprocessable Entity", "bad_pack");               // zly obraz (naglowek/suma/SHA-256)
  if (!same_project(p)) return fail(r, "422 Unprocessable Entity", "bad_pack");                         // plik z innego projektu
  if (esp_ota_set_boot_partition(p) != ESP_OK) return fail(r, "500 Internal Server Error", "ota_end");
  httpd_resp_set_type(r, "application/json"); httpd_resp_sendstr(r, "{\"ok\":true,\"reboot\":true}"); vTaskDelay(pdMS_TO_TICKS(500)); esp_restart(); return ESP_OK;
}
// ---- kanal BETA: pobieranie przez HTTPS (certyfikaty z wbudowanego pakietu), ten sam podpis ECDSA co przy POST /api/ota/fw
static volatile int g_bstate; static volatile int g_bpct; static char g_bmsg[64]; static volatile bool g_bbusy;
static void bmsg(int st, const char* m) { g_bstate = st; strlcpy(g_bmsg, m, sizeof g_bmsg); }
BetaStatus beta_status() { BetaStatus b; b.state = g_bstate; b.pct = g_bpct; strlcpy(b.msg, g_bmsg, sizeof b.msg); return b; }
void beta_get_url(char* out, size_t cap) { strlcpy(out, cfg::kBetaBaseUrl, cap); nvs_handle_t h; if (nvs_open("dev", NVS_READONLY, &h) == ESP_OK) { size_t n = cap; char t[160]; size_t tn = sizeof t; if (nvs_get_str(h, "beta_url", t, &tn) == ESP_OK && !strncmp(t, "https://", 8)) { strlcpy(out, t, cap); (void)n; } nvs_close(h); } }
bool beta_set_url(const char* url) { if (!url || strncmp(url, "https://", 8) || strlen(url) > 150) return false; for (const char* c = url; *c; c++) if ((unsigned char)*c <= 32 || *c == '"' || *c == '\\') return false;
  nvs_handle_t h; if (nvs_open("dev", NVS_READWRITE, &h) != ESP_OK) return false; bool ok = nvs_set_str(h, "beta_url", url) == ESP_OK && nvs_commit(h) == ESP_OK; nvs_close(h); return ok; }
typedef bool (*DataCb)(void* user, esp_http_client_handle_t cl, const uint8_t* d, int n);
struct HttpCtx { DataCb cb; void* user; bool failed; };
static esp_err_t http_ev(esp_http_client_event_t* e) {
  HttpCtx* c = (HttpCtx*)e->user_data;
  if (e->event_id == HTTP_EVENT_ON_DATA && c && esp_http_client_get_status_code(e->client) == 200) { if (!c->cb(c->user, e->client, (const uint8_t*)e->data, e->data_len)) { c->failed = true; return ESP_FAIL; } }
  return ESP_OK;
}
static int http_get(const char* url, DataCb cb, void* user) {          // zwraca kod HTTP lub -1
  HttpCtx c{cb, user, false}; esp_http_client_config_t hc{}; hc.url = url; hc.event_handler = http_ev; hc.user_data = &c; hc.crt_bundle_attach = esp_crt_bundle_attach; hc.timeout_ms = 20000; hc.buffer_size = 2048; hc.max_redirection_count = 6;
  esp_http_client_handle_t h = esp_http_client_init(&hc); if (!h) return -1; esp_err_t e = esp_http_client_perform(h); int st = esp_http_client_get_status_code(h); esp_http_client_cleanup(h); return (e == ESP_OK && !c.failed) ? st : -1;
}
struct Txt { char b[512]; size_t n; };
static bool txt_cb(void* u, esp_http_client_handle_t, const uint8_t* d, int n) { Txt* t = (Txt*)u; if (t->n + n >= sizeof t->b) return false; memcpy(t->b + t->n, d, n); t->n += n; t->b[t->n] = 0; return true; }
bool beta_check(char* version, size_t vn, char* err, size_t en) {
  char base[160], url[200]; beta_get_url(base, sizeof base); snprintf(url, sizeof url, "%s/beta.json", base); static Txt t; t.n = 0; t.b[0] = 0; int st = http_get(url, txt_cb, &t);
  if (st != 200) { snprintf(err, en, "http_%d", st); return false; } cJSON* j = cJSON_Parse(t.b); const cJSON* v = j ? cJSON_GetObjectItem(j, "version") : nullptr; bool ok = v && cJSON_IsString(v); if (ok) strlcpy(version, v->valuestring, vn); else strlcpy(err, "bad_manifest", en); cJSON_Delete(j); return ok;
}
struct Dl { esp_ota_handle_t h; const esp_partition_t* part; size_t total, got; bool started; };
static bool dl_cb(void* u, esp_http_client_handle_t cl, const uint8_t* d, int n) {
  Dl* x = (Dl*)u;
  if (!x->started) { int64_t tl = esp_http_client_get_content_length(cl); if (tl < 100000) return false; x->total = (size_t)tl; x->part = esp_ota_get_next_update_partition(nullptr); if (!x->part || x->total > x->part->size || esp_ota_begin(x->part, OTA_WITH_SEQUENTIAL_WRITES, &x->h) != ESP_OK) return false; x->started = true; bmsg(2, "pobieranie"); }
  if (esp_ota_write(x->h, d, n) != ESP_OK) return false;
  x->got += n; g_bpct = (int)(x->got * 100 / x->total); return true;
}
static void beta_task(void*) {
  char base[160], url[200]; beta_get_url(base, sizeof base); snprintf(url, sizeof url, "%s/tekla_pellet_beta.bin", base); static Dl x; memset(&x, 0, sizeof x); g_bpct = 0; bmsg(1, "laczenie");
  int st = http_get(url, dl_cb, &x);
  if (st != 200 || !x.started || x.got != x.total) { if (x.started) esp_ota_abort(x.h); char m[64]; snprintf(m, sizeof m, "pobieranie nieudane (http %d)", st); bmsg(5, m); g_bbusy = false; vTaskDelete(nullptr); return; }
  bmsg(3, "kontrola obrazu");
  if (esp_ota_end(x.h) != ESP_OK || !same_project(x.part)) { bmsg(5, "zly obraz - odrzucono"); g_bbusy = false; vTaskDelete(nullptr); return; }
  if (esp_ota_set_boot_partition(x.part) != ESP_OK) { bmsg(5, "zapis slotu nieudany"); g_bbusy = false; vTaskDelete(nullptr); return; }
  bmsg(4, "gotowe, restart"); vTaskDelay(pdMS_TO_TICKS(2000)); esp_restart();
}
bool beta_start() { if (g_bbusy) return false; g_bbusy = true; bmsg(1, "start"); if (xTaskCreate(beta_task, "beta", 8192, nullptr, 4, nullptr) != pdPASS) { g_bbusy = false; return false; } return true; }
void note_healthy_condition(bool http_up, bool can_ok) { g_http = http_up; g_can = can_ok; }
static void healthy_task(void*) {
  vTaskDelay(pdMS_TO_TICKS(cfg::kHealthyMs)); for (int i = 0; i < 18 && !(g_http && g_can); i++) vTaskDelay(pdMS_TO_TICKS(5000));   // do ok. 120 s
  if (g_http && g_can) { esp_ota_mark_app_valid_cancel_rollback(); ESP_LOGI(TAG, "slot FW oznaczony jako poprawny"); } else { ESP_LOGE(TAG, "zdrowy start nieudany - rollback"); esp_ota_mark_app_invalid_rollback_and_reboot(); }
  vTaskDelete(nullptr);
}
void boot_checks() { xTaskCreate(healthy_task, "healthy", 3072, nullptr, 3, nullptr); }
}  // namespace ota
