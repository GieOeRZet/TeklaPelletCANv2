#!/bin/sh
# Zrzuty ekranow aplikacji w emulatorze (CI). Wynik: docs/zrzuty_android/*.png
set -e
APK=android/app/build/outputs/apk/debug/app-debug.apk
adb install -r "$APK"
mkdir -p docs/zrzuty_android
for s in home kociol palnik co1 co2 cwu config sched:co1 more module alarms stats; do
  adb shell am force-stop pl.tekla.pellet
  adb shell am start -n pl.tekla.pellet/.MainActivity --es screen "$s" >/dev/null
  sleep 6
  if ! adb shell pidof pl.tekla.pellet >/dev/null 2>&1; then
    echo "CRASH: aplikacja nie ma procesu po ekranie $s"
    echo "===== CRASH LOG $s ====="
    adb logcat -d -b main -b system -b crash -t 300 || true
    exit 1
  fi
  if ! adb shell dumpsys activity activities | grep -q "pl.tekla.pellet"; then
    echo "CRASH/STOP: aktywność aplikacji nie jest widoczna po ekranie $s"
    adb logcat -d -b main -b system -b crash -t 200 > "docs/zrzuty_android/crash_$s.log" || true
    exit 1
  fi
  adb exec-out screencap -p > "docs/zrzuty_android/$(echo $s | tr ':' '_').png"
done
for s in palnik kociol home; do
  adb shell am force-stop pl.tekla.pellet
  adb shell am start -n pl.tekla.pellet/.MainActivity --es screen "$s" --ei tick 25 >/dev/null
  sleep 6
  if ! adb shell pidof pl.tekla.pellet >/dev/null 2>&1; then
    echo "CRASH: aplikacja nie ma procesu po ekranie ${s}_run"
    adb logcat -d -b main -b system -b crash -t 200 > "docs/zrzuty_android/crash_${s}_run.log" || true
    exit 1
  fi
  adb exec-out screencap -p > "docs/zrzuty_android/${s}_run.png"
done
adb logcat -d -b main -b system -b crash -t 500 > docs/zrzuty_android/crash.log || true
