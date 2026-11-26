#include "ble.hpp"
#include "auth.hpp"
#include "config.hpp"
#include "device.hpp"
#include "net.hpp"
#include "cJSON.h"
#include "esp_log.h"
#include "nvs.h"
#include "esp_system.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "host/ble_hs.h"
#include "host/util/util.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"
#include "store/config/ble_store_config.h"
#include <cstdio>
#include <cstring>
namespace ble {
static const char* TAG = "ble";
// UUID (zapis zwykly): usluga 8f1c0001-5a7e-4c1b-9d2a-7e6b2f0a1001, INFO ...0002 (odczyt), PROV ...0003 (zapis, wymaga szyfrowania lacza)
#define U128(n) BLE_UUID128_INIT(0x01, 0x10, 0x0a, 0x2f, 0x6b, 0x7e, 0x2a, 0x9d, 0x1b, 0x4c, 0x7e, 0x5a, n, 0x00, 0x1c, 0x8f)
static const ble_uuid128_t UUID_SVC = U128(0x01), UUID_INFO = U128(0x02), UUID_PROV = U128(0x03);
static volatile bool g_inited, g_synced, g_want; static uint8_t g_addr_type; static volatile int g_state;   // 0 bezczynny, 1 przetwarzanie, 2 ok (urzadzenie sie zrestartuje), 3 ok (Wi-Fi zmieniane), 4 bledne haslo, 5 zablokowane, 6 zle dane, 7 blad zapisu
static char g_req[420]; static volatile bool g_busy;
static void adv_start();
static int chr_info(uint16_t, uint16_t, struct ble_gatt_access_ctxt* c, void*) {
  if (c->op != BLE_GATT_ACCESS_OP_READ_CHR) return BLE_ATT_ERR_UNLIKELY;
  char b[240]; int n = snprintf(b, sizeof b, "{\"id\":\"%s\",\"name\":\"%s\",\"fw\":\"%s\",\"api\":%d,\"setup_needed\":%s,\"wifi\":%s,\"state\":%d}", device::id(), device::name(), cfg::kVersion, cfg::kApiVersion, auth::configured() ? "false" : "true", net::connected() ? "true" : "false", (int)g_state);
  return os_mbuf_append(c->om, b, n) == 0 ? 0 : BLE_ATT_ERR_INSUFFICIENT_RES;
}
static void prov_task(void*) {   // poza watkiem hosta BLE: hasla przechodza przez PBKDF2 (ok. sekundy)
  cJSON* j = cJSON_Parse(g_req); memset(g_req, 0, sizeof g_req);
  auto s = [&](const char* k) { const cJSON* x = j ? cJSON_GetObjectItem(j, k) : nullptr; return x && cJSON_IsString(x) ? x->valuestring : ""; };
  const char* ssid = s("ssid"); const char* wp = s("wifi_pass"); int st = 6;
  if (j && *ssid && strlen(ssid) <= 32 && strlen(wp) <= 64) {
    if (!auth::configured()) {                                            // pierwsza konfiguracja: haslo urzadzenia, PIN-y i Wi-Fi; potem restart
      if (auth::setup(s("password"), s("pin1"), s("pin2"), s("pin3"))) { st = net::wifi_save(ssid, wp) ? 2 : 7; } else st = 6;
    } else {                                                              // zmiana Wi-Fi na skonfigurowanym module: haslo urzadzenia + cofniecie przy braku IP
      int rc = auth::verify_limited(s("password"), 0xB1E00001u); st = rc == 0 ? (net::wifi_apply(ssid, wp) ? 3 : 7) : (rc == 2 ? 5 : 4);
    }
  }
  cJSON_Delete(j); g_state = st; g_busy = false;
  if (st == 2) { vTaskDelay(pdMS_TO_TICKS(2500)); esp_restart(); }
  vTaskDelete(nullptr);
}
static int chr_prov(uint16_t, uint16_t, struct ble_gatt_access_ctxt* c, void*) {
  if (c->op != BLE_GATT_ACCESS_OP_WRITE_CHR) return BLE_ATT_ERR_UNLIKELY;
  uint16_t len = OS_MBUF_PKTLEN(c->om); if (len == 0 || len >= sizeof g_req) return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN; if (g_busy) return BLE_ATT_ERR_INSUFFICIENT_RES;
  uint16_t out = 0; if (ble_hs_mbuf_to_flat(c->om, g_req, sizeof g_req - 1, &out) != 0) return BLE_ATT_ERR_UNLIKELY; g_req[out] = 0;
  g_busy = true; g_state = 1; if (xTaskCreate(prov_task, "blep", 6144, nullptr, 4, nullptr) != pdPASS) { g_busy = false; g_state = 7; }
  return 0;
}
static const struct ble_gatt_chr_def CHRS[] = {
  { .uuid = &UUID_INFO.u, .access_cb = chr_info, .flags = BLE_GATT_CHR_F_READ },
  { .uuid = &UUID_PROV.u, .access_cb = chr_prov, .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_ENC },
  { 0 } };
static const struct ble_gatt_svc_def SVCS[] = {
  { .type = BLE_GATT_SVC_TYPE_PRIMARY, .uuid = &UUID_SVC.u, .characteristics = CHRS },
  { 0 } };
static int gap_event(struct ble_gap_event* e, void*) {
  switch (e->type) {
    case BLE_GAP_EVENT_DISCONNECT: if (g_want) adv_start(); break;
    case BLE_GAP_EVENT_ADV_COMPLETE: if (g_want) adv_start(); break;
    case BLE_GAP_EVENT_ENC_CHANGE: ESP_LOGI(TAG, "szyfrowanie lacza: status=%d", e->enc_change.status); break;
    case BLE_GAP_EVENT_MTU: ESP_LOGI(TAG, "MTU=%d", e->mtu.value); break;
    case BLE_GAP_EVENT_REPEAT_PAIRING: {   // nowe parowanie z tym samym urzadzeniem: usun stary wpis i pozwol powtorzyc
      struct ble_gap_conn_desc d; if (ble_gap_conn_find(e->repeat_pairing.conn_handle, &d) == 0) ble_store_util_delete_peer(&d.peer_id_addr); return BLE_GAP_REPEAT_PAIRING_RETRY; }
    default: break;
  }
  return 0;
}
static void adv_start() {
  if (!g_synced) return; ble_gap_adv_stop();
  struct ble_hs_adv_fields f{}; f.flags = BLE_HS_ADV_F_DISC_GEN | BLE_HS_ADV_F_BREDR_UNSUP; f.uuids128 = (ble_uuid128_t*)&UUID_SVC; f.num_uuids128 = 1; f.uuids128_is_complete = 1;
  if (ble_gap_adv_set_fields(&f) != 0) return;
  char nm[24]; snprintf(nm, sizeof nm, "TeklaPellet-%s", device::host() + 12); struct ble_hs_adv_fields r{}; r.name = (uint8_t*)nm; r.name_len = strlen(nm); r.name_is_complete = 1; ble_gap_adv_rsp_set_fields(&r);
  struct ble_gap_adv_params p{}; p.conn_mode = BLE_GAP_CONN_MODE_UND; p.disc_mode = BLE_GAP_DISC_MODE_GEN; p.itvl_min = BLE_GAP_ADV_ITVL_MS(200); p.itvl_max = BLE_GAP_ADV_ITVL_MS(300);
  int rc = ble_gap_adv_start(g_addr_type, nullptr, BLE_HS_FOREVER, &p, gap_event, nullptr); if (rc != 0 && rc != BLE_HS_EALREADY) ESP_LOGW(TAG, "adv_start rc=%d", rc);
}
static void on_sync() { if (ble_hs_id_infer_auto(0, &g_addr_type) != 0) return; g_synced = true; ESP_LOGI(TAG, "BLE gotowe"); if (g_want) adv_start(); }
static void on_reset(int) { g_synced = false; }
static void host_task(void*) { nimble_port_run(); nimble_port_freertos_deinit(); }
static bool init_stack() {
  if (nimble_port_init() != ESP_OK) return false;
  ble_hs_cfg.sync_cb = on_sync; ble_hs_cfg.reset_cb = on_reset; ble_hs_cfg.sm_io_cap = BLE_HS_IO_NO_INPUT_OUTPUT; ble_hs_cfg.sm_bonding = 0; ble_hs_cfg.sm_mitm = 0; ble_hs_cfg.sm_sc = 1;
  ble_svc_gap_init(); ble_svc_gatt_init(); ble_store_config_init(); if (ble_gatts_count_cfg(SVCS) != 0 || ble_gatts_add_svcs(SVCS) != 0) return false;
  char nm[24]; snprintf(nm, sizeof nm, "TeklaPellet-%s", device::host() + 12); ble_svc_gap_device_name_set(nm); ble_att_set_preferred_mtu(256);
  nimble_port_freertos_init(host_task); return true;
}
static void policy_task(void*) {
  for (;;) {
    bool want = !auth::configured() || (net::connected() ? false : net::down_s() >= 180);
    if (net::connected() && net::up_s() < 60 && g_want) want = true;       // po polaczeniu daj aplikacji 60 s na odczyt wyniku
    if (want && !g_inited) { g_inited = init_stack(); if (!g_inited) ESP_LOGE(TAG, "start BLE nieudany"); }
    if (want != g_want) { g_want = want; if (g_inited && g_synced) { if (want) adv_start(); else ble_gap_adv_stop(); } ESP_LOGI(TAG, "reklamowanie BLE: %s", want ? "wlaczone" : "wylaczone"); }
    vTaskDelay(pdMS_TO_TICKS(5000));
  }
}
void start() { xTaskCreate(policy_task, "blepol", 4096, nullptr, 2, nullptr); }
}  // namespace ble
