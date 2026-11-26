// Stale konfiguracyjne firmware (docs/ESP_API_V2.md sekcje 4-13). Zmiana bez przebudowy architektury.
#pragma once
#include <cstdint>
namespace cfg {
constexpr uint8_t kNodeId = 95;            // wezel ESP na CAN (wlasciciel: 95); konfigurowalny w NVS "node"
constexpr uint8_t kBoilerNode = 4;         // sterownik kotla
// XIAO ESP32-C6 + MCP2515 (do potwierdzenia z okablowaniem): SCK=D8/GPIO19, MISO=D9/GPIO20, MOSI=D10/GPIO18, CS=D1/GPIO1, INT=D2/GPIO2
constexpr int kPinSck = 19, kPinMiso = 20, kPinMosi = 18, kPinCs = 1, kPinInt = 2;
constexpr bool kOsc16MHz = false;          // kwarc modulu MCP2515: 8 MHz (domyslnie) albo 16 MHz
constexpr uint32_t kSdoTimeoutMs = 300;
constexpr uint32_t kSessionTtlMs = 12u * 3600u * 1000u;     // token 12 h od ostatniej aktywnosci
constexpr int kMaxSessions = 8;
constexpr uint32_t kLevelAutoLockMs = 5u * 60u * 1000u;     // auto-blokada poziomu PIN po 5 min (13.1)
constexpr int kLoginMaxFails = 5; constexpr uint32_t kLoginLockMs = 60000;
constexpr uint32_t kHealthyMs = 30000;                      // slot OTA poprawny po 30 s bez resetu (13.2)
constexpr int kPbkdf2Iter = 10000;
}
