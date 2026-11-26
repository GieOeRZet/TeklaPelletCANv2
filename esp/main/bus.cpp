#include "bus.hpp"
#include "config.hpp"
#include "mcp2515.hpp"
#include "tekla/model.hpp"
#include "tekla/identity.hpp"
#include "driver/gpio.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/semphr.h"
#include "esp_timer.h"
#include "esp_log.h"
using namespace tekla;
namespace bus {
static const char* TAG = "bus";
struct Req { bool write; uint16_t pid; uint32_t raw; uint32_t* out; bool* ok; SemaphoreHandle_t done; };
static QueueHandle_t q; static tekla::IdentityResponder g_ident(cfg::kNodeId, cfg::kName, cfg::kVersion, cfg::kProduct); static bool g_booted; static int64_t g_hb_us;
static bool g_can_ok; static int64_t g_last_reply_us; static volatile uint8_t g_mask = LS_BASE;
static volatile int64_t g_rest_until; static uint8_t eff_mask(int64_t now) { return g_mask | (now < g_rest_until ? 0xFE : 0); }
static double g_val[kLiveCount]; static bool g_known[kLiveCount], g_dirty[kLiveCount]; static int64_t g_next_us[kLiveCount]; static SemaphoreHandle_t g_mu;
static bool transact(const CanFrame& tx, uint16_t pid, uint32_t* raw) {                // wyslij, czekaj na 0x580+node z tym samym indeksem
  if (!mcp::send(tx, 50)) return false; int64_t end = esp_timer_get_time() + (int64_t)cfg::kSdoTimeoutMs * 1000;
  while (esp_timer_get_time() < end) { CanFrame f; if (mcp::recv(&f)) { if (f.id == 0x600u + cfg::kNodeId) { CanFrame o; if (g_ident.handle(f, &o)) mcp::send(o, 20); continue; } SdoReply r = sdo_parse_reply(cfg::kBoilerNode, f); if (r.ok && r.index == pid) { g_last_reply_us = esp_timer_get_time(); g_can_ok = true; if (raw) *raw = r.value; return true; } if (r.abort && r.index == pid) return false; } else vTaskDelay(1); }
  g_can_ok = esp_timer_get_time() - g_last_reply_us < 5000000; return false;
}
static void service() {                                      // tozsamosc wezla: odpowiedzi SDO na zapytania panelu + boot-up/heartbeat NMT
  CanFrame f, o; while (mcp::recv(&f)) if (f.id == 0x600u + cfg::kNodeId && g_ident.handle(f, &o)) mcp::send(o, 20);
  int64_t now = esp_timer_get_time(); if (now - g_hb_us >= (int64_t)cfg::kHeartbeatMs * 1000) { g_hb_us = now; mcp::send(g_booted ? tekla::nmt_heartbeat_frame(cfg::kNodeId) : tekla::nmt_bootup_frame(cfg::kNodeId), 20); g_booted = true; }
}
static void task(void*) {
  for (;;) {
    service();
    Req r; if (xQueueReceive(q, &r, 0) == pdTRUE) {
      uint32_t v = 0; bool ok = r.write ? transact(sdo_write_frame(cfg::kBoilerNode, r.pid, r.raw), r.pid, &v) : transact(sdo_read_frame(cfg::kBoilerNode, r.pid), r.pid, &v);
      if (r.out) *r.out = v; if (r.ok) *r.ok = ok; xSemaphoreGive(r.done); continue;
    }
    int64_t now = esp_timer_get_time(); bool did = false;
    for (size_t i = 0; i < kLiveCount && !did; i++) {
      const LiveDef& d = kLive[i]; if (!((d.sets & LS_BASE) || (d.sets & eff_mask(now))) || now < g_next_us[i]) continue;
      uint32_t raw; g_next_us[i] = now + (int64_t)d.period_ms * 1000; did = true;
      if (transact(sdo_read_frame(cfg::kBoilerNode, d.pid), d.pid, &raw)) { double v = live_value(d.kind, raw); xSemaphoreTake(g_mu, portMAX_DELAY); if (!g_known[i] || g_val[i] != v) { g_val[i] = v; g_dirty[i] = true; } g_known[i] = true; xSemaphoreGive(g_mu); }
    }
    if (!did) vTaskDelay(pdMS_TO_TICKS(5));
  }
}
bool start() {
  if (cfg::kExternalAntenna) { gpio_config_t g{}; g.pin_bit_mask = (1ULL << cfg::kPinRfSwitch) | (1ULL << cfg::kPinRfAntenna); g.mode = GPIO_MODE_OUTPUT; gpio_config(&g); gpio_set_level((gpio_num_t)cfg::kPinRfSwitch, 0); gpio_set_level((gpio_num_t)cfg::kPinRfAntenna, 1); }   // jak PelletCAN (external_antenna)
  mcp::Pins p{cfg::kPinSck, cfg::kPinMiso, cfg::kPinMosi, cfg::kPinCs, cfg::kPinInt, cfg::kSpiHz};
  if (!mcp::init(p, cfg::kOsc16MHz ? mcp::Osc::MHz16 : mcp::Osc::MHz8)) { ESP_LOGE(TAG, "MCP2515 init nieudany"); return false; }
  q = xQueueCreate(16, sizeof(Req)); g_mu = xSemaphoreCreateMutex(); xTaskCreate(task, "bus", 4096, nullptr, 5, nullptr); return true;
}
static bool submit(bool wr, uint16_t pid, uint32_t raw, uint32_t* out) {
  Req r{wr, pid, raw, out, nullptr, xSemaphoreCreateBinary()}; bool ok = false; r.ok = &ok;
  if (xQueueSend(q, &r, pdMS_TO_TICKS(200)) != pdTRUE) { vSemaphoreDelete(r.done); return false; }
  xSemaphoreTake(r.done, portMAX_DELAY); vSemaphoreDelete(r.done); return ok;     // zadanie magistrali ZAWSZE oddaje semafor (timeout transakcji = ok=false), wskazniki na stos pozostaja wazne
}
bool read(uint16_t pid, uint32_t* raw) { return submit(false, pid, 0, raw); }
bool write(uint16_t pid, uint32_t raw) { return submit(true, pid, raw, nullptr); }
void set_screen_mask(uint8_t m) { g_mask = m | LS_BASE; }
void rest_all_sets(uint32_t ttl_ms) { g_rest_until = esp_timer_get_time() + (int64_t)ttl_ms * 1000; }
size_t changes(LiveItem* out, size_t cap) { size_t n = 0; xSemaphoreTake(g_mu, portMAX_DELAY); for (size_t i = 0; i < kLiveCount && n < cap; i++) if (g_dirty[i]) { out[n++] = {kLive[i].pid, g_val[i]}; g_dirty[i] = false; } xSemaphoreGive(g_mu); return n; }
size_t snapshot(LiveItem* out, size_t cap) { size_t n = 0; xSemaphoreTake(g_mu, portMAX_DELAY); for (size_t i = 0; i < kLiveCount && n < cap; i++) if (g_known[i]) out[n++] = {kLive[i].pid, g_val[i]}; xSemaphoreGive(g_mu); return n; }
bool can_ok() { return g_can_ok; } uint32_t last_reply_age_ms() { return g_last_reply_us ? (uint32_t)((esp_timer_get_time() - g_last_reply_us) / 1000) : 0xFFFFFFFFu; }
}  // namespace bus
