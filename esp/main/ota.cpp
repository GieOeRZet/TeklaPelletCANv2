#include "ota.hpp"
#include "config.hpp"
#include "tekla/ecdsa_der.hpp"
#include "tekla/ui_pack.hpp"
#include "esp_ota_ops.h"
#include "esp_log.h"
#include "nvs.h"
#include "mbedtls/pk.h"
#include "mbedtls/sha256.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <cstring>
extern const char pubkey_start[] asm("_binary_ota_pubkey_pem_start"); extern const char pubkey_end[] asm("_binary_ota_pubkey_pem_end");
namespace ota {
static const char* TAG = "ota"; static volatile bool g_http, g_can;
static bool verify(const uint8_t hash[32], const uint8_t rs[64]) {
  mbedtls_pk_context pk; mbedtls_pk_init(&pk); bool ok = false;
  if (mbedtls_pk_parse_public_key(&pk, (const unsigned char*)pubkey_start, pubkey_end - pubkey_start) == 0) { uint8_t der[80]; size_t n = tekla::sig_rs_to_der(rs, der, sizeof der); ok = n && mbedtls_pk_verify(&pk, MBEDTLS_MD_SHA256, hash, 32, der, n) == 0; }
  mbedtls_pk_free(&pk); return ok;                                   // brak/niepoprawny klucz publiczny = zawsze odrzucenie
}
static esp_err_t fail(httpd_req_t* r, const char* st, const char* code) { httpd_resp_set_status(r, st); httpd_resp_set_type(r, "application/json"); char b[80]; snprintf(b, sizeof b, "{\"ok\":false,\"code\":\"%s\"}", code); return httpd_resp_sendstr(r, b); }
// Wspolna petla: strumieniowo czyta cialo, pierwsze len-64 B -> sink(), liczy SHA-256, ostatnie 64 B = podpis.
template <typename Sink> static int stream(httpd_req_t* r, Sink sink, uint8_t hash[32], uint8_t sig[64]) {
  size_t total = r->content_len; if (total <= 64) return -1; size_t body = total - 64, got = 0; mbedtls_sha256_context c; mbedtls_sha256_init(&c); mbedtls_sha256_starts(&c, 0);
  static uint8_t buf[2048]; size_t sigf = 0;
  while (got < total) {
    int n = httpd_req_recv(r, (char*)buf, sizeof buf < total - got ? sizeof buf : total - got); if (n <= 0) { mbedtls_sha256_free(&c); return -2; }
    size_t imgn = got < body ? (body - got < (size_t)n ? body - got : n) : 0;
    if (imgn) { if (!sink(buf, imgn, got)) { mbedtls_sha256_free(&c); return -3; } mbedtls_sha256_update(&c, buf, imgn); }
    if ((size_t)n > imgn) { memcpy(sig + sigf, buf + imgn, n - imgn); sigf += n - imgn; } got += n;
  }
  mbedtls_sha256_finish(&c, hash); mbedtls_sha256_free(&c); return sigf == 64 ? 0 : -4;
}
esp_err_t handle_fw(httpd_req_t* r) {
  const esp_partition_t* p = esp_ota_get_next_update_partition(nullptr); esp_ota_handle_t h = 0; if (!p || esp_ota_begin(p, OTA_WITH_SEQUENTIAL_WRITES, &h) != ESP_OK) return fail(r, "500 Internal Server Error", "ota_begin");
  uint8_t hash[32], sig[64]; int rc = stream(r, [&](const uint8_t* d, size_t n, size_t) { return esp_ota_write(h, d, n) == ESP_OK; }, hash, sig);
  if (rc != 0) { esp_ota_abort(h); return fail(r, "400 Bad Request", "bad_stream"); }
  if (!verify(hash, sig)) { esp_ota_abort(h); return fail(r, "422 Unprocessable Entity", "bad_signature"); }
  if (esp_ota_end(h) != ESP_OK || esp_ota_set_boot_partition(p) != ESP_OK) return fail(r, "500 Internal Server Error", "ota_end");
  httpd_resp_set_type(r, "application/json"); httpd_resp_sendstr(r, "{\"ok\":true,\"reboot\":true}"); vTaskDelay(pdMS_TO_TICKS(500)); esp_restart(); return ESP_OK;
}
static int nvs_ui() { nvs_handle_t h; uint8_t v = 0; if (nvs_open("ota", NVS_READONLY, &h) == ESP_OK) { nvs_get_u8(h, "ui", &v); nvs_close(h); } return v & 1; }
const esp_partition_t* ui_active() { return esp_partition_find_first(ESP_PARTITION_TYPE_DATA, (esp_partition_subtype_t)(0x40 + nvs_ui()), nullptr); }
esp_err_t handle_ui(httpd_req_t* r) {
  const esp_partition_t* t = esp_partition_find_first(ESP_PARTITION_TYPE_DATA, (esp_partition_subtype_t)(0x40 + (1 - nvs_ui())), nullptr); if (!t) return fail(r, "500 Internal Server Error", "no_partition");
  if (r->content_len - 64 > t->size) return fail(r, "413 Payload Too Large", "too_big");
  if (esp_partition_erase_range(t, 0, t->size) != ESP_OK) return fail(r, "500 Internal Server Error", "erase");
  uint8_t hash[32], sig[64]; int rc = stream(r, [&](const uint8_t* d, size_t n, size_t off) { return esp_partition_write(t, off, d, n) == ESP_OK; }, hash, sig);
  if (rc != 0) return fail(r, "400 Bad Request", "bad_stream"); if (!verify(hash, sig)) return fail(r, "422 Unprocessable Entity", "bad_signature");
  const void* mp; esp_partition_mmap_handle_t mh; if (esp_partition_mmap(t, 0, t->size, ESP_PARTITION_MMAP_DATA, &mp, &mh) != ESP_OK) return fail(r, "500 Internal Server Error", "mmap");
  bool good = tekla::ui_verify((const uint8_t*)mp, t->size); esp_partition_munmap(mh); if (!good) return fail(r, "422 Unprocessable Entity", "bad_pack");
  nvs_handle_t h; if (nvs_open("ota", NVS_READWRITE, &h) != ESP_OK) return fail(r, "500 Internal Server Error", "nvs");
  nvs_set_u8(h, "ui_prev", nvs_ui()); nvs_set_u8(h, "ui", 1 - nvs_ui()); nvs_set_u8(h, "ui_pend", 1); nvs_set_u8(h, "ui_try", 0); nvs_commit(h); nvs_close(h);
  httpd_resp_set_type(r, "application/json"); return httpd_resp_sendstr(r, "{\"ok\":true,\"confirm_required\":true}");
}
void confirm_ui() { nvs_handle_t h; if (nvs_open("ota", NVS_READWRITE, &h) == ESP_OK) { nvs_set_u8(h, "ui_pend", 0); nvs_commit(h); nvs_close(h); } }
void note_healthy_condition(bool http_up, bool can_ok) { g_http = http_up; g_can = can_ok; }
static void healthy_task(void*) {
  vTaskDelay(pdMS_TO_TICKS(cfg::kHealthyMs)); for (int i = 0; i < 18 && !(g_http && g_can); i++) vTaskDelay(pdMS_TO_TICKS(5000));   // do ok. 120 s
  if (g_http && g_can) { esp_ota_mark_app_valid_cancel_rollback(); ESP_LOGI(TAG, "slot FW oznaczony jako poprawny"); } else { ESP_LOGE(TAG, "zdrowy start nieudany - rollback"); esp_ota_mark_app_invalid_rollback_and_reboot(); }
  vTaskDelete(nullptr);
}
void boot_checks() {
  nvs_handle_t h; if (nvs_open("ota", NVS_READWRITE, &h) == ESP_OK) { uint8_t pend = 0, tr = 0, prev = 0; nvs_get_u8(h, "ui_pend", &pend); nvs_get_u8(h, "ui_try", &tr); nvs_get_u8(h, "ui_prev", &prev);
    if (pend) { if (tr >= 2) { nvs_set_u8(h, "ui", prev); nvs_set_u8(h, "ui_pend", 0); ESP_LOGW(TAG, "UI niepotwierdzone - powrot do poprzedniego slotu"); } else nvs_set_u8(h, "ui_try", tr + 1); } nvs_commit(h); nvs_close(h); }
  xTaskCreate(healthy_task, "healthy", 3072, nullptr, 3, nullptr);
}
}  // namespace ota
