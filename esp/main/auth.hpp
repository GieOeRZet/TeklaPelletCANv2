#pragma once
#include <cstdint>
#include "tekla/guard.hpp"
namespace auth {
struct Session { bool used; char token[48]; uint8_t level; int64_t last_us, level_us; int8_t client = -1; int64_t svc_us = 0, dev_us = 0; };   // client >= 0: sesja sparowanego klienta (token staly); svc_us: odblokowanie operacji serwisowych
bool configured();                                                    // haslo ustawione (pierwsza konfiguracja wykonana)
bool setup(const char* password, const char* pin1, const char* pin2, const char* pin3);   // zapisuje hasze (PBKDF2-SHA256, sol 16 B) w NVS
int verify_limited(const char* password, uint32_t key);                 // 0 ok, 1 bledne, 2 zablokowane; limit prob jak w login(), klucz zamiast IP (np. kanal BLE)
int login(const char* password, uint32_t ip, char* token_out);        // 0 ok, 1 bledne haslo, 2 zablokowane (5 prob/60 s na IP)
Session* find(const char* token);                                     // odswieza aktywnosc; wygasza token po TTL i poziom po bezczynnosci
int set_level(Session* s, const char* pin);                           // 0 ok (poziom wg dopasowanego PIN), 1 bledny PIN, 2 zablokowane
void drop_level(Session* s);
// Sparowani klienci: pierwsze polaczenie haslem -> staly token (przechowywany jako SHA-256), pozniej samo Authorization: Bearer <token>. Maks. 8; odwolanie pojedyncze lub wszystkich.
int pair(const char* password, const char* client_name, uint32_t ip, char* token_out);   // 0 ok, 1 bledne haslo, 2 zablokowane, 3 brak miejsca, 4 zla nazwa
int client_count(); const char* client_name(int i); bool client_revoke(int i); void client_revoke_all();
// Operacje serwisowe (Wi-Fi, IP, haslo, nazwa, klienci, OTA, restart, reset): wymagaja swiezego podania hasla (5 min), niezaleznie od trybu zapisu do kotla.
int service_unlock(Session* s, const char* password, uint32_t ip);   // 0 ok, 1 bledne haslo, 2 zablokowane
bool service_ok(Session* s);
// Konto DEV: osobne haslo (NVS "auth"/"dev", hasz PBKDF2), odblokowanie 5 min; daje dostep do OTA z kanalu BETA na stronie serwisowej.
bool dev_configured(); bool dev_set(const char* password);
int dev_unlock(Session* s, const char* password, uint32_t ip);   // 0 ok, 1 bledne haslo, 2 zablokowane
bool dev_ok(Session* s);
bool verify_password(const char* pw); bool set_password(const char* pw);   // set_password: min. 8 znakow
void factory_reset();                                                  // kasuje haslo, PIN-y i Wi-Fi z NVS
tekla::Mode mode(); bool set_mode(tekla::Mode m, const char* password);   // tryb globalny urzadzenia, po restarcie zawsze tylko-odczyt
}
