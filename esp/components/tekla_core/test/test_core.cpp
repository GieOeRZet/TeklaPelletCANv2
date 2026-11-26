#include <cassert>
#include <cstdio>
#include <cstring>
#include "tekla/guard.hpp"
#include "tekla/rules.hpp"
#include "tekla/live.hpp"
#include "tekla/b64.hpp"
#include "tekla/ui_pack.hpp"
#include <vector>
#include <fstream>
#include <iterator>
using namespace tekla;
static int fails = 0;
#define CHECK(c) do { if (!(c)) { printf("FAIL %s:%d %s\n", __FILE__, __LINE__, #c); fails++; } } while (0)
int main(int argc, char** argv) {
  // ramka odczytu 0x604: 40 idx_lo idx_hi 01 ...
  CanFrame r = sdo_read_frame(4, 0x3601); CHECK(r.id == 0x604 && r.data[0] == 0x40 && r.data[1] == 0x01 && r.data[2] == 0x36 && r.data[3] == 1);
  // odpowiedz 0x584 (przyklad z HA: 0x2277 = 257)
  CanFrame a{0x584, 8, {0x43, 0x77, 0x22, 0x01, 0x01, 0x01, 0, 0}}; SdoReply rp = sdo_parse_reply(4, a);
  CHECK(rp.ok && rp.index == 0x2277 && rp.value == 257 && output_state(rp.value) == 1);
  CHECK(!sdo_parse_reply(5, a).ok);
  // ramka zapisu panelu (MEASURED): PID 0x2003 raw 66 -> 42 03 20 01 42 00 00 00
  CanFrame w = sdo_write_frame(4, 0x2003, 66); const uint8_t exp[8] = {0x42, 0x03, 0x20, 0x01, 0x42, 0, 0, 0}; CHECK(w.id == 0x604 && memcmp(w.data, exp, 8) == 0);
  // model: liczba PID i wyszukiwanie
  CHECK(kPidCount == 655); CHECK(find_pid(0x2003) && find_pid(0x2003)->min == 60 && find_pid(0x2003)->max == 80); CHECK(!find_pid(0x1234));
  // straznik: tryb tylko-odczyt blokuje wszystko
  CHECK(plan_write(Mode::ReadOnly, 3, 4, 0x2003, 65).err == WriteErr::ReadOnlyMode);
  // zakres i zapis poprawny (kociol: temperatura zadana reczna 60..80, poziom >= level_min)
  WriteResult ok = plan_write(Mode::Full, 3, 4, 0x2003, 66); CHECK(ok.err == WriteErr::Ok && ok.raw == 66 && ok.frame.data[4] == 66);
  CHECK(plan_write(Mode::Full, 3, 4, 0x2003, 90).err == WriteErr::OutOfRange);
  // 42 parametry poza listami: WE_BLOCKED (kominiarz 0x3750, drewno 0x3000, typ paliwa 0x299F, ruszt 0x2151)
  CHECK(plan_write(Mode::Full, 3, 4, 0x3750, 1).err == WriteErr::Blocked);
  CHECK(plan_write(Mode::Full, 3, 4, 0x3000, 50).err == WriteErr::Blocked);
  CHECK(plan_write(Mode::Full, 3, 4, 0x299F, 1).err == WriteErr::Blocked);
  CHECK(plan_write(Mode::Full, 3, 4, 0x2151, 1).err == WriteErr::Blocked);
  // PID tylko do odczytu (live) i nieznany
  CHECK(plan_write(Mode::Full, 3, 4, 0x2001, 20).err == WriteErr::NotWritable);
  CHECK(plan_write(Mode::Full, 3, 4, 0x1234, 1).err == WriteErr::UnknownPid);
  // licznik paliwa 0x3601 (jedyny dotychczasowy zapis na zywym kotle): plain kg
  const PidEntry* fe = find_pid(0x3601); CHECK(fe != nullptr);
  // reguly ikon
  CHECK(mixer_icon(0x100) == MIX_NONE && mixer_icon(0x101) == MIX_CLOSE_BLUE_106 && mixer_icon(0x102) == MIX_OPEN_RED_107);
  CHECK(!burner_led_on(0) && burner_led_on(1) && burner_led_on(8) && !burner_led_on(9) && !burner_led_on(10));
  CHECK(!strcmp(burner_state_name(5), "Modulacja") && !strcmp(solar_state_name(2), "Przeplukiwanie") && !strcmp(ch_status_name(8), "Zrodlo blokuje"));
  // kropki paliwa (pojemnosc 135, rezerwa 0): 0-10 kg 0, 11-33 -> 2, 34-51 -> 5, 52-76 -> 9, 77-99 -> 14, 100-117 -> 19, 118+ -> 24
  CHECK(fuel_icon(10, 135, 0).dots == 0 && fuel_icon(11, 135, 0).dots == 2 && fuel_icon(33, 135, 0).dots == 2 && fuel_icon(34, 135, 0).dots == 5);
  CHECK(fuel_icon(52, 135, 0).dots == 9 && fuel_icon(77, 135, 0).dots == 14 && fuel_icon(100, 135, 0).dots == 19 && fuel_icon(118, 135, 0).dots == 24);
  CHECK(fuel_icon(9, 135, 10).empty_E && !fuel_icon(11, 135, 10).empty_E);
  char b[16]; format_hmm(180, b, 16); CHECK(!strcmp(b, "3:00")); format_hmm(75, b, 16); CHECK(!strcmp(b, "1:15"));
  // konwersje live
  CHECK(live_value(LK_DC, 0x0270) == 62.4); CHECK(live_value(LK_DC, 0xFFEC) == -2.0); CHECK(live_value(LK_STATE, 0x0101) == 1.0); CHECK(live_value(LK_STATE, 0x013C) == 60.0); CHECK(live_value(LK_D10, 21) == 2.1);
  CHECK(kLiveCount > 40); for (size_t i = 0; i < kLiveCount; i++) CHECK(find_pid(kLive[i].pid) != nullptr);   // kazdy PID zestawu live istnieje w modelu
  LiveItem li[3] = {{0x2001, 62.4}, {0x2277, 1}, {0x215F, -3.5}}; char jb[160]; size_t jn = json_live(li, 3, jb, sizeof jb);
  CHECK(jn > 0 && !strcmp(jb, "{\"t\":\"live\",\"v\":{\"0x2001\":62.4,\"0x2277\":1.0,\"0x215F\":-3.5}}")); char small[16]; CHECK(json_live(li, 3, small, sizeof small) == 0);
  // token base64url
  uint8_t tk[3] = {0xFB, 0xEF, 0xBE}; char tb[16]; CHECK(b64url(tk, 3, tb, sizeof tb) == 4 && !strcmp(tb, "----")); uint8_t t2[2] = {0xFF, 0xFF}; CHECK(b64url(t2, 2, tb, sizeof tb) == 3 && !strcmp(tb, "__8"));
  // paczka UI (argv[1] = plik z tools/pack_ui.py)
  if (argc > 1) {
    std::ifstream f(argv[1], std::ios::binary); std::vector<uint8_t> d((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>()); CHECK(d.size() > 1000 && ui_verify(d.data(), d.size()));
    UiFile uf; CHECK(ui_find(d.data(), d.size(), "index.html", &uf) && uf.gzip && !strcmp(uf.mime, "text/html; charset=utf-8") && uf.size > 100 && uf.data[0] == 0x1F && uf.data[1] == 0x8B);
    CHECK(ui_find(d.data(), d.size(), "assets/ic/106.png", &uf) && !uf.gzip && uf.data[1] == 'P'); CHECK(!ui_find(d.data(), d.size(), "nie_ma.js", &uf));
    d[d.size() / 2] ^= 0xFF; CHECK(!ui_verify(d.data(), d.size()));            // uszkodzona paczka wykrywana (healthy start)
  }
  printf(fails ? "TESTY: %d BLEDOW\n" : "TESTY OK\n", fails); return fails ? 1 : 0;
}
