#pragma once
#include <cstdint>
#include "tekla/guard.hpp"
namespace auth {
struct Session { bool used; char token[48]; uint8_t level; int64_t last_us, level_us; };
bool configured();                                                    // haslo ustawione (pierwsza konfiguracja wykonana)
bool setup(const char* password, const char* pin1, const char* pin2, const char* pin3);   // zapisuje hasze (PBKDF2-SHA256, sol 16 B) w NVS
int login(const char* password, uint32_t ip, char* token_out);        // 0 ok, 1 bledne haslo, 2 zablokowane (5 prob/60 s na IP)
Session* find(const char* token);                                     // odswieza aktywnosc; wygasza token po TTL i poziom po bezczynnosci
int set_level(Session* s, const char* pin);                           // 0 ok (poziom wg dopasowanego PIN), 1 bledny PIN, 2 zablokowane
void drop_level(Session* s);
tekla::Mode mode(); bool set_mode(tekla::Mode m, const char* password);   // tryb globalny urzadzenia, po restarcie zawsze tylko-odczyt
}
