#include "net.hpp"
#include "esp_wifi.h"
#include "esp_netif.h"
#include "esp_event.h"
#include "esp_log.h"
#include "esp_mac.h"
#include "nvs.h"
#include "mdns.h"
#include <cstring>
namespace net {
static bool g_up; static int g_retry;
static void on_event(void*, esp_event_base_t b, int32_t id, void*) {
  if (b == WIFI_EVENT && id == WIFI_EVENT_STA_START) esp_wifi_connect();
  else if (b == WIFI_EVENT && id == WIFI_EVENT_STA_DISCONNECTED) { g_up = false; if (g_retry++ < 1000) esp_wifi_connect(); }
  else if (b == IP_EVENT && id == IP_EVENT_STA_GOT_IP) { g_up = true; g_retry = 0; }
}
bool connected() { return g_up; }
void start() {
  esp_netif_init(); esp_event_loop_create_default(); esp_netif_create_default_wifi_sta(); esp_netif_t* ap = esp_netif_create_default_wifi_ap(); (void)ap;
  wifi_init_config_t ic = WIFI_INIT_CONFIG_DEFAULT(); esp_wifi_init(&ic);
  esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, on_event, nullptr); esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, on_event, nullptr);
  char ssid[33] = {}, pass[65] = {}; size_t a = sizeof ssid, b = sizeof pass; nvs_handle_t h; bool have = false;
  if (nvs_open("wifi", NVS_READONLY, &h) == ESP_OK) { have = nvs_get_str(h, "ssid", ssid, &a) == ESP_OK && nvs_get_str(h, "pass", pass, &b) == ESP_OK; nvs_close(h); }
  uint8_t mac[6]; esp_read_mac(mac, ESP_MAC_WIFI_STA); char host[24]; snprintf(host, sizeof host, "teklapellet-%02x%02x", mac[4], mac[5]);
  if (have) { wifi_config_t c{}; strlcpy((char*)c.sta.ssid, ssid, sizeof c.sta.ssid); strlcpy((char*)c.sta.password, pass, sizeof c.sta.password); esp_wifi_set_mode(WIFI_MODE_STA); esp_wifi_set_config(WIFI_IF_STA, &c); }
  else { wifi_config_t c{}; snprintf((char*)c.ap.ssid, sizeof c.ap.ssid, "TeklaPellet-%02X%02X", mac[4], mac[5]); c.ap.ssid_len = strlen((char*)c.ap.ssid); c.ap.max_connection = 2; c.ap.authmode = WIFI_AUTH_OPEN; esp_wifi_set_mode(WIFI_MODE_AP); esp_wifi_set_config(WIFI_IF_AP, &c); }   // AP otwarty tylko do pierwszej konfiguracji
  esp_wifi_start(); mdns_init(); mdns_hostname_set(host); mdns_instance_name_set(host); mdns_service_add(nullptr, "_teklapellet", "_tcp", 80, nullptr, 0);
}
}  // namespace net
