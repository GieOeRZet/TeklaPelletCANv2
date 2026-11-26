#pragma once
// Tozsamosc urzadzenia: staly ID z MAC ("tp-aabbccddeeff"), nazwa nadawana przez uzytkownika (NVS "dev"/"name"), nazwa hosta mDNS.
namespace device {
const char* id();
const char* host();
const char* name();                 // domyslnie "TeklaPellet"
bool set_name(const char* n);       // walidacja tekla::name_ok; zapis NVS
}
