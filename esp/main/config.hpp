// Stale konfiguracyjne firmware (docs/ESP_API_V2.md sekcje 4-13). Zmiana bez przebudowy architektury.
#pragma once
#include <cstdint>
namespace cfg {
constexpr uint8_t kNodeId = 95;            // wezel ESP na CAN (wlasciciel: 95); konfigurowalny w NVS "node"
constexpr uint8_t kBoilerNode = 4;         // sterownik kotla
// XIAO ESP32-C6 + MCP2515: piny i kwarc ze STAREGO projektu (docs/ARCHITEKTURA.md, firmware/main/main.cpp: PelletCAN/ESPHome): SCK=GPIO19, MISO=GPIO20, MOSI=GPIO18, CS=GPIO17; INT nieuzywany (odczyt przez odpytywanie flag)
constexpr int kPinSck = 19, kPinMiso = 20, kPinMosi = 18, kPinCs = 17, kPinInt = -1;
constexpr bool kOsc16MHz = true;           // kwarc modulu MCP2515: 16 MHz (jak w starym projekcie); 8 MHz = false
constexpr int kSpiHz = 1000000;            // 1 MHz jak w PelletCAN (przy zakloceniach 500/250 kHz)
constexpr int kPinRfSwitch = 3, kPinRfAntenna = 14;   // XIAO ESP32-C6: GPIO3 niski = sterowanie wlaczone, GPIO14 wysoki = antena zewnetrzna U.FL
constexpr bool kExternalAntenna = false;   // true dopiero PO podpieciu anteny (bez niej Wi-Fi zniknie)
constexpr uint32_t kProduct = 1;           // kod produktu 0x1018:2 (panel dobiera po nim nazwe w zakladce Siec; 1 = 'Unknown' jak w PelletCAN)
constexpr const char* kName = "TeklaPellet"; constexpr const char* kVersion = "v0.2"; constexpr int kApiVersion = 2;   // wersja API w tozsamosci: klienci sprawdzaja zgodnosc
constexpr uint32_t kHeartbeatMs = 1000;    // NMT heartbeat wezla co 1 s
constexpr uint32_t kSdoTimeoutMs = 300;
constexpr uint32_t kSessionTtlMs = 12u * 3600u * 1000u;     // token 12 h od ostatniej aktywnosci
constexpr int kMaxSessions = 8;
constexpr uint32_t kLevelAutoLockMs = 5u * 60u * 1000u;     // auto-blokada poziomu PIN po 5 min (13.1)
constexpr int kLoginMaxFails = 5; constexpr uint32_t kLoginLockMs = 60000;
constexpr uint32_t kHealthyMs = 30000;                      // slot OTA poprawny po 30 s bez resetu (13.2)
constexpr int kPbkdf2Iter = 10000;
constexpr const char* kBetaBaseUrl = "https://github.com/GieOeRZet/TeklaPelletCANv2/releases/download/beta";   // kanal BETA: tekla_pellet_beta.bin (z podpisem) + beta.json; mozna zmienic na stronie serwisowej (konto DEV)
}
