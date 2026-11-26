#include <cassert>
#include <cstdio>
#include <cstring>
#include "tekla/guard.hpp"
#include "tekla/rules.hpp"
#include "tekla/live.hpp"
#include "tekla/b64.hpp"
#include "tekla/ui_pack.hpp"
#include "tekla/ecdsa_der.hpp"
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
  CHECK(kListCount == 13); for (size_t i = 0; i < kListCount; i++) for (uint16_t j = 0; j < kLists[i].n; j++) CHECK(find_pid(kLists[i].pids[j]) != nullptr);
  // konwersje live
  CHECK(live_value(LK_DC, 0x0270) == 62.4); CHECK(live_value(LK_DC, 0xFFEC) == -2.0); CHECK(live_value(LK_STATE, 0x0101) == 1.0); CHECK(live_value(LK_STATE, 0x013C) == 60.0); CHECK(live_value(LK_D10, 21) == 2.1);
  CHECK(kLiveCount > 40); for (size_t i = 0; i < kLiveCount; i++) CHECK(find_pid(kLive[i].pid) != nullptr);   // kazdy PID zestawu live istnieje w modelu
  LiveItem li[3] = {{0x2001, 62.4}, {0x2277, 1}, {0x215F, -3.5}}; char jb[160]; size_t jn = json_live(li, 3, jb, sizeof jb);
  CHECK(jn > 0 && !strcmp(jb, "{\"t\":\"live\",\"v\":{\"0x2001\":62.4,\"0x2277\":1.0,\"0x215F\":-3.5}}")); char small[16]; CHECK(json_live(li, 3, small, sizeof small) == 0);
  // token base64url
  uint8_t tk[3] = {0xFB, 0xEF, 0xBE}; char tb[16]; CHECK(b64url(tk, 3, tb, sizeof tb) == 4 && !strcmp(tb, "----")); uint8_t t2[2] = {0xFF, 0xFF}; CHECK(b64url(t2, 2, tb, sizeof tb) == 3 && !strcmp(tb, "__8"));
  // podpis ECDSA: surowe r||s -> DER (wektory z biblioteki cryptography)
  { const uint8_t rs[64] = {0x82,0x42,0xba,0x83,0x4a,0xad,0x09,0xfc,0x17,0x25,0x7d,0x5c,0xd3,0xf1,0xdb,0x5d,0x09,0x85,0xe7,0x54,0x51,0x40,0xac,0xb9,0x6a,0x3c,0x5f,0xf5,0xf4,0x6b,0xe3,0xc7,0x80,0x89,0x54,0xdc,0x79,0x42,0xd1,0x6d,0x6c,0xd2,0xb4,0x23,0xf6,0x57,0xfa,0x54,0x76,0x8d,0x25,0x5b,0x5f,0x90,0xc1,0xb8,0x67,0xd6,0xc8,0xe2,0x04,0x55,0x27,0x77}; const uint8_t ex[] = {0x30,0x46,0x02,0x21,0x00,0x82,0x42,0xba,0x83,0x4a,0xad,0x09,0xfc,0x17,0x25,0x7d,0x5c,0xd3,0xf1,0xdb,0x5d,0x09,0x85,0xe7,0x54,0x51,0x40,0xac,0xb9,0x6a,0x3c,0x5f,0xf5,0xf4,0x6b,0xe3,0xc7,0x02,0x21,0x00,0x80,0x89,0x54,0xdc,0x79,0x42,0xd1,0x6d,0x6c,0xd2,0xb4,0x23,0xf6,0x57,0xfa,0x54,0x76,0x8d,0x25,0x5b,0x5f,0x90,0xc1,0xb8,0x67,0xd6,0xc8,0xe2,0x04,0x55,0x27,0x77}; uint8_t o[80]; size_t n = sig_rs_to_der(rs, o, sizeof o); CHECK(n == sizeof ex && !memcmp(o, ex, n)); }
  { const uint8_t rs[64] = {0xc3,0x8a,0x56,0xf6,0x34,0x93,0x17,0x49,0xdb,0x20,0x97,0x1e,0x29,0x06,0x36,0x7b,0xc2,0xe8,0xe9,0xb8,0x75,0x65,0xea,0x99,0xa9,0x49,0xa1,0x11,0xa1,0x30,0x55,0xee,0xa7,0x04,0x64,0xac,0x21,0x98,0xc9,0xb3,0xfc,0x5a,0x17,0xa6,0xb4,0xf7,0xb3,0x60,0x11,0xc1,0x41,0xf6,0x76,0x9b,0x87,0x44,0xec,0x07,0x21,0xc5,0x51,0x2f,0x1c,0x38}; const uint8_t ex[] = {0x30,0x46,0x02,0x21,0x00,0xc3,0x8a,0x56,0xf6,0x34,0x93,0x17,0x49,0xdb,0x20,0x97,0x1e,0x29,0x06,0x36,0x7b,0xc2,0xe8,0xe9,0xb8,0x75,0x65,0xea,0x99,0xa9,0x49,0xa1,0x11,0xa1,0x30,0x55,0xee,0x02,0x21,0x00,0xa7,0x04,0x64,0xac,0x21,0x98,0xc9,0xb3,0xfc,0x5a,0x17,0xa6,0xb4,0xf7,0xb3,0x60,0x11,0xc1,0x41,0xf6,0x76,0x9b,0x87,0x44,0xec,0x07,0x21,0xc5,0x51,0x2f,0x1c,0x38}; uint8_t o[80]; size_t n = sig_rs_to_der(rs, o, sizeof o); CHECK(n == sizeof ex && !memcmp(o, ex, n)); }
  { uint8_t z[64] = {}; z[63] = 1; uint8_t o[80]; size_t n = sig_rs_to_der(z, o, sizeof o); CHECK(n == 8 && o[0] == 0x30 && o[1] == 6 && o[2] == 2 && o[3] == 1 && o[4] == 0 && o[5] == 2 && o[6] == 1 && o[7] == 1); }   // r=0 -> jeden bajt 0
  // paczka UI (argv[1] = plik z tools/pack_ui.py)
  if (argc > 1) {
    std::ifstream f(argv[1], std::ios::binary); std::vector<uint8_t> d((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>()); CHECK(d.size() > 1000 && ui_verify(d.data(), d.size()));
    UiFile uf; CHECK(ui_find(d.data(), d.size(), "index.html", &uf) && uf.gzip && !strcmp(uf.mime, "text/html; charset=utf-8") && uf.size > 100 && uf.data[0] == 0x1F && uf.data[1] == 0x8B);
    CHECK(ui_find(d.data(), d.size(), "assets/ic/106.png", &uf) && !uf.gzip && uf.data[1] == 'P'); CHECK(!ui_find(d.data(), d.size(), "nie_ma.js", &uf));
    d[d.size() / 2] ^= 0xFF; CHECK(!ui_verify(d.data(), d.size()));            // uszkodzona paczka wykrywana (healthy start)
  }
  printf(fails ? "TESTY: %d BLEDOW\n" : "TESTY OK\n", fails); return fails ? 1 : 0;
}
