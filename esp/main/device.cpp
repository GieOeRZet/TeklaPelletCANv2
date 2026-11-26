#include "device.hpp"
#include "tekla/netcfg.hpp"
#include "esp_mac.h"
#include "nvs.h"
#include <cstring>
namespace device {
static char g_id[24], g_host[24], g_name[32]; static bool g_init;
static void init() {
  if (g_init) return; uint8_t mac[6] = {}; esp_read_mac(mac, ESP_MAC_WIFI_STA); tekla::device_id(mac, g_id, sizeof g_id); tekla::device_host(mac, g_host, sizeof g_host);
  strcpy(g_name, "TeklaPellet"); nvs_handle_t h; if (nvs_open("dev", NVS_READONLY, &h) == ESP_OK) { size_t n = sizeof g_name; char tmp[32]; if (nvs_get_str(h, "name", tmp, &n) == ESP_OK && tekla::name_ok(tmp)) strcpy(g_name, tmp); nvs_close(h); }
  g_init = true;
}
const char* id() { init(); return g_id; }
const char* host() { init(); return g_host; }
const char* name() { init(); return g_name; }
bool set_name(const char* n) {
  init(); if (!tekla::name_ok(n)) return false; nvs_handle_t h; if (nvs_open("dev", NVS_READWRITE, &h) != ESP_OK) return false;
  bool ok = nvs_set_str(h, "name", n) == ESP_OK && nvs_commit(h) == ESP_OK; nvs_close(h); if (ok) strcpy(g_name, n); return ok;
}
}  // namespace device
