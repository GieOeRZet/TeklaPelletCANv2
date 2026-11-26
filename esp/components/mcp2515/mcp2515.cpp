#include "mcp2515.hpp"
#include "driver/spi_master.h"
#include "driver/gpio.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <cstring>
namespace mcp {
static spi_device_handle_t dev; static uint32_t ntx, nrx, nerr;
enum : uint8_t { CMD_RESET = 0xC0, CMD_READ = 0x03, CMD_WRITE = 0x02, CMD_RTS0 = 0x81, CMD_BITMOD = 0x05, CMD_RDRX0 = 0x90, CMD_RDRX1 = 0x94, CMD_STATUS = 0xA0 };
enum : uint8_t { CANSTAT = 0x0E, CANCTRL = 0x0F, CNF3 = 0x28, CNF2 = 0x29, CNF1 = 0x2A, CANINTE = 0x2B, CANINTF = 0x2C, EFLG = 0x2D, TXB0CTRL = 0x30, TXB0SIDH = 0x31, RXB0CTRL = 0x60, RXB1CTRL = 0x70 };
static void xfer(const uint8_t* tx, uint8_t* rx, size_t n) { spi_transaction_t t{}; t.length = n * 8; t.tx_buffer = tx; t.rx_buffer = rx; spi_device_polling_transmit(dev, &t); }
static void wr(uint8_t reg, uint8_t v) { uint8_t b[3] = {CMD_WRITE, reg, v}; xfer(b, nullptr, 3); }
static uint8_t rd(uint8_t reg) { uint8_t tx[3] = {CMD_READ, reg, 0}, rx[3] = {}; xfer(tx, rx, 3); return rx[2]; }
static void bitmod(uint8_t reg, uint8_t mask, uint8_t v) { uint8_t b[4] = {CMD_BITMOD, reg, mask, v}; xfer(b, nullptr, 4); }
bool init(const Pins& p, Osc osc) {
  spi_bus_config_t bc{}; bc.sclk_io_num = p.sck; bc.miso_io_num = p.miso; bc.mosi_io_num = p.mosi; bc.quadwp_io_num = -1; bc.quadhd_io_num = -1; bc.max_transfer_sz = 32;
  if (spi_bus_initialize(SPI2_HOST, &bc, SPI_DMA_DISABLED) != ESP_OK) return false;
  spi_device_interface_config_t dc{}; dc.clock_speed_hz = 4 * 1000 * 1000; dc.mode = 0; dc.spics_io_num = p.cs; dc.queue_size = 2;
  if (spi_bus_add_device(SPI2_HOST, &dc, &dev) != ESP_OK) return false;
  uint8_t r = CMD_RESET; xfer(&r, nullptr, 1); vTaskDelay(pdMS_TO_TICKS(10));
  if ((rd(CANSTAT) & 0xE0) != 0x80) return false;                           // po resecie: tryb konfiguracji
  // 125 kbit/s (wartosci jak w popularnych bibliotekach MCP_CAN): 8 MHz: 01 F1 85, 16 MHz: 03 F0 86 (jak w starym firmware)
  if (osc == Osc::MHz8) { wr(CNF1, 0x01); wr(CNF2, 0xF1); wr(CNF3, 0x85); } else { wr(CNF1, 0x03); wr(CNF2, 0xF0); wr(CNF3, 0x86); }
  wr(RXB0CTRL, 0x64); wr(RXB1CTRL, 0x60);                                  // RXM=11 (bez filtrow), RXB0 rollover do RXB1
  wr(CANINTE, 0x00);                                                       // odczyt przez odpytywanie flag (bez przerwan)
  wr(CANCTRL, 0x00); vTaskDelay(pdMS_TO_TICKS(5));                         // tryb normalny
  return (rd(CANSTAT) & 0xE0) == 0x00;
}
bool send(const tekla::CanFrame& f, uint32_t timeout_ms) {
  int64_t end = esp_timer_get_time() + (int64_t)timeout_ms * 1000;
  while (rd(TXB0CTRL) & 0x08) { if (esp_timer_get_time() > end) { nerr++; return false; } vTaskDelay(1); }   // TXREQ: poprzednia ramka jeszcze nie poszla
  uint8_t b[2 + 5 + 8] = {CMD_WRITE, TXB0SIDH, (uint8_t)(f.id >> 3), (uint8_t)((f.id & 7) << 5), 0, 0, f.len};
  memcpy(b + 7, f.data, f.len); xfer(b, nullptr, 7 + f.len);
  uint8_t rts = CMD_RTS0; xfer(&rts, nullptr, 1); ntx++; return true;
}
bool recv(tekla::CanFrame* f) {
  uint8_t flag = rd(CANINTF); uint8_t cmd, mask;
  if (flag & 0x01) { cmd = CMD_RDRX0; mask = 0x01; } else if (flag & 0x02) { cmd = CMD_RDRX1; mask = 0x02; } else return false;
  uint8_t tx[1 + 13] = {cmd}, rx[1 + 13] = {}; xfer(tx, rx, 14);                // odczyt bufora zeruje flage RXnIF (instrukcja READ RX BUFFER)
  f->id = (rx[1] << 3) | (rx[2] >> 5); f->len = rx[5] & 0x0F; if (f->len > 8) f->len = 8; memcpy(f->data, rx + 6, f->len);
  (void)mask; nrx++; return true;
}
uint32_t tx_count() { return ntx; } uint32_t rx_count() { return nrx; } uint32_t err_count() { return nerr; }
}  // namespace mcp
