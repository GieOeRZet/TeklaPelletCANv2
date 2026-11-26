#include "net.hpp"
#include "config.hpp"
#include "device.hpp"
#include "esp_wifi.h"
#include "esp_netif.h"
#include "esp_event.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "nvs.h"
#include "mdns.h"
#include "lwip/sockets.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <cstring>
namespace net {
static const char* TAG = "net";
static volatile bool g_up, g_ap; static int g_retry; static volatile int64_t g_down_since, g_up_since; static esp_netif_t* g_sta;
static volatile bool g_ip_pending, g_ip_confirmed; static IpCfg g_ip_prev; static IpCfg g_ip_new; static char g_new_ssid[33], g_new_pass[65]; static volatile bool g_wifi_busy;
static void on_event(void*, esp_event_base_t b, int32_t id, void*) {
  if (b == WIFI_EVENT && id == WIFI_EVENT_STA_START) { g_down_since = esp_timer_get_time(); esp_wifi_connect(); }
  else if (b == WIFI_EVENT && id == WIFI_EVENT_STA_DISCONNECTED) { g_up = false; if (!g_down_since) g_down_since = esp_timer_get_time(); if (g_retry++ < 100000) esp_wifi_connect(); }
  else if (b == IP_EVENT && id == IP_EVENT_STA_GOT_IP) { g_up = true; g_retry = 0; g_down_since = 0; g_up_since = esp_timer_get_time(); }
}
bool connected() { return g_up; }
int64_t down_s() { return g_up || !g_down_since ? 0 : (esp_timer_get_time() - g_down_since) / 1000000; }
int64_t up_s() { return g_up && g_up_since ? (esp_timer_get_time() - g_up_since) / 1000000 : 0; }
// ---- zapis/odczyt ustawien IP (NVS "ip")
static IpCfg ip_load() { IpCfg c; nvs_handle_t h; if (nvs_open("ip", NVS_READONLY, &h) == ESP_OK) { uint8_t d = 1; nvs_get_u8(h, "dhcp", &d); c.dhcp = d != 0; nvs_get_u32(h, "ip", &c.ip); nvs_get_u32(h, "mask", &c.mask); nvs_get_u32(h, "gw", &c.gw); nvs_get_u32(h, "dns1", &c.dns1); nvs_get_u32(h, "dns2", &c.dns2); nvs_close(h); } return c; }
static void ip_save(const IpCfg& c) { nvs_handle_t h; if (nvs_open("ip", NVS_READWRITE, &h) != ESP_OK) return; nvs_set_u8(h, "dhcp", c.dhcp); nvs_set_u32(h, "ip", c.ip); nvs_set_u32(h, "mask", c.mask); nvs_set_u32(h, "gw", c.gw); nvs_set_u32(h, "dns1", c.dns1); nvs_set_u32(h, "dns2", c.dns2); nvs_commit(h); nvs_close(h); }
static void ip_set(const IpCfg& c) {
  if (!g_sta) return;
  if (c.dhcp) { esp_netif_dhcpc_start(g_sta); return; }
  esp_netif_dhcpc_stop(g_sta); esp_netif_ip_info_t i{}; i.ip.addr = htonl(c.ip); i.netmask.addr = htonl(c.mask); i.gw.addr = htonl(c.gw); esp_netif_set_ip_info(g_sta, &i);
  esp_netif_dns_info_t d{}; d.ip.type = ESP_IPADDR_TYPE_V4; d.ip.u_addr.ip4.addr = htonl(c.dns1 ? c.dns1 : c.gw); esp_netif_set_dns_info(g_sta, ESP_NETIF_DNS_MAIN, &d);
  if (c.dns2) { d.ip.u_addr.ip4.addr = htonl(c.dns2); esp_netif_set_dns_info(g_sta, ESP_NETIF_DNS_BACKUP, &d); }
}
void ip_get(IpCfg* saved, IpCfg* cur, bool* pending) {
  if (saved) *saved = ip_load(); if (pending) *pending = g_ip_pending;
  if (cur) { *cur = IpCfg(); esp_netif_ip_info_t i; if (g_sta && esp_netif_get_ip_info(g_sta, &i) == ESP_OK) { cur->ip = ntohl(i.ip.addr); cur->mask = ntohl(i.netmask.addr); cur->gw = ntohl(i.gw.addr); }
    esp_netif_dns_info_t d; if (g_sta && esp_netif_get_dns_info(g_sta, ESP_NETIF_DNS_MAIN, &d) == ESP_OK) cur->dns1 = ntohl(d.ip.u_addr.ip4.addr); if (g_sta && esp_netif_get_dns_info(g_sta, ESP_NETIF_DNS_BACKUP, &d) == ESP_OK) cur->dns2 = ntohl(d.ip.u_addr.ip4.addr); cur->dhcp = ip_load().dhcp; }
}
static void ip_task(void*) {
  vTaskDelay(pdMS_TO_TICKS(800)); ip_set(g_ip_new); g_ip_confirmed = false;
  for (int i = 0; i < 120 && !g_ip_confirmed; i++) vTaskDelay(pdMS_TO_TICKS(500));                       // 60 s na potwierdzenie z nowego adresu
  if (g_ip_confirmed) { ip_save(g_ip_new); ESP_LOGI(TAG, "ustawienia IP potwierdzone"); } else { ip_set(g_ip_prev); ESP_LOGW(TAG, "brak potwierdzenia IP - powrot do poprzednich ustawien"); }
  g_ip_pending = false; vTaskDelete(nullptr);
}
bool ip_apply(const IpCfg& c) { if (g_ip_pending) return false; g_ip_prev = ip_load(); g_ip_new = c; g_ip_pending = true; if (xTaskCreate(ip_task, "ipcfg", 4096, nullptr, 4, nullptr) != pdPASS) { g_ip_pending = false; return false; } return true; }
void ip_confirm() { if (g_ip_pending) g_ip_confirmed = true; }
// ---- Wi-Fi: zmiana sieci z cofnieciem
static bool wifi_load(char* ssid, size_t sn, char* pass, size_t pn) { nvs_handle_t h; bool ok = false; if (nvs_open("wifi", NVS_READONLY, &h) == ESP_OK) { ok = nvs_get_str(h, "ssid", ssid, &sn) == ESP_OK && nvs_get_str(h, "pass", pass, &pn) == ESP_OK; nvs_close(h); } return ok; }
static void wifi_store(const char* ssid, const char* pass) { nvs_handle_t h; if (nvs_open("wifi", NVS_READWRITE, &h) == ESP_OK) { nvs_set_str(h, "ssid", ssid); nvs_set_str(h, "pass", pass); nvs_commit(h); nvs_close(h); } }
bool wifi_save(const char* ssid, const char* pass) { if (!ssid || !*ssid || strlen(ssid) > 32 || !pass || strlen(pass) > 64) return false; wifi_store(ssid, pass); char a[33] = {}, b[65] = {}; return wifi_load(a, sizeof a, b, sizeof b) && !strcmp(a, ssid); }
static void sta_config(const char* ssid, const char* pass) { wifi_config_t c{}; strlcpy((char*)c.sta.ssid, ssid, sizeof c.sta.ssid); strlcpy((char*)c.sta.password, pass, sizeof c.sta.password); esp_wifi_set_config(WIFI_IF_STA, &c); }
static void wifi_task(void*) {
  vTaskDelay(pdMS_TO_TICKS(800)); char os[33] = {}, op[65] = {}; bool had = wifi_load(os, sizeof os, op, sizeof op);
  wifi_mode_t m; esp_wifi_get_mode(&m); if (m == WIFI_MODE_AP) esp_wifi_set_mode(WIFI_MODE_APSTA);
  g_up = false; esp_wifi_disconnect(); sta_config(g_new_ssid, g_new_pass); esp_wifi_connect();
  for (int i = 0; i < 80 && !g_up; i++) vTaskDelay(pdMS_TO_TICKS(500));                                   // 40 s na IP
  if (g_up) { wifi_store(g_new_ssid, g_new_pass); ESP_LOGI(TAG, "nowa siec Wi-Fi zapisana"); }
  else if (had) { ESP_LOGW(TAG, "brak IP w nowej sieci - powrot do poprzedniej"); esp_wifi_disconnect(); sta_config(os, op); esp_wifi_connect(); }
  g_wifi_busy = false; vTaskDelete(nullptr);
}
bool wifi_apply(const char* ssid, const char* pass) {
  if (g_wifi_busy || !ssid || !*ssid || strlen(ssid) > 32 || !pass || strlen(pass) > 64) return false; strlcpy(g_new_ssid, ssid, sizeof g_new_ssid); strlcpy(g_new_pass, pass, sizeof g_new_pass);
  g_wifi_busy = true; if (xTaskCreate(wifi_task, "wifi", 4096, nullptr, 4, nullptr) != pdPASS) { g_wifi_busy = false; return false; } return true;
}
// ---- awaryjny AP: gdy po konfiguracji Wi-Fi znika > 3 min, otwiera sie AP (tylko strona serwisowa, operacje wymagaja hasla); gasnie po powrocie sieci
static void ap_config() { wifi_config_t c{}; snprintf((char*)c.ap.ssid, sizeof c.ap.ssid, "TeklaPellet-%s", device::host() + 12); c.ap.ssid_len = strlen((char*)c.ap.ssid); c.ap.max_connection = 2; c.ap.authmode = WIFI_AUTH_OPEN; esp_wifi_set_config(WIFI_IF_AP, &c); }
static void recovery_task(void*) {
  for (;;) {
    vTaskDelay(pdMS_TO_TICKS(10000)); int64_t now = esp_timer_get_time();
    if (!g_up && g_down_since && !g_ap && now - g_down_since > 180LL * 1000000) { ap_config(); esp_wifi_set_mode(WIFI_MODE_APSTA); g_ap = true; ESP_LOGW(TAG, "brak Wi-Fi > 3 min: awaryjny AP wlaczony"); }
    if (g_up && g_ap && now - g_up_since > 30LL * 1000000) { esp_wifi_set_mode(WIFI_MODE_STA); g_ap = false; ESP_LOGI(TAG, "Wi-Fi wrocilo: awaryjny AP wylaczony"); }
  }
}
void refresh_identity() {
  mdns_instance_name_set(device::name()); char node[8]; snprintf(node, sizeof node, "%d", cfg::kNodeId); char api[8]; snprintf(api, sizeof api, "%d", cfg::kApiVersion);
  mdns_service_txt_item_set("_teklapellet", "_tcp", "id", device::id()); mdns_service_txt_item_set("_teklapellet", "_tcp", "name", device::name()); mdns_service_txt_item_set("_teklapellet", "_tcp", "fw", cfg::kVersion);
  mdns_service_txt_item_set("_teklapellet", "_tcp", "api", api); mdns_service_txt_item_set("_teklapellet", "_tcp", "node", node);
}
void start() {
  esp_netif_init(); esp_event_loop_create_default(); g_sta = esp_netif_create_default_wifi_sta(); esp_netif_create_default_wifi_ap();
  esp_netif_set_hostname(g_sta, device::host());
  wifi_init_config_t ic = WIFI_INIT_CONFIG_DEFAULT(); esp_wifi_init(&ic);
  esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, on_event, nullptr); esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, on_event, nullptr);
  char ssid[33] = {}, pass[65] = {}; bool have = wifi_load(ssid, sizeof ssid, pass, sizeof pass);
  IpCfg ic4 = ip_load(); if (!ic4.dhcp && have) ip_set(ic4);                                              // staly adres zapisany wczesniej
  if (have) { esp_wifi_set_mode(WIFI_MODE_STA); sta_config(ssid, pass); }
  else { ap_config(); esp_wifi_set_mode(WIFI_MODE_AP); g_ap = true; }                                    // AP otwarty tylko do pierwszej konfiguracji
  esp_wifi_start(); mdns_init(); mdns_hostname_set(device::host()); mdns_service_add(device::name(), "_teklapellet", "_tcp", 80, nullptr, 0); refresh_identity();
  xTaskCreate(recovery_task, "wrec", 3072, nullptr, 2, nullptr);
}
size_t scan_json(char* out, size_t cap) {   // blokujacy skan (kilka sekund); {"ok":true,"networks":[{"ssid":..,"rssi":..}]}
  wifi_mode_t m; esp_wifi_get_mode(&m); if (m == WIFI_MODE_AP) esp_wifi_set_mode(WIFI_MODE_APSTA);
  wifi_scan_config_t sc{}; size_t k = snprintf(out, cap, "{\"ok\":true,\"networks\":[");
  if (esp_wifi_scan_start(&sc, true) == ESP_OK) { uint16_t n = 12; wifi_ap_record_t r[12]; esp_wifi_scan_get_ap_records(&n, r);
    for (uint16_t i = 0; i < n && k + 80 < cap; i++) { char s[40]; size_t o = 0; for (const char* p = (const char*)r[i].ssid; *p && o < sizeof s - 1; p++) if (*p != '"' && *p != '\\' && (unsigned char)*p >= 32) s[o++] = *p; s[o] = 0; k += snprintf(out + k, cap - k, "%s{\"ssid\":\"%s\",\"rssi\":%d}", i ? "," : "", s, r[i].rssi); } }
  k += snprintf(out + k, cap - k, "]}"); return k;
}
void current_ssid(char* out, size_t cap) { out[0] = 0; nvs_handle_t h; if (nvs_open("wifi", NVS_READONLY, &h) == ESP_OK) { size_t n = cap; if (nvs_get_str(h, "ssid", out, &n) != ESP_OK) out[0] = 0; nvs_close(h); } }
}  // namespace net
