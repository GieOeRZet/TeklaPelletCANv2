// tekla_core: ramki SDO do/z sterownika (wezel 4): odczyt 0x604 -> odpowiedz 0x584; zapis jak panel: 42 idx_lo idx_hi 01 u32LE.
// Zrodlo: poll_sequence_655.csv, write_frames.json (MEASURED w emulatorze panelu).
#pragma once
#include <cstdint>
#include <cstring>
namespace tekla {
struct CanFrame { uint32_t id; uint8_t len; uint8_t data[8]; };
inline CanFrame sdo_read_frame(uint8_t node, uint16_t index, uint8_t sub = 1) {
  CanFrame f{}; f.id = 0x600u + node; f.len = 8; f.data[0] = 0x40; f.data[1] = index & 0xFF; f.data[2] = index >> 8; f.data[3] = sub; return f;
}
inline CanFrame sdo_write_frame(uint8_t node, uint16_t index, uint32_t raw, uint8_t sub = 1) {
  CanFrame f{}; f.id = 0x600u + node; f.len = 8; f.data[0] = 0x42; f.data[1] = index & 0xFF; f.data[2] = index >> 8; f.data[3] = sub;
  f.data[4] = raw & 0xFF; f.data[5] = (raw >> 8) & 0xFF; f.data[6] = (raw >> 16) & 0xFF; f.data[7] = (raw >> 24) & 0xFF; return f;
}
struct SdoReply { bool ok; uint16_t index; uint8_t sub; uint32_t value; bool abort; };
inline SdoReply sdo_parse_reply(uint8_t node, const CanFrame& f) {
  SdoReply r{}; if (f.id != 0x580u + node || f.len < 8) return r;
  r.index = f.data[1] | (f.data[2] << 8); r.sub = f.data[3];
  r.value = f.data[4] | (f.data[5] << 8) | (f.data[6] << 16) | ((uint32_t)f.data[7] << 24);
  r.abort = (f.data[0] == 0x80); r.ok = !r.abort; return r;
}
}  // namespace tekla
