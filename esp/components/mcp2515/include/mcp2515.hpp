// Sterownik MCP2515 (SPI) dla ESP-IDF 5.x: CAN 125 kbit/s, ramki standardowe 11-bit. NIEKOMPILOWANE w srodowisku agenta (brak toolchaina) - do weryfikacji na sprzecie.
#pragma once
#include <cstdint>
#include "tekla/sdo.hpp"
namespace mcp {
struct Pins { int sck, miso, mosi, cs, irq, spi_hz; };
enum class Osc : uint8_t { MHz8, MHz16 };
bool init(const Pins& p, Osc osc);                 // reset, konfiguracja 125 kbit/s, filtry wylaczone (przyjmuje wszystko), tryb normalny
bool send(const tekla::CanFrame& f, uint32_t timeout_ms);
bool recv(tekla::CanFrame* f);                     // bez blokowania; false = brak ramki
uint32_t tx_count(); uint32_t rx_count(); uint32_t err_count();
}
