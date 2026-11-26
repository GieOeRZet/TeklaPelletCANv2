// tekla_core: tozsamosc wezla ESP na magistrali (jak PelletCAN/ESPHome): boot-up + heartbeat NMT oraz odpowiedzi SDO (upload) na 0x1000/0x1001/0x1008/0x100A/0x1018.
// Dzieki temu panel widzi wezel na zakladce "Siec". Zrodlo: GieOeRZet/PelletCAN esphome/PelletCAN_packages.yaml (linie 751-797, 1346-1353).
#pragma once
#include <cstdint>
#include <cstring>
#include "sdo.hpp"
namespace tekla {
inline CanFrame nmt_bootup_frame(uint8_t node) { CanFrame f{}; f.id = 0x700u + node; f.len = 1; f.data[0] = 0x00; return f; }
inline CanFrame nmt_heartbeat_frame(uint8_t node) { CanFrame f{}; f.id = 0x700u + node; f.len = 1; f.data[0] = 0x05; return f; }   // 0x05 = operational
class IdentityResponder {
 public:
  IdentityResponder(uint8_t node, const char* name, const char* version, uint32_t product) : node_(node), product_(product) { strncpy(name_, name, sizeof name_ - 1); strncpy(ver_, version, sizeof ver_ - 1); }
  // Przetwarza ramke odebrana z magistrali; zwraca true i wypelnia *out, gdy trzeba odpowiedziec (0x580+node).
  bool handle(const CanFrame& rx, CanFrame* out) {
    if (rx.id != 0x600u + node_ || rx.len < 4) return false;
    CanFrame r{}; r.id = 0x580u + node_; r.len = 8; uint8_t cs = rx.data[0]; uint16_t idx = rx.data[1] | (rx.data[2] << 8); uint8_t sub = rx.data[3];
    if (cs == 0x40) {
      r.data[1] = rx.data[1]; r.data[2] = rx.data[2]; r.data[3] = rx.data[3];
      if (idx == 0x1000 && sub == 0) exp(r, 0, 4); else if (idx == 0x1001 && sub == 0) exp(r, 0, 1);
      else if (idx == 0x1008 && sub == 0) str(r, name_); else if (idx == 0x100A && sub == 0) str(r, ver_);
      else if (idx == 0x1018 && sub == 0) exp(r, 4, 1); else if (idx == 0x1018 && sub == 1) exp(r, 0x4E4350, 4);     // producent (PCN)
      else if (idx == 0x1018 && sub == 2) exp(r, product_, 4); else if (idx == 0x1018 && sub == 3) exp(r, 1, 4); else if (idx == 0x1018 && sub == 4) exp(r, 1, 4);
      else { r.data[0] = 0x80; r.data[4] = 0x00; r.data[5] = 0x00; r.data[6] = 0x02; r.data[7] = 0x06; }                // abort 0x06020000 (obiekt nie istnieje)
    } else if ((cs & 0xE0) == 0x60) {                                                                                   // kolejny segment napisu
      size_t left = seg_len_ > seg_pos_ ? seg_len_ - seg_pos_ : 0, n = left > 7 ? 7 : left;
      r.data[0] = (cs & 0x10) | ((7 - n) << 1) | (left <= 7 ? 1 : 0); for (size_t i = 0; i < n; i++) r.data[1 + i] = (uint8_t)seg_[seg_pos_ + i]; seg_pos_ += n;
    } else return false;
    *out = r; return true;
  }
 private:
  void exp(CanFrame& r, uint32_t v, int n) { r.data[0] = 0x43 | ((4 - n) << 2); for (int i = 0; i < n; i++) r.data[4 + i] = (v >> (8 * i)) & 0xFF; }
  void str(CanFrame& r, const char* t) {
    size_t l = strlen(t);
    if (l <= 4) { r.data[0] = 0x43 | ((4 - l) << 2); for (size_t i = 0; i < l; i++) r.data[4 + i] = (uint8_t)t[i]; }
    else { strncpy(seg_, t, sizeof seg_ - 1); seg_len_ = l; seg_pos_ = 0; r.data[0] = 0x41; for (int i = 0; i < 4; i++) r.data[4 + i] = (l >> (8 * i)) & 0xFF; }
  }
  uint8_t node_; uint32_t product_; char name_[24] = {}, ver_[16] = {}; char seg_[24] = {}; size_t seg_len_ = 0, seg_pos_ = 0;
};
}  // namespace tekla
