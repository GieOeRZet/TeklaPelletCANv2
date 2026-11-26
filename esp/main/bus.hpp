#pragma once
#include <cstdint>
#include <cstddef>
#include "tekla/live.hpp"
namespace bus {
bool start();                                              // MCP2515 + zadanie magistrali (kolejka zlecen + poller)
bool read(uint16_t pid, uint32_t* raw);                    // blokujaco (<= kSdoTimeoutMs), zlecenia maja priorytet nad pollerem
bool write(uint16_t pid, uint32_t raw);                    // wywolywac TYLKO po tekla::plan_write (straznik)
void rest_all_sets(uint32_t ttl_ms);                   // GET /api/state?all=1: odpytywanie wszystkich zestawow przez ttl_ms (integracje bez WebSocket)
void set_screen_mask(uint8_t mask);                        // zestaw ekranu (tekla::LiveScreen), bity sumowane przez klientow
size_t changes(tekla::LiveItem* out, size_t cap);          // zmiany od ostatniego wywolania (do /ws)
size_t snapshot(tekla::LiveItem* out, size_t cap);         // wszystkie znane wartosci live
bool can_ok(); uint32_t last_reply_age_ms();
}
