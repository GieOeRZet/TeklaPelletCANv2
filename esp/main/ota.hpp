#pragma once
#include "esp_http_server.h"
namespace ota {
esp_err_t handle_fw(httpd_req_t* r);               // POST /api/ota/fw: obraz + 64 B podpisu ECDSA P-256 (r||s) na koncu; zapis do nieaktywnego slotu, boot dopiero po weryfikacji podpisu
struct BetaStatus { int state; int pct; char msg[64]; };   // state: 0 bezczynny, 1 sprawdzanie, 2 pobieranie, 3 weryfikacja, 4 gotowe (restart), 5 blad
bool beta_check(char* version, size_t vn, char* err, size_t en);     // GET <base>/beta.json -> "version"
bool beta_start();                                                   // zadanie: pobierz <base>/tekla_pellet_beta.bin (obraz + 64 B podpisu), zweryfikuj podpis, zapisz do slotu, restart
BetaStatus beta_status();
bool beta_set_url(const char* url); void beta_get_url(char* out, size_t cap);
void boot_checks();                                // start zadania "zdrowego startu" (mark_app_valid po 30 s, gdy HTTP i CAN dzialaja; inaczej rollback)
void note_healthy_condition(bool http_up, bool can_init_ok);
}
