idf_component_register(SRCS "main.cpp" "bus.cpp" "auth.cpp" "web.cpp" "ota.cpp" "net.cpp" "device.cpp" "ble.cpp"
  INCLUDE_DIRS "." REQUIRES tekla_core mcp2515 nvs_flash esp_wifi esp_netif esp_event esp_http_server esp_timer app_update json mbedtls esp_partition esp_http_client esp-tls bt
  EMBED_TXTFILES "ota_pubkey.pem")
