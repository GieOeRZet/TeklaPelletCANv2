// tekla_core: tablice modelu (generowane z data/device_model.json). Bez zaleznosci od ESP-IDF.
#pragma once
#include <cstdint>
#include <cstddef>
namespace tekla {
enum TypeTag : uint8_t { TT_U8 = 1, TT_U16, TT_S16, TT_U32 };
enum PidMode : uint8_t { PM_SETTING = 1, PM_LIVE, PM_COMMAND, PM_MIRROR };
enum WriteEnc : uint8_t { WE_NONE = 0, WE_PLAIN, WE_SCALED, WE_OPTION_INDEX, WE_SWITCH, WE_OTHER, WE_BLOCKED };
struct PidEntry { uint16_t pid; TypeTag type; PidMode mode; int64_t min, max; WriteEnc enc; float scale; };
struct LevelEntry { uint16_t pid; uint8_t level_min; };
static const PidEntry kPids[] = {
#include "../../gen/model_pids.inc"
};
static const LevelEntry kLevels[] = {
#include "../../gen/model_levels.inc"
};
struct ListDef { const char* key; const uint16_t* pids; uint16_t n; };
#include "../../gen/model_lists.inc"
static const size_t kPidCount = sizeof(kPids) / sizeof(kPids[0]);
static const size_t kLevelCount = sizeof(kLevels) / sizeof(kLevels[0]);
inline const PidEntry* find_pid(uint16_t pid) {          // tablica posortowana rosnaco (generator)
  size_t lo = 0, hi = kPidCount;
  while (lo < hi) { size_t mid = (lo + hi) / 2; if (kPids[mid].pid < pid) lo = mid + 1; else hi = mid; }
  return (lo < kPidCount && kPids[lo].pid == pid) ? &kPids[lo] : nullptr;
}
inline uint8_t level_min(uint16_t pid) {                  // brak wpisu = poziom 3 (najostrozniej)
  for (size_t i = 0; i < kLevelCount; i++) if (kLevels[i].pid == pid) return kLevels[i].level_min;
  return 3;
}
}  // namespace tekla
