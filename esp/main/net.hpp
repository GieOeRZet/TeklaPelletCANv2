#pragma once
#include <cstddef>
#include <cstdint>
namespace net {
struct IpCfg { bool dhcp = true; uint32_t ip = 0, mask = 0, gw = 0, dns1 = 0, dns2 = 0; };   // adresy w porzadku "a.b.c.d" = a<<24|...
void start();                                       // Wi-Fi STA (zapisane dane) albo AP pierwszej konfiguracji; mDNS z ID i TXT; zadanie awaryjnego AP
bool connected();
int64_t down_s();                                    // sekundy bez Wi-Fi (0 gdy polaczono)
int64_t up_s();                                      // sekundy od uzyskania IP (0 gdy brak)
bool wifi_save(const char* ssid, const char* pass);  // zapis danych Wi-Fi do NVS (pierwsza konfiguracja)
size_t scan_json(char* out, size_t cap);
void current_ssid(char* out, size_t cap);
bool wifi_apply(const char* ssid, const char* pass); // asynchronicznie: laczy z nowa siecia; bez IP w 40 s wraca do poprzedniej; zapis NVS dopiero po sukcesie
void ip_get(IpCfg* saved, IpCfg* current, bool* pending);
bool ip_apply(const IpCfg& c);                       // asynchronicznie; po zmianie klient musi w 60 s wywolac ip_confirm z nowego adresu, inaczej powrot do poprzednich ustawien
void ip_confirm();
void refresh_identity();                             // nazwa urzadzenia i TXT mDNS po zmianie nazwy
}
