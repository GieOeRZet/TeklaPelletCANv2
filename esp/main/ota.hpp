#pragma once
#include "esp_http_server.h"
#include "esp_partition.h"
namespace ota {
esp_err_t handle_fw(httpd_req_t* r);               // POST /api/ota/fw: obraz + 64 B podpisu ECDSA P-256 (r||s) na koncu; zapis do nieaktywnego slotu, boot dopiero po weryfikacji podpisu
esp_err_t handle_ui(httpd_req_t* r);               // POST /api/ota/ui: paczka TPUI + 64 B podpisu; zapis do nieaktywnego slotu ui_x
const esp_partition_t* ui_active();                // slot UI z NVS ("ui" = 0/1)
void confirm_ui();                                 // UI zaladowal sie poprawnie (POST /api/ui/confirm)
void boot_checks();                                // rollback slotu UI po 2 nieudanych startach; start zadania "zdrowego startu" (mark_app_valid po 30 s)
void note_healthy_condition(bool http_up, bool can_init_ok);
}
