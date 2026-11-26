#pragma once
// BLE (NimBLE) - tylko do pierwszego wykrycia i konfiguracji Wi-Fi przez aplikacje. Reklamowanie wlacza sie, gdy modul jest nieskonfigurowany
// albo nie ma Wi-Fi (> 3 min), a wylacza po stabilnym polaczeniu z siecia (> 60 s). Szczegoly protokolu: docs/API_ESP.md sekcja 12.
namespace ble { void start(); }
