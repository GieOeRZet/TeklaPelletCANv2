// tekla_core: straznik zapisu - jedyne miejsce, ktore zamienia zadanie zapisu na ramke SDO. Zasady: tryb pracy, blokada 42 parametrow poza listami panelu,
// zakres z modelu, poziom PIN sesji, kodowanie panelu (plain / x scale / indeks opcji / przelacznik). Zadnych zapisow poza modelem.
#pragma once
#include <cmath>
#include "model.hpp"
#include "sdo.hpp"
namespace tekla {
enum class Mode : uint8_t { ReadOnly = 0, Full = 1 };
enum class WriteErr : uint8_t { Ok = 0, ReadOnlyMode, UnknownPid, NotWritable, Blocked, NoEncoding, OutOfRange, LevelTooLow };
struct WriteResult { WriteErr err; uint32_t raw; CanFrame frame; };
inline WriteResult plan_write(Mode mode, uint8_t session_level, uint8_t node, uint16_t pid, double display_value) {
  WriteResult r{WriteErr::Ok, 0, {}};
  if (mode != Mode::Full) { r.err = WriteErr::ReadOnlyMode; return r; }
  const PidEntry* e = find_pid(pid);
  if (!e) { r.err = WriteErr::UnknownPid; return r; }
  if (e->enc == WE_BLOCKED) { r.err = WriteErr::Blocked; return r; }
  if (e->mode != PM_SETTING && e->mode != PM_COMMAND) { r.err = WriteErr::NotWritable; return r; }
  if (e->enc == WE_NONE || e->enc == WE_OTHER) { r.err = WriteErr::NoEncoding; return r; }   // zapis tylko dla PID z zmierzonym kodowaniem panelu
  if (session_level < level_min(pid)) { r.err = WriteErr::LevelTooLow; return r; }
  double raw = (e->enc == WE_SCALED) ? std::floor(display_value / e->scale + 0.5) : std::floor(display_value + 0.5);
  if (raw < (double)e->min || raw > (double)e->max) { r.err = WriteErr::OutOfRange; return r; }
  r.raw = (uint32_t)(int64_t)raw; r.frame = sdo_write_frame(node, pid, r.raw); return r;
}
}  // namespace tekla
