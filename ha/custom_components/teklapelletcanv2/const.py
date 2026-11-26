"""Stale integracji TeklaPelletCANv2."""
DOMAIN = "teklapelletcanv2"
VERSION = "0.1.0"
URL_BASE = "/teklapelletcanv2"
CONF_TOKEN = "token"
CONF_ID = "device_id"
CONF_LEVEL = "level"
CONF_PIDS = "pids"
CONF_SCAN = "scan_interval"
DEFAULT_SCAN = 5
API_VERSION = 2
EXTRA_EVERY = 6        # PID-y spoza zestawu "na zywo" odpytywane co N cykli
