#include "auth.hpp"
#include "bus.hpp"
#include "net.hpp"
#include "ota.hpp"
#include "web.hpp"
#include "nvs_flash.h"
#include "esp_log.h"
extern "C" void app_main() {
  esp_err_t e = nvs_flash_init(); if (e == ESP_ERR_NVS_NO_FREE_PAGES || e == ESP_ERR_NVS_NEW_VERSION_FOUND) { nvs_flash_erase(); nvs_flash_init(); }
  ota::boot_checks();                                   // rollback slotu UI + zadanie "zdrowego startu" (FW oznaczony poprawnym po 30 s, gdy HTTP i CAN dzialaja)
  net::start();
  bool can = bus::start(); bool http = web::start();
  ota::note_healthy_condition(http, can);
  ESP_LOGI("main", "start: CAN=%d HTTP=%d skonfigurowane=%d (tryb domyslny: tylko odczyt)", can, http, auth::configured());
}
