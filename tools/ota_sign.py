#!/usr/bin/env python3
"""Klucz i podpis OTA (ECDSA P-256, SHA-256; podpis surowy r||s, 64 B, doklejany na koniec pliku). Klucz PRYWATNY zostaje tylko u wlasciciela (NIE w repo).
  python3 tools/ota_sign.py keygen PRIV.pem PUB.pem      # tworzy pare; PUB.pem wklej do esp/main/ota_pubkey.pem i przebuduj firmware
  python3 tools/ota_sign.py sign PRIV.pem IN.bin OUT.bin # IN.bin = app.bin albo ui_pack.bin; OUT.bin wysylasz POST /api/ota/fw|ui
  python3 tools/ota_sign.py verify PUB.pem SIGNED.bin"""
import sys
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, utils
cmd = sys.argv[1]
if cmd == "keygen":
    k = ec.generate_private_key(ec.SECP256R1())
    open(sys.argv[2], "wb").write(k.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    open(sys.argv[3], "wb").write(k.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo)); print("OK (klucz prywatny trzymaj poza repo)")
elif cmd == "sign":
    k = serialization.load_pem_private_key(open(sys.argv[2], "rb").read(), None); d = open(sys.argv[3], "rb").read()
    r, s = utils.decode_dss_signature(k.sign(d, ec.ECDSA(hashes.SHA256()))); open(sys.argv[4], "wb").write(d + r.to_bytes(32, "big") + s.to_bytes(32, "big")); print("podpisano:", len(d) + 64, "B")
elif cmd == "verify":
    pub = serialization.load_pem_public_key(open(sys.argv[2], "rb").read()); b = open(sys.argv[3], "rb").read(); d, sig = b[:-64], b[-64:]
    pub.verify(utils.encode_dss_signature(int.from_bytes(sig[:32], "big"), int.from_bytes(sig[32:], "big")), d, ec.ECDSA(hashes.SHA256())); print("podpis poprawny")
