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
  adb exec-out screencap -p > "docs/zrzuty_android/$(echo $s | tr ':' '_').png"
done
adb logcat -d -s AndroidRuntime:E > docs/zrzuty_android/crash.log || true
