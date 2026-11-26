#pragma once
#include <cstddef>
namespace net { void start(); bool connected(); size_t scan_json(char* out, size_t cap); bool save(const char* ssid, const char* pass); void current_ssid(char* out, size_t cap); }
