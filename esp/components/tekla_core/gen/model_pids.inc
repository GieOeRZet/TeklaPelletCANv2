// GENEROWANE przez tools/gen_esp_tables.py z data/device_model.json - nie edytowac recznie
// {pid, typ, tryb, min, max, kodowanie zapisu, skala}
// WE_BLOCKED = zapis zabroniony (parametry poza listami panelu)
// Odczyt stanu wyjsc: stan = (wartosc SDO & 0xFF) (patrz docs/ESP_API_V2.md 13.4)
  {0x0003, TT_U8, PM_SETTING, 0LL, 30LL, WE_NONE, 1.0f}, /* Language */
  {0x0004, TT_U16, PM_SETTING, 0LL, 65535LL, WE_NONE, 1.0f}, /* Parameters set Number */
  {0x0005, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Default settings request */
  {0x2001, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Boiler temp[dC] */
  {0x2002, TT_U16, PM_SETTING, 0LL, 3LL, WE_NONE, 1.0f}, /* Boiler program */
  {0x2003, TT_U16, PM_SETTING, 60LL, 80LL, WE_PLAIN, 1.0f}, /* Boiler set manual temp[C] */
  {0x2004, TT_U16, PM_LIVE, 60LL, 150LL, WE_NONE, 1.0f}, /* Boiler set result temp[C] */
  {0x2005, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Boiler on/off */
  {0x2006, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Burner external permission */
  {0x2007, TT_U8, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Burner state */
  {0x200A, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Remote boiler 1 temp[dC] */
  {0x200B, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Remote boiler 2 temp[dC] */
  {0x200C, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Remote boiler 3 temp[dC] */
  {0x200D, TT_U16, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Remote boiler 1 requested temp[C] */
  {0x200E, TT_U16, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Remote boiler 2 requested temp[C] */
  {0x200F, TT_U16, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Remote boiler 3 requested temp[C] */
  {0x2010, TT_U16, PM_SETTING, 20LL, 70LL, WE_PLAIN, 1.0f}, /* Hot water 1 comfort temperature */
  {0x2012, TT_U16, PM_SETTING, 20LL, 70LL, WE_PLAIN, 1.0f}, /* Hot water 1 economical temperature */
  {0x2014, TT_U8, PM_SETTING, 0LL, 3LL, WE_BLOCKED, 1.0f}, /* Hot water 1 program */
  {0x2016, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* Room measured temp */
  {0x2017, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* Room measured temp */
  {0x2018, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* Room measured temp */
  {0x2019, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* Room measured temp */
  {0x201A, TT_U16, PM_LIVE, 0LL, 500LL, WE_NONE, 1.0f}, /* Hot water 1 result temperature */
  {0x201C, TT_U8, PM_SETTING, 40LL, 80LL, WE_PLAIN, 1.0f}, /* Hot water 1 temperature max */
  {0x2032, TT_U16, PM_SETTING, 150LL, 300LL, WE_SCALED, 0.1f}, /* CH comf temp set [dC] */
  {0x2033, TT_U16, PM_SETTING, 150LL, 300LL, WE_SCALED, 0.1f}, /* CH comf temp set [dC] */
  {0x2034, TT_U16, PM_SETTING, 150LL, 300LL, WE_NONE, 1.0f}, /* CH comf temp set [dC] */
  {0x2035, TT_U16, PM_SETTING, 150LL, 300LL, WE_NONE, 1.0f}, /* CH comf temp set [dC] */
  {0x2048, TT_U16, PM_SETTING, 100LL, 300LL, WE_SCALED, 0.1f}, /* CH eco temp set [dC] */
  {0x2049, TT_U16, PM_SETTING, 100LL, 300LL, WE_SCALED, 0.1f}, /* CH eco temp set [dC] */
  {0x204A, TT_U16, PM_SETTING, 100LL, 300LL, WE_NONE, 1.0f}, /* CH eco temp set [dC] */
  {0x204B, TT_U16, PM_SETTING, 100LL, 300LL, WE_NONE, 1.0f}, /* CH eco temp set [dC] */
  {0x2064, TT_U8, PM_SETTING, 0LL, 3LL, WE_BLOCKED, 1.0f}, /* CH program */
  {0x2065, TT_U8, PM_SETTING, 0LL, 3LL, WE_OPTION_INDEX, 1.0f}, /* CH program */
  {0x2066, TT_U8, PM_SETTING, 0LL, 3LL, WE_OPTION_INDEX, 1.0f}, /* CH program */
  {0x2067, TT_U8, PM_SETTING, 0LL, 3LL, WE_OPTION_INDEX, 1.0f}, /* CH program */
  {0x2101, TT_U16, PM_LIVE, 0LL, 10000LL, WE_NONE, 1.0f}, /* Solar power [W] */
  {0x2102, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Solar temperature 2 [C] */
  {0x2103, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Solar temperature 3 [C] */
  {0x2104, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Solar temperature 4 [C] */
  {0x2105, TT_U8, PM_SETTING, 0LL, 4LL, WE_PLAIN, 1.0f}, /* Heating configuration */
  {0x2106, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Hot water configuration */
  {0x2107, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Buffer configuration */
  {0x2108, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Solar configuration */
  {0x2109, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Solar temperature 1 [C] */
  {0x2110, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* Hot water 1 measured temperature */
  {0x2111, TT_U16, PM_LIVE, 0LL, 150LL, WE_NONE, 1.0f}, /* Hot water 1 needed temp. [C] */
  {0x2112, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Buffer top measured temperature */
  {0x2113, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Buffer bottom measured temperature */
  {0x2114, TT_U16, PM_SETTING, 7LL, 80LL, WE_PLAIN, 1.0f}, /* Buffer top set temperature */
  {0x2115, TT_U16, PM_SETTING, 7LL, 80LL, WE_PLAIN, 1.0f}, /* Buffer bottom set temperature */
  {0x2116, TT_S16, PM_SETTING, 7LL, 200LL, WE_SCALED, 0.1f}, /* Solar flow [l/min] */
  {0x2119, TT_S16, PM_LIVE, -50LL, 350LL, WE_NONE, 1.0f}, /* Exhaust gas temperature (2) [C] */
  {0x2120, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Lambda oxygen [%] */
  {0x2122, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Compatibility igneo solar pump state */
  {0x2123, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Lambda module presence */
  {0x2124, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Total fuel counter[hg] */
  {0x2125, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Device type */
  {0x2126, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule monday */
  {0x2127, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule monday */
  {0x2128, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule monday */
  {0x2129, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule monday */
  {0x212A, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule tuesday */
  {0x212B, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule tuesday */
  {0x212C, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule tuesday */
  {0x212D, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule tuesday */
  {0x212E, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule wednesday */
  {0x212F, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule wednesday */
  {0x2130, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule wednesday */
  {0x2131, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule wednesday */
  {0x2132, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule thursday */
  {0x2133, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule thursday */
  {0x2134, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule thursday */
  {0x2135, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule thursday */
  {0x2136, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule friday */
  {0x2137, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule friday */
  {0x2138, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule friday */
  {0x2139, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule friday */
  {0x213A, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule saturday */
  {0x213B, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule saturday */
  {0x213C, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule saturday */
  {0x213D, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule saturday */
  {0x213E, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule sunday */
  {0x213F, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule sunday */
  {0x2140, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule sunday */
  {0x2141, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* CH schedule sunday */
  {0x2142, TT_U8, PM_SETTING, 0LL, 1LL, WE_OPTION_INDEX, 1.0f}, /* CH mode */
  {0x2143, TT_U8, PM_SETTING, 0LL, 1LL, WE_OPTION_INDEX, 1.0f}, /* CH mode */
  {0x2144, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* CH mode */
  {0x2145, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* CH mode */
  {0x2146, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* Tch set manual [C] */
  {0x2147, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* Tch set manual [C] */
  {0x2148, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* Tch set manual [C] */
  {0x2149, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* Tch set manual [C] */
  {0x214A, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer test */
  {0x214B, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer test */
  {0x214C, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer test */
  {0x214D, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer test */
  {0x214E, TT_U8, PM_SETTING, 60LL, 150LL, WE_SCALED, 0.1f}, /* Oxygen for 30% of power [%] */
  {0x2151, TT_U8, PM_SETTING, 0LL, 1LL, WE_BLOCKED, 1.0f}, /* Grate silence mode */
  {0x2152, TT_U8, PM_SETTING, 80LL, 150LL, WE_SCALED, 0.1f}, /* Oxygen for 100% of power [%] */
  {0x2153, TT_U8, PM_SETTING, 1LL, 60LL, WE_PLAIN, 1.0f}, /* Burner inertia [min] */
  {0x2154, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Hot water 1 schedule monday */
  {0x2155, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Hot water 1 schedule tuesday */
  {0x2156, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Hot water 1 schedule wednesday */
  {0x2157, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Hot water 1 schedule thursday */
  {0x2158, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Hot water 1 schedule friday */
  {0x2159, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Hot water 1 schedule saturday */
  {0x215A, TT_U16, PM_SETTING, 5LL, 500LL, WE_BLOCKED, 1.0f}, /* Airflow speed for 30% of power [m/s] */
  {0x215B, TT_U16, PM_SETTING, 10LL, 500LL, WE_BLOCKED, 1.0f}, /* Airflow speed for 100% of power [m/s] */
  {0x215F, TT_S16, PM_LIVE, -500LL, 1000LL, WE_NONE, 1.0f}, /* Outside temperature */
  {0x2160, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Hot water 1 schedule sunday */
  {0x2161, TT_U16, PM_COMMAND, 0LL, 65535LL, WE_NONE, 1.0f}, /* Weather forecast 0 */
  {0x2162, TT_U16, PM_COMMAND, 0LL, 65535LL, WE_NONE, 1.0f}, /* Weather forecast 1 */
  {0x2163, TT_U16, PM_COMMAND, 0LL, 65535LL, WE_NONE, 1.0f}, /* Weather forecast 2 */
  {0x2164, TT_U8, PM_COMMAND, 0LL, 255LL, WE_NONE, 1.0f}, /* Weather validity left time [min] */
  {0x2165, TT_U32, PM_COMMAND, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Compatibility timestamp */
  {0x2166, TT_S16, PM_COMMAND, 0LL, 500LL, WE_NONE, 1.0f}, /* CH res [dC] */
  {0x2167, TT_S16, PM_COMMAND, 0LL, 500LL, WE_NONE, 1.0f}, /* CH res [dC] */
  {0x2168, TT_S16, PM_COMMAND, 0LL, 500LL, WE_NONE, 1.0f}, /* CH res [dC] */
  {0x2169, TT_S16, PM_COMMAND, 0LL, 500LL, WE_NONE, 1.0f}, /* CH res [dC] */
  {0x216A, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Exchanger cleaning request */
  {0x216B, TT_U8, PM_SETTING, 0LL, 1LL, WE_BLOCKED, 1.0f}, /* Exchanger silence mode */
  {0x216C, TT_U16, PM_SETTING, 35LL, 75LL, WE_PLAIN, 1.0f}, /* Boiler minimal return temperature */
  {0x216D, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Boiler return mixer test */
  {0x216E, TT_U16, PM_SETTING, 15LL, 360LL, WE_PLAIN, 1.0f}, /* Boiler mixer opening time [s] */
  {0x216F, TT_U8, PM_LIVE, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer state */
  {0x2170, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* VideRoom PIN */
  {0x218A, TT_U8, PM_SETTING, 1LL, 1LL, WE_BLOCKED, 1.0f}, /* Is grate cleaning on */
  {0x2207, TT_U16, PM_SETTING, 20LL, 70LL, WE_PLAIN, 1.0f}, /* Boiler minimal pump temperature */
  {0x2208, TT_U16, PM_SETTING, 0LL, 20LL, WE_PLAIN, 1.0f}, /* Boiler on histeresis */
  {0x2209, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Boiler schedule monday */
  {0x220A, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Boiler schedule tuesday */
  {0x220B, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Boiler schedule wednesday */
  {0x220C, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Boiler schedule thursday */
  {0x220D, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Boiler schedule friday */
  {0x220E, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Boiler schedule saturday */
  {0x220F, TT_U32, PM_SETTING, 0LL, 16777215LL, WE_PLAIN, 1.0f}, /* Boiler schedule sunday */
  {0x2210, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Hot water 1 heat now */
  {0x2211, TT_U16, PM_LIVE, 0LL, 150LL, WE_NONE, 1.0f}, /*  */
  {0x2212, TT_U16, PM_SETTING, 5LL, 30LL, WE_PLAIN, 1.0f}, /* Hot water 1 histeresis */
  {0x2214, TT_U8, PM_SETTING, 0LL, 1LL, WE_OPTION_INDEX, 1.0f}, /* Hot water 1 source */
  {0x2216, TT_U8, PM_SETTING, 2LL, 30LL, WE_PLAIN, 1.0f}, /* Hot water 1 delta source */
  {0x2218, TT_U8, PM_SETTING, 2LL, 20LL, WE_PLAIN, 1.0f}, /* Hot water 1 delta min temperature */
  {0x2250, TT_U16, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Burner brightness */
  {0x2251, TT_U8, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Burner storage feeder */
  {0x2252, TT_U8, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Burner burner feeder */
  {0x2253, TT_U16, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Burner igniter */
  {0x2254, TT_U8, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Burner blower */
  {0x2256, TT_U16, PM_SETTING, 10LL, 200LL, WE_PLAIN, 1.0f}, /* Pellet predose [s] */
  {0x2257, TT_U16, PM_SETTING, 10LL, 150LL, WE_SCALED, 0.1f}, /* Max Feeding [s] */
  {0x2258, TT_U8, PM_SETTING, 1LL, 3LL, WE_PLAIN, 1.0f}, /* Grate cleaning number */
  {0x2259, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Burner storage feeder test */
  {0x225A, TT_U8, PM_SETTING, 2LL, 30LL, WE_PLAIN, 1.0f}, /* Pwr reg. speed */
  {0x2260, TT_U16, PM_LIVE, 0LL, 3600LL, WE_NONE, 1.0f}, /* Burner storage feeder work time during refuling [s] */
  {0x2261, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* blower char. 1 */
  {0x2262, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* blower char. 2 */
  {0x2263, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* blower char. 3 */
  {0x2264, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* blower char. 4 */
  {0x2265, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* blower char. 5 */
  {0x2266, TT_U8, PM_COMMAND, 0LL, 1LL, WE_NONE, 1.0f}, /* Refueling need request */
  {0x2267, TT_U16, PM_LIVE, 0LL, 3600LL, WE_NONE, 1.0f}, /* Burner storage feeder left time during refuling [s] */
  {0x2268, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* exh. fan char. 1 */
  {0x226A, TT_U8, PM_SETTING, 5LL, 100LL, WE_PLAIN, 1.0f}, /* Brightness threshold */
  {0x226B, TT_U8, PM_SETTING, 2LL, 6LL, WE_PLAIN, 1.0f}, /* Burning off time [min] */
  {0x226C, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Burner feeder temperature [dC] */
  {0x226D, TT_U16, PM_LIVE, 0LL, 1000LL, WE_NONE, 1.0f}, /* Burner storage feeding time [dS] */
  {0x226E, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* exh. fan char. 2 */
  {0x226F, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* exh. fan char. 3 */
  {0x2270, TT_U8, PM_SETTING, 30LL, 100LL, WE_PLAIN, 1.0f}, /* Min power */
  {0x2271, TT_U8, PM_SETTING, 30LL, 100LL, WE_PLAIN, 1.0f}, /* Max power */
  {0x2272, TT_U16, PM_SETTING, 50LL, 1000LL, WE_SCALED, 0.1f}, /* Feeder rate [dkg/h] */
  {0x2273, TT_S16, PM_SETTING, -10LL, 40LL, WE_PLAIN, 1.0f}, /* Burner blower corretion */
  {0x2275, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* exh. fan char. 4 */
  {0x2276, TT_U8, PM_LIVE, 0LL, 2LL, WE_NONE, 1.0f}, /* Grate move indicator */
  {0x2277, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Boiler pump state */
  {0x2278, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Hot water pump state */
  {0x2279, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Burner grate test */
  {0x227A, TT_U16, PM_SETTING, 0LL, 100LL, WE_NONE, 1.0f}, /* exh. fan char. 5 */
  {0x227B, TT_U16, PM_SETTING, 0LL, 2000LL, WE_NONE, 1.0f}, /* Pwr reg. Kp */
  {0x227C, TT_U16, PM_SETTING, 0LL, 2000LL, WE_NONE, 1.0f}, /* Pwr reg. Ki */
  {0x227D, TT_U16, PM_SETTING, 0LL, 10000LL, WE_NONE, 1.0f}, /* Pwr reg. Kd */
  {0x227E, TT_U16, PM_SETTING, 0LL, 2000LL, WE_NONE, 1.0f}, /* Pwr reg. Ki 2 */
  {0x227F, TT_U16, PM_SETTING, 0LL, 10000LL, WE_NONE, 1.0f}, /* Pwr reg. Kd 2 */
  {0x2280, TT_U16, PM_LIVE, 25LL, 80LL, WE_NONE, 1.0f}, /* Buffer top set temperature */
  {0x2281, TT_U8, PM_SETTING, 15LL, 60LL, WE_PLAIN, 1.0f}, /* Buffer minimal pump temperature */
  {0x2282, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Buffer is automatic top temperature */
  {0x2283, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Blower test */
  {0x2284, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Igniter test */
  {0x2285, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Exchanger cleaner test */
  {0x2286, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Boiler pump test */
  {0x2288, TT_U8, PM_COMMAND, 0LL, 100LL, WE_NONE, 1.0f}, /* Blower test power */
  {0x2289, TT_U8, PM_SETTING, 20LL, 100LL, WE_PLAIN, 1.0f}, /* Blower power during fire up */
  {0x228A, TT_U16, PM_SETTING, 0LL, 2000LL, WE_NONE, 1.0f}, /* Pwr reg. Kp 2 */
  {0x228B, TT_U16, PM_SETTING, 3LL, 500LL, WE_NONE, 1.0f}, /* Grate full move time [s] */
  {0x228C, TT_U8, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Exhaust fan power */
  {0x2290, TT_U8, PM_SETTING, 1LL, 10LL, WE_PLAIN, 1.0f}, /* Incandescing time [min] */
  {0x2291, TT_U16, PM_SETTING, 0LL, 20LL, WE_PLAIN, 1.0f}, /* Boiler off histeresis */
  {0x2292, TT_U8, PM_SETTING, 20LL, 100LL, WE_SCALED, 0.1f}, /* Fuel calorific [kWh/kg] */
  {0x2293, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Exhaust fan test */
  {0x2294, TT_U16, PM_SETTING, 0LL, 65535LL, WE_NONE, 1.0f}, /* User fuel counter[kg] */
  {0x2295, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Burner feeder test */
  {0x2296, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* HW1 pump test */
  {0x2297, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* HW1 anti legionella */
  {0x2298, TT_U8, PM_COMMAND, 0LL, 100LL, WE_NONE, 1.0f}, /* Exhaust fan test power */
  {0x2299, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH pump state */
  {0x229A, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH pump state */
  {0x229B, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH pump state */
  {0x229C, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH pump state */
  {0x229D, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH pump test */
  {0x229E, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH pump test */
  {0x229F, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH pump test */
  {0x22A0, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* CH pump test */
  {0x22A1, TT_U8, PM_SETTING, 0LL, 5LL, WE_OPTION_INDEX, 1.0f}, /* Room temp sensor type */
  {0x22A2, TT_U8, PM_SETTING, 0LL, 5LL, WE_OPTION_INDEX, 1.0f}, /* Room temp sensor type */
  {0x22A3, TT_U8, PM_SETTING, 0LL, 5LL, WE_NONE, 1.0f}, /* Room temp sensor type */
  {0x22A4, TT_U8, PM_SETTING, 0LL, 5LL, WE_NONE, 1.0f}, /* Room temp sensor type */
  {0x22A5, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* CH sensor use */
  {0x22A6, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* CH sensor use */
  {0x22A7, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* CH sensor use */
  {0x22A8, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* CH sensor use */
  {0x22AD, TT_S16, PM_LIVE, 10LL, 90LL, WE_NONE, 1.0f}, /* CH temp calc */
  {0x22AE, TT_S16, PM_LIVE, 10LL, 90LL, WE_NONE, 1.0f}, /* CH temp calc */
  {0x22AF, TT_S16, PM_LIVE, 10LL, 90LL, WE_NONE, 1.0f}, /* CH temp calc */
  {0x22B0, TT_S16, PM_LIVE, 10LL, 90LL, WE_NONE, 1.0f}, /* CH temp calc */
  {0x22B1, TT_U8, PM_SETTING, 0LL, 30LL, WE_PLAIN, 1.0f}, /* CH delta source [C] */
  {0x22B2, TT_U8, PM_SETTING, 0LL, 30LL, WE_PLAIN, 1.0f}, /* CH delta source [C] */
  {0x22B3, TT_U8, PM_SETTING, 0LL, 30LL, WE_NONE, 1.0f}, /* CH delta source [C] */
  {0x22B4, TT_U8, PM_SETTING, 0LL, 30LL, WE_NONE, 1.0f}, /* CH delta source [C] */
  {0x22B5, TT_U16, PM_SETTING, 60LL, 360LL, WE_PLAIN, 1.0f}, /* CH mixer opening time [s] */
  {0x22B6, TT_U16, PM_SETTING, 60LL, 360LL, WE_PLAIN, 1.0f}, /* CH mixer opening time [s] */
  {0x22B7, TT_U16, PM_SETTING, 60LL, 360LL, WE_NONE, 1.0f}, /* CH mixer opening time [s] */
  {0x22B8, TT_U16, PM_SETTING, 60LL, 360LL, WE_NONE, 1.0f}, /* CH mixer opening time [s] */
  {0x22B9, TT_U8, PM_LIVE, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer state */
  {0x22BA, TT_U8, PM_LIVE, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer state */
  {0x22BB, TT_U8, PM_LIVE, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer state */
  {0x22BC, TT_U8, PM_LIVE, 0LL, 2LL, WE_NONE, 1.0f}, /* CH mixer state */
  {0x22BD, TT_U8, PM_SETTING, 0LL, 50LL, WE_SCALED, 0.1f}, /* Room temp hysteresis [C] */
  {0x22BE, TT_U8, PM_SETTING, 0LL, 50LL, WE_SCALED, 0.1f}, /* Room temp hysteresis [C] */
  {0x22BF, TT_U8, PM_SETTING, 0LL, 50LL, WE_NONE, 1.0f}, /* Room temp hysteresis [C] */
  {0x22C0, TT_U8, PM_SETTING, 0LL, 50LL, WE_NONE, 1.0f}, /* Room temp hysteresis [C] */
  {0x22C1, TT_S16, PM_SETTING, -30LL, 30LL, WE_SCALED, 0.1f}, /* Room sensor cal [C] */
  {0x22C2, TT_S16, PM_SETTING, -30LL, 30LL, WE_SCALED, 0.1f}, /* Room sensor cal [C] */
  {0x22C3, TT_S16, PM_SETTING, -30LL, 30LL, WE_NONE, 1.0f}, /* Room sensor cal [C] */
  {0x22C4, TT_S16, PM_SETTING, -30LL, 30LL, WE_NONE, 1.0f}, /* Room sensor cal [C] */
  {0x22C5, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for -20C [C] */
  {0x22C6, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for -20C [C] */
  {0x22C7, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for -20C [C] */
  {0x22C8, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for -20C [C] */
  {0x22C9, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for 0C [C] */
  {0x22CA, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for 0C [C] */
  {0x22CB, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for 0C [C] */
  {0x22CC, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for 0C [C] */
  {0x22CD, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for -20C [C] */
  {0x22CE, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for -20C [C] */
  {0x22CF, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for -20C [C] */
  {0x22D0, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for -20C [C] */
  {0x22D1, TT_U8, PM_SETTING, 0LL, 1LL, WE_OPTION_INDEX, 1.0f}, /* CH source */
  {0x22D2, TT_U8, PM_SETTING, 0LL, 1LL, WE_OPTION_INDEX, 1.0f}, /* CH source */
  {0x22D3, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* CH source */
  {0x22D4, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* CH source */
  {0x22D5, TT_U8, PM_SETTING, 30LL, 80LL, WE_PLAIN, 1.0f}, /* CH max temp [C] */
  {0x22D6, TT_U8, PM_SETTING, 30LL, 80LL, WE_PLAIN, 1.0f}, /* CH max temp [C] */
  {0x22D7, TT_U8, PM_SETTING, 30LL, 80LL, WE_NONE, 1.0f}, /* CH max temp [C] */
  {0x22D8, TT_U8, PM_SETTING, 30LL, 80LL, WE_NONE, 1.0f}, /* CH max temp [C] */
  {0x22D9, TT_U8, PM_SETTING, 0LL, 20LL, WE_PLAIN, 1.0f}, /* CH correction factor [C] */
  {0x22DA, TT_U8, PM_SETTING, 0LL, 20LL, WE_PLAIN, 1.0f}, /* CH correction factor [C] */
  {0x22DB, TT_U8, PM_SETTING, 0LL, 20LL, WE_NONE, 1.0f}, /* CH correction factor [C] */
  {0x22DC, TT_U8, PM_SETTING, 0LL, 20LL, WE_NONE, 1.0f}, /* CH correction factor [C] */
  {0x22E5, TT_U8, PM_SETTING, 0LL, 40LL, WE_PLAIN, 1.0f}, /* CH temp lowering [C] */
  {0x22E6, TT_U8, PM_SETTING, 0LL, 40LL, WE_PLAIN, 1.0f}, /* CH temp lowering [C] */
  {0x22E7, TT_U8, PM_SETTING, 0LL, 40LL, WE_NONE, 1.0f}, /* CH temp lowering [C] */
  {0x22E8, TT_U8, PM_SETTING, 0LL, 40LL, WE_NONE, 1.0f}, /* CH temp lowering [C] */
  {0x22E9, TT_U8, PM_SETTING, 20LL, 60LL, WE_PLAIN, 1.0f}, /* Min CH temp for pump [C] */
  {0x22EA, TT_U8, PM_SETTING, 20LL, 60LL, WE_PLAIN, 1.0f}, /* Min CH temp for pump [C] */
  {0x22EB, TT_U8, PM_SETTING, 20LL, 60LL, WE_NONE, 1.0f}, /* Min CH temp for pump [C] */
  {0x22EC, TT_U8, PM_SETTING, 20LL, 60LL, WE_NONE, 1.0f}, /* Min CH temp for pump [C] */
  {0x22F1, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Permament pump */
  {0x22F2, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Permament pump */
  {0x22F3, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Permament pump */
  {0x22F4, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Permament pump */
  {0x22F5, TT_U16, PM_LIVE, 0LL, 150LL, WE_NONE, 1.0f}, /* CH needed temp. [C] */
  {0x22F6, TT_U16, PM_LIVE, 0LL, 150LL, WE_NONE, 1.0f}, /* CH needed temp. [C] */
  {0x22F7, TT_U16, PM_LIVE, 0LL, 150LL, WE_NONE, 1.0f}, /* CH needed temp. [C] */
  {0x22F8, TT_U16, PM_LIVE, 0LL, 150LL, WE_NONE, 1.0f}, /* CH needed temp. [C] */
  {0x22F9, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Tch lowering for energ. consumption reduction */
  {0x22FA, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Tch lowering for energ. consumption reduction */
  {0x22FB, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Tch lowering for energ. consumption reduction */
  {0x22FC, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Tch lowering for energ. consumption reduction */
  {0x22FD, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CH status */
  {0x22FE, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CH status */
  {0x22FF, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CH status */
  {0x2300, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CH status */
  {0x2301, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH thermostat state */
  {0x2302, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH thermostat state */
  {0x2303, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH thermostat state */
  {0x2304, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* CH thermostat state */
  {0x2305, TT_S16, PM_COMMAND, -110LL, 40LL, WE_NONE, 1.0f}, /* CH room set temp. adjustment [dC] */
  {0x2306, TT_S16, PM_COMMAND, -110LL, 40LL, WE_NONE, 1.0f}, /* CH room set temp. adjustment [dC] */
  {0x2307, TT_S16, PM_COMMAND, -110LL, 40LL, WE_NONE, 1.0f}, /* CH room set temp. adjustment [dC] */
  {0x2308, TT_S16, PM_COMMAND, -110LL, 40LL, WE_NONE, 1.0f}, /* CH room set temp. adjustment [dC] */
  {0x2309, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor batt. volt [mV] */
  {0x230A, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor batt. volt [mV] */
  {0x230B, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor batt. volt [mV] */
  {0x230C, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor batt. volt [mV] */
  {0x230D, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor signal [%] */
  {0x230E, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor signal [%] */
  {0x230F, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor signal [%] */
  {0x2310, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor signal [%] */
  {0x2311, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor time since lasr rec. */
  {0x2312, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor time since lasr rec. */
  {0x2313, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor time since lasr rec. */
  {0x2314, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Room temp. radio sensor time since lasr rec. */
  {0x2315, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Automatic summer recognition */
  {0x2316, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Automatic summer recognition */
  {0x2317, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Automatic summer recognition */
  {0x2318, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Automatic summer recognition */
  {0x2319, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. before dot */
  {0x231A, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. before dot */
  {0x231B, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. before dot */
  {0x231C, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. before dot */
  {0x231D, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. after dot */
  {0x231E, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. after dot */
  {0x231F, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. after dot */
  {0x2320, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Room temp. radio sensor soft. ver. after dot */
  {0x2321, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_OPTION_INDEX, 1.0f}, /* CH circuit name part1 */
  {0x2322, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part1 */
  {0x2323, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part1 */
  {0x2324, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part1 */
  {0x2325, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part2 */
  {0x2326, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part2 */
  {0x2327, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part2 */
  {0x2328, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part2 */
  {0x2329, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part3 */
  {0x232A, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part3 */
  {0x232B, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part3 */
  {0x232C, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* CH circuit name part3 */
  {0x2999, TT_U16, PM_SETTING, 1LL, 12LL, WE_PLAIN, 1.0f}, /* Burner max continuous work time [h] */
  {0x299A, TT_U8, PM_SETTING, 20LL, 50LL, WE_PLAIN, 1.0f}, /* Incandenscing fuel [%] */
  {0x299B, TT_U8, PM_SETTING, 3LL, 30LL, WE_PLAIN, 1.0f}, /*  */
  {0x299C, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Burner blower RPM */
  {0x299D, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* HALL Activated */
  {0x299E, TT_U8, PM_SETTING, 0LL, 2LL, WE_OPTION_INDEX, 1.0f}, /* Boiler pump work mode */
  {0x299F, TT_U8, PM_SETTING, 0LL, 2LL, WE_BLOCKED, 1.0f}, /* Burning type */
  {0x3000, TT_U8, PM_SETTING, 10LL, 100LL, WE_BLOCKED, 1.0f}, /* Modulation blower power on wood [%] */
  {0x3001, TT_U8, PM_COMMAND, 0LL, 1LL, WE_NONE, 1.0f}, /* Start burning wood */
  {0x3003, TT_U16, PM_SETTING, 0LL, 200LL, WE_SCALED, 0.1f}, /* Grate move in modulation [0.1s] */
  {0x3004, TT_U8, PM_COMMAND, 7LL, 150LL, WE_NONE, 1.0f}, /* Virt. energy consumer requested temp [C] */
  {0x3005, TT_U8, PM_SETTING, 5LL, 100LL, WE_BLOCKED, 1.0f}, /* Fireup blower power on wood [%] */
  {0x3006, TT_U8, PM_SETTING, 1LL, 100LL, WE_BLOCKED, 1.0f}, /* Incandescing blower power on wood [%] */
  {0x3007, TT_U8, PM_SETTING, 0LL, 50LL, WE_BLOCKED, 1.0f}, /* Pause blower power on wood [%] */
  {0x3008, TT_U8, PM_SETTING, 1LL, 100LL, WE_BLOCKED, 1.0f}, /* Sec. air modulation MIN [%] */
  {0x3009, TT_U8, PM_SETTING, 1LL, 100LL, WE_BLOCKED, 1.0f}, /* Sec. air modulation MAX [%] */
  {0x300A, TT_U8, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Sec. air mixer position [%] */
  {0x300B, TT_U8, PM_COMMAND, 0LL, 1LL, WE_NONE, 1.0f}, /* Stop burning wood */
  {0x300C, TT_U8, PM_SETTING, 25LL, 100LL, WE_PLAIN, 1.0f}, /* Blower power in burning off [%] */
  {0x300D, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Hot water priority */
  {0x300E, TT_U16, PM_SETTING, 0LL, 5LL, WE_BLOCKED, 1.0f}, /* Burner blower minimal RPM [*100] */
  {0x300F, TT_U8, PM_COMMAND, 0LL, 100LL, WE_NONE, 1.0f}, /* Virt. consumer maximal power */
  {0x3010, TT_U8, PM_SETTING, 1LL, 20LL, WE_BLOCKED, 1.0f}, /* Grate move interval in modulation [kg] */
  {0x3011, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Burner pwr. depend of buf. energy level */
  {0x3012, TT_U16, PM_SETTING, 1LL, 720LL, WE_BLOCKED, 1.0f}, /* Grate move interval in modulation [min] */
  {0x3013, TT_U16, PM_SETTING, 500LL, 5000LL, WE_NONE, 1.0f}, /* Burner blower max RPM */
  {0x3116, TT_U8, PM_SETTING, 0LL, 20LL, WE_PLAIN, 1.0f}, /* Solar delta on [C] */
  {0x3117, TT_U8, PM_SETTING, 0LL, 10LL, WE_PLAIN, 1.0f}, /* Solar delta off [C] */
  {0x3118, TT_U16, PM_SETTING, 15LL, 60LL, WE_SCALED, 0.1f}, /* Solar liquid specific heat [kJ/(kg * C)] */
  {0x3119, TT_S16, PM_SETTING, 70LL, 95LL, WE_PLAIN, 1.0f}, /* Solar temperature water max [C] */
  {0x311A, TT_S16, PM_SETTING, 90LL, 145LL, WE_PLAIN, 1.0f}, /* Solar temperature alarm max[C] */
  {0x311B, TT_S16, PM_SETTING, -40LL, 0LL, WE_PLAIN, 1.0f}, /* Solar temperature alarm min[C] */
  {0x311C, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Solar pump state */
  {0x311F, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* Solar pump test */
  {0x3120, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Solar status */
  {0x3121, TT_U16, PM_SETTING, 0LL, 30LL, WE_BLOCKED, 1.0f}, /* Exchanger cleaning duration [s] */
  {0x3122, TT_U16, PM_SETTING, 10LL, 600LL, WE_BLOCKED, 1.0f}, /* Exchanger cleaning interval [min] */
  {0x3123, TT_U8, PM_SETTING, 0LL, 1LL, WE_BLOCKED, 1.0f}, /*  */
  {0x3124, TT_U8, PM_SETTING, 0LL, 1LL, WE_BLOCKED, 1.0f}, /*  */
  {0x3125, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /*  */
  {0x3126, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /*  */
  {0x3127, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Clean burner monit period [kg] */
  {0x3128, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Clean exchanger monit period [kg] */
  {0x3129, TT_U8, PM_SETTING, 1LL, 15LL, WE_SCALED, 0.1f}, /* Boiler min hydraulic pressure [0.1bar] */
  {0x312A, TT_U8, PM_SETTING, 4LL, 25LL, WE_SCALED, 0.1f}, /* Boiler max hydraulic pressure [0.1bar] */
  {0x3170, TT_U8, PM_SETTING, 2LL, 20LL, WE_PLAIN, 1.0f}, /* Buffer pump delta min temperature */
  {0x3171, TT_S16, PM_LIVE, -300LL, 1500LL, WE_NONE, 1.0f}, /* Buffer T1 measured temperature */
  {0x3172, TT_S16, PM_LIVE, -300LL, 1500LL, WE_NONE, 1.0f}, /* Buffer T2 measured temperature */
  {0x3173, TT_S16, PM_LIVE, -300LL, 1500LL, WE_NONE, 1.0f}, /* Buffer T3 measured temperature */
  {0x3174, TT_S16, PM_LIVE, -100LL, 200LL, WE_NONE, 1.0f}, /* Buffer internal energy [%] */
  {0x3333, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Boiler pressure[mbar] */
  {0x3500, TT_U8, PM_SETTING, 0LL, 1LL, WE_OPTION_INDEX, 1.0f}, /* Main module type */
  {0x3501, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Autoconfig activation */
  {0x3502, TT_U8, PM_SETTING, 0LL, 1LL, WE_OPTION_INDEX, 1.0f}, /* CH expansion module type */
  {0x3505, TT_U8, PM_SETTING, 0LL, 2LL, WE_OPTION_INDEX, 1.0f}, /* Outside temperature sensor configuration */
  {0x3506, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Sound alarm activation */
  {0x3507, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Total fuel counter[kg] */
  {0x3508, TT_U8, PM_SETTING, 0LL, 24LL, WE_PLAIN, 1.0f}, /* Silence hours from */
  {0x3509, TT_U8, PM_SETTING, 0LL, 24LL, WE_PLAIN, 1.0f}, /* Silence hours to */
  {0x350A, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Return to boiler temperature sensor */
  {0x350B, TT_U8, PM_SETTING, 1LL, 3LL, WE_PLAIN, 1.0f}, /* Profile number */
  {0x350C, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Is this device cascade master */
  {0x350D, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Pressure sensor */
  {0x3511, TT_U8, PM_SETTING, 1LL, 4LL, WE_PLAIN, 1.0f}, /* Cascade master power level */
  {0x3512, TT_U8, PM_SETTING, 1LL, 4LL, WE_PLAIN, 1.0f}, /* Cascade slave 1 power level */
  {0x3513, TT_U8, PM_SETTING, 1LL, 4LL, WE_PLAIN, 1.0f}, /* Cascade slave 2 power level */
  {0x3514, TT_U8, PM_SETTING, 1LL, 4LL, WE_PLAIN, 1.0f}, /* Cascade slave 3 power level */
  {0x3515, TT_U8, PM_SETTING, 0LL, 100LL, WE_PLAIN, 1.0f}, /* Cascade minimal energy [%] */
  {0x3516, TT_U8, PM_SETTING, 0LL, 100LL, WE_PLAIN, 1.0f}, /* Cascade maximal energy [%] */
  {0x3517, TT_U8, PM_SETTING, 0LL, 240LL, WE_PLAIN, 1.0f}, /* Cascade time to inc power lvel [min] */
  {0x3518, TT_U8, PM_SETTING, 0LL, 240LL, WE_PLAIN, 1.0f}, /* Cascade time to dec power lvel [min] */
  {0x3519, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Cascade current power lvel */
  {0x351A, TT_U8, PM_SETTING, 1LL, 3LL, WE_PLAIN, 1.0f}, /* Cascade slaves number */
  {0x351B, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Cascade timer to inc power lvel [s] */
  {0x351C, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Cascade timer to dec power lvel [s] */
  {0x351E, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* External fuel storage motor 1 test */
  {0x351F, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* External fuel storage motor 2 test */
  {0x3520, TT_U16, PM_SETTING, 5LL, 60LL, WE_PLAIN, 1.0f}, /* Ext. fuel storage motor 1 transport. time [s] */
  {0x3521, TT_U16, PM_SETTING, 0LL, 30LL, WE_PLAIN, 1.0f}, /* Ext. fuel storage time [s] to empty transport. hose */
  {0x3522, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* External fuel storage motor 1 state */
  {0x3523, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* External fuel storage motor 2 state */
  {0x3524, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* External fuel storage level sens. state */
  {0x3525, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Additional fuel storage */
  {0x3526, TT_U16, PM_SETTING, 3LL, 15LL, WE_PLAIN, 1.0f}, /* Ext. fuel storage motor 1 overrun time [s] */
  {0x3527, TT_U16, PM_SETTING, 1LL, 60LL, WE_PLAIN, 1.0f}, /* Ext. fuel storage feeder work time [s] */
  {0x3528, TT_U16, PM_SETTING, 0LL, 30LL, WE_PLAIN, 1.0f}, /* Ext. fuel storage feeder pause time [s] */
  {0x3529, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Ext. fuel storage reserve sensor */
  {0x352A, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Ext. fuel stor. reserve sens. state */
  {0x352B, TT_U8, PM_SETTING, 5LL, 100LL, WE_PLAIN, 1.0f}, /* Ext. fuel storage max cycles no. */
  {0x3600, TT_U16, PM_SETTING, 10LL, 1500LL, WE_PLAIN, 1.0f}, /* Tank capacity [kg] */
  {0x3601, TT_U16, PM_SETTING, 0LL, 65535LL, WE_NONE, 1.0f}, /* How much fuel left in tank [kg] */
  {0x3602, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Is fuel reserve */
  {0x3603, TT_U16, PM_SETTING, 0LL, 100LL, WE_PLAIN, 1.0f}, /* Fuel reserve [kg] */
  {0x3604, TT_U16, PM_SETTING, 0LL, 100LL, WE_PLAIN, 1.0f}, /* Fuel reserve [kg] */
  {0x3605, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Lambda oxygen control */
  {0x3606, TT_U8, PM_SETTING, 0LL, 2LL, WE_OPTION_INDEX, 1.0f}, /* Input IN17 config */
  {0x3607, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Reserve sensor state */
  {0x3608, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Statistics - Ignition cnt */
  {0x3609, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Statistics - Avg ignition time [s] */
  {0x360A, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* State on closing actuator out in stop */
  {0x3610, TT_U8, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Statistics - Avg burner power [%] */
  {0x3611, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Feeder 1 total work time [h] */
  {0x3612, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Feeder 2 total work time [h] */
  {0x3613, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Average oxygen for 30% pwr [%] */
  {0x3614, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Average oxygen for 50% pwr [%] */
  {0x3615, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Average oxygen for 70% pwr [%] */
  {0x3616, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Average oxygen for 90% pwr [%] */
  {0x3617, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_BLOCKED, 1.0f}, /* Statistics - Grate cleaning count */
  {0x3618, TT_U32, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Statistics - Exchanger cleaning count */
  {0x3619, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Statistics - Boiler overheat count */
  {0x3620, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 5 */
  {0x3621, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 6 */
  {0x3622, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 7 */
  {0x3623, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 4 */
  {0x3624, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 3 */
  {0x3625, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 2 */
  {0x3626, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 1 */
  {0x3627, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO 0 */
  {0x3628, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Statistics - total energy produced [kWh] */
  {0x3629, TT_U8, PM_SETTING, 0LL, 2LL, WE_OPTION_INDEX, 1.0f}, /* Energy meter type */
  {0x362A, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Statistics - reset count */
  {0x362B, TT_U8, PM_SETTING, 0LL, 1LL, WE_BLOCKED, 1.0f}, /* Airflow meter */
  {0x362C, TT_U16, PM_LIVE, 0LL, 65535LL, WE_BLOCKED, 1.0f}, /* Total grate unblock try count */
  {0x3630, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 0 sw. ver. before dot */
  {0x3631, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 1 sw. ver. before dot */
  {0x3632, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 2 sw. ver. before dot */
  {0x3633, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 3 sw. ver. before dot */
  {0x3634, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 4 sw. ver. before dot */
  {0x3635, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 5 sw. ver. before dot */
  {0x3636, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 6 sw. ver. before dot */
  {0x3637, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 7 sw. ver. before dot */
  {0x3638, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 0 sw. ver. after dot */
  {0x3639, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 1 sw. ver. after dot */
  {0x363A, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 2 sw. ver. after dot */
  {0x363B, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 3 sw. ver. after dot */
  {0x363C, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 4 sw. ver. after dot */
  {0x363D, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 5 sw. ver. after dot */
  {0x363E, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 6 sw. ver. after dot */
  {0x363F, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO 7 sw. ver. after dot */
  {0x3640, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Inside temp [C] CAN IO Mini 0 */
  {0x3648, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO Mini 0 sw. ver. before dot */
  {0x3650, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO Mini 0 sw. ver. after dot */
  {0x3658, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO Big sw. ver. before dot */
  {0x3659, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* CAN IO Big sw. ver. after dot */
  {0x365A, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Lambda module sw. ver. before dot */
  {0x365B, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Lambda module sw. ver. after dot */
  {0x365C, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 1 sw. ver. before dot */
  {0x365D, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 2 sw. ver. before dot */
  {0x365E, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 3 sw. ver. before dot */
  {0x365F, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 4 sw. ver. before dot */
  {0x3660, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 1 sw. ver. after dot */
  {0x3661, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 2 sw. ver. after dot */
  {0x3662, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 3 sw. ver. after dot */
  {0x3663, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Vide room 4 sw. ver. after dot */
  {0x3664, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* soft. version */
  {0x3665, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* igneTouch boiler short name */
  {0x3666, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Airflowmeter 1 sw. ver. before dot */
  {0x3667, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Airflowmeter 1 sw. ver. after dot */
  {0x366A, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* RadioGateway sw. ver. before dot */
  {0x366B, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* RadioGateway sw. ver. after dot */
  {0x3700, TT_U16, PM_SETTING, 5LL, 180LL, WE_PLAIN, 1.0f}, /* Storage feeder period [s] */
  {0x3701, TT_U16, PM_SETTING, 70LL, 120LL, WE_BLOCKED, 1.0f}, /* Min EGT required [C] */
  {0x3702, TT_U16, PM_SETTING, 90LL, 240LL, WE_BLOCKED, 1.0f}, /* Nominal exhaust gas temp. [C] */
  {0x3703, TT_U16, PM_SETTING, 180LL, 280LL, WE_BLOCKED, 1.0f}, /* Max exhaust gas temp. [C] */
  {0x3704, TT_U8, PM_SETTING, 30LL, 100LL, WE_PLAIN, 1.0f}, /* Blower power in incandescing [%] */
  {0x3705, TT_U8, PM_SETTING, 10LL, 100LL, WE_BLOCKED, 1.0f}, /* Blower power in hold/pause [%] */
  {0x3706, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Current burner power [dkW] */
  {0x3707, TT_U8, PM_SETTING, 0LL, 1LL, WE_BLOCKED, 1.0f}, /* EGT sensor type */
  {0x3708, TT_U8, PM_SETTING, 0LL, 120LL, WE_PLAIN, 1.0f}, /* Boiler pump overrun [min] */
  {0x3709, TT_U16, PM_SETTING, 4LL, 200LL, WE_BLOCKED, 1.0f}, /* Nominal power [kW] */
  {0x370A, TT_U8, PM_SETTING, 1LL, 3LL, WE_NONE, 1.0f}, /* Boiler image selector */
  {0x370B, TT_U16, PM_SETTING, 50LL, 500LL, WE_PLAIN, 1.0f}, /* Burner feeder % of stor. feeder */
  {0x370C, TT_U8, PM_SETTING, 1LL, 6LL, WE_BLOCKED, 1.0f}, /* Min fire up time [min] */
  {0x370D, TT_U16, PM_SETTING, 0LL, 300LL, WE_BLOCKED, 1.0f}, /* Igniter preheat time [s] */
  {0x370E, TT_U16, PM_SETTING, 0LL, 500LL, WE_BLOCKED, 1.0f}, /* Pellet predose [g] */
  {0x370F, TT_U16, PM_SETTING, 0LL, 180LL, WE_BLOCKED, 1.0f}, /* Burning off burner feeder work time [s] */
  {0x3710, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Fuel consumption [Dkg/h] */
  {0x3711, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Time to grate cleaning [min] */
  {0x3712, TT_U8, PM_MIRROR, 0LL, 255LL, WE_NONE, 1.0f}, /* Blower type */
  {0x3713, TT_U16, PM_SETTING, 30LL, 240LL, WE_BLOCKED, 1.0f}, /*  */
  {0x3714, TT_U8, PM_SETTING, 1LL, 30LL, WE_BLOCKED, 1.0f}, /*  */
  {0x3715, TT_U8, PM_SETTING, 0LL, 120LL, WE_PLAIN, 1.0f}, /* Burner pause time [min] */
  {0x3716, TT_U8, PM_SETTING, 0LL, 30LL, WE_PLAIN, 1.0f}, /* Burner pause power [%] */
  {0x3717, TT_U8, PM_SETTING, 0LL, 50LL, WE_PLAIN, 1.0f}, /* Burner pause blower power [%] */
  {0x3718, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Next service date timestamp */
  {0x3719, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Confirm. service date timestamp */
  {0x371A, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Airflowmeter 1 flow */
  {0x371B, TT_U16, PM_SETTING, 10LL, 1000LL, WE_NONE, 1.0f}, /* Min airflow (blower 100%) [dm/s] */
  {0x371C, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Boiler efficiency [d%] */
  {0x371D, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Oxygen target [0.1%] */
  {0x371E, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Airflowmeter 2 flow */
  {0x371F, TT_S16, PM_LIVE, -32768LL, 32767LL, WE_NONE, 1.0f}, /* Lambda oxygen averaged [%] */
  {0x3720, TT_U8, PM_SETTING, 0LL, 48LL, WE_PLAIN, 1.0f}, /* Outside temp. averaging period [h] */
  {0x3721, TT_S16, PM_LIVE, -500LL, 1000LL, WE_NONE, 1.0f}, /* Outside temperature averaged */
  {0x3722, TT_U8, PM_COMMAND, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Start settings recovery */
  {0x3723, TT_U16, PM_COMMAND, 0LL, 65535LL, WE_NONE, 1.0f}, /* Settings recovered count */
  {0x3724, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Out temp. radio sensor batt. volt [mV] */
  {0x3725, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Out temp. radio sensor signal [%] */
  {0x3726, TT_U16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Out temp. radio sensor time since lasr rec. */
  {0x3727, TT_U8, PM_SETTING, 0LL, 60LL, WE_BLOCKED, 1.0f}, /* Burner feeder absolut work time [s] */
  {0x3728, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Is reset required */
  {0x3729, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Is summer time */
  {0x372A, TT_U8, PM_SETTING, 5LL, 25LL, WE_PLAIN, 1.0f}, /* Summer time threshold temp. [C] */
  {0x372B, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Out temp. radio sensor sw. ver. before dot */
  {0x372C, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Out temp. radio sensor sw. ver. after dot */
  {0x372D, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /*  */
  {0x372E, TT_U32, PM_LIVE, 70000LL, 130000LL, WE_NONE, 1.0f}, /* [m%] */
  {0x372F, TT_U8, PM_SETTING, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Is automatic time synchronization */
  {0x3730, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* SSID part 1 */
  {0x3731, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* SSID part 2 */
  {0x3732, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* SSID part 3 */
  {0x3733, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* SSID part 4 */
  {0x3734, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* SSID part 5 */
  {0x3735, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* SSID part 6 */
  {0x3736, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* KEY part 1 */
  {0x3737, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* KEY part 2 */
  {0x3738, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* KEY part 3 */
  {0x3739, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* KEY part 4 */
  {0x373A, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* KEY part 5 */
  {0x373B, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* KEY part 6 */
  {0x373C, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x373D, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x373E, TT_U8, PM_SETTING, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x373F, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /*  */
  {0x3740, TT_U32, PM_LIVE, 0LL, 4294967295LL, WE_NONE, 1.0f}, /*  */
  {0x3741, TT_U8, PM_SETTING, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x3742, TT_U8, PM_COMMAND, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x3743, TT_U8, PM_COMMAND, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x3744, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x3745, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x3746, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /*  */
  {0x3747, TT_S16, PM_SETTING, -30LL, 30LL, WE_SCALED, 0.1f}, /* Outside calibration [dC] */
  {0x3748, TT_U8, PM_SETTING, 1LL, 8LL, WE_BLOCKED, 1.0f}, /* Storage feeder pause in predose[s] */
  {0x3749, TT_U8, PM_LIVE, 0LL, 1LL, WE_NONE, 1.0f}, /* Hot water circular pump state */
  {0x374A, TT_U8, PM_COMMAND, 0LL, 2LL, WE_NONE, 1.0f}, /* HW1 circular pump test */
  {0x374B, TT_U8, PM_SETTING, 0LL, 10LL, WE_PLAIN, 1.0f}, /* Hot water 1 circular pump fork time */
  {0x374C, TT_U8, PM_SETTING, 0LL, 60LL, WE_PLAIN, 1.0f}, /* Hot water 1 circular pump pause time */
  {0x374D, TT_U8, PM_SETTING, 0LL, 1LL, WE_NONE, 1.0f}, /* Burner plug disc. possible */
  {0x3750, TT_U8, PM_COMMAND, 0LL, 1LL, WE_BLOCKED, 1.0f}, /* Sweep mode */
  {0x4000, TT_S16, PM_LIVE, 0LL, 1500LL, WE_NONE, 1.0f}, /* Boiler return temp [dC] */
  {0x4001, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* CH temp measured */
  {0x4002, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* CH temp measured */
  {0x4003, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* CH temp measured */
  {0x4004, TT_S16, PM_LIVE, -100LL, 1500LL, WE_NONE, 1.0f}, /* CH temp measured */
  {0x4005, TT_U16, PM_LIVE, 0LL, 100LL, WE_NONE, 1.0f}, /* Burner power [%] */
  {0x4006, TT_S16, PM_LIVE, 0LL, 65535LL, WE_NONE, 1.0f}, /* Power regulator [%] */
  {0x4007, TT_U8, PM_LIVE, 0LL, 255LL, WE_NONE, 1.0f}, /* Blower power [%] */
  {0x4008, TT_U8, PM_LIVE, 0LL, 2LL, WE_NONE, 1.0f}, /* Burning type active */
  {0x4009, TT_S16, PM_LIVE, -50LL, 350LL, WE_NONE, 1.0f}, /* Exhaust gas temperature (2) [C] */
  {0x400A, TT_U16, PM_COMMAND, 7LL, 150LL, WE_NONE, 1.0f}, /* Virt. energy consumer set temp [C] */
  {0x400C, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for -10C [C] */
  {0x400D, TT_U8, PM_SETTING, 20LL, 80LL, WE_PLAIN, 1.0f}, /* CH temp for -10C [C] */
  {0x400E, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for -10C [C] */
  {0x400F, TT_U8, PM_SETTING, 20LL, 80LL, WE_NONE, 1.0f}, /* CH temp for -10C [C] */
  {0x4010, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4011, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4012, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4013, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4014, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4015, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4016, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4017, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4018, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4019, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x401A, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x401B, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x401C, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x401D, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x401E, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x401F, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4020, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4021, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4022, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4023, TT_U32, PM_SETTING, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm register */
  {0x4024, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4025, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4026, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4027, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4028, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4029, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x402A, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x402B, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x402C, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x402D, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x402E, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x402F, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4030, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4031, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4032, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4033, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4034, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4035, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4036, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4037, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm date timestamp */
  {0x4038, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4039, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x403A, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x403B, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x403C, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x403D, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x403E, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x403F, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4040, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4041, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4042, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4043, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4044, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4045, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4046, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4047, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4048, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x4049, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x404A, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x404B, TT_U32, PM_MIRROR, 0LL, 4294967295LL, WE_NONE, 1.0f}, /* Alarm confirm timestamp */
  {0x404C, TT_U8, PM_MIRROR, 0LL, 19LL, WE_NONE, 1.0f}, /* Alarm next index */
  {0x404D, TT_U8, PM_LIVE, 0LL, 20LL, WE_NONE, 1.0f}, /* Number of alarms */
  {0x404E, TT_U8, PM_COMMAND, 0LL, 1LL, WE_SWITCH, 1.0f}, /* Request reset alarm memory */
