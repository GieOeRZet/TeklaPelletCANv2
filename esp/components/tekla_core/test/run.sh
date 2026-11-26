#!/bin/sh
# Test natywny rdzenia (bez ESP-IDF): sh esp/components/tekla_core/test/run.sh
set -e; cd "$(dirname "$0")"
g++ -std=c++17 -Wall -Wextra -Wno-misleading-indentation -I../include -o /tmp/tekla_core_test test_core.cpp && /tmp/tekla_core_test
