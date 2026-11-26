#pragma once
#include <cstdint>
namespace auth {
struct Session { bool used; char token[48]; int64_t last_us; int8_t client = -1; int64_t svc_us = 0; int64_t dev_us = 0; };
bool configured(); bool setup(const char* password);
int verify_limited(const char* password, uint32_t key); int login(const char* password, uint32_t ip, char* token_out);
Session* find(const char* token);
int pair(const char* password, const char* client_name, uint32_t ip, char* token_out);
int client_count(); const char* client_name(int i); bool client_revoke(int i); void client_revoke_all();
int service_unlock(Session* s, const char* password, uint32_t ip); bool service_ok(Session* s);
bool dev_configured(); bool dev_set(const char* password); int dev_unlock(Session* s, const char* password, uint32_t ip); bool dev_ok(Session* s);
bool recovery_configured(); bool recovery_set(const char* code); int recover(const char* code, const char* new_password, uint32_t key);
bool verify_password(const char* pw); bool set_password(const char* pw); void factory_reset();
}
