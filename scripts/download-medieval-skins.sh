#!/usr/bin/env bash
set -u
OUT="src/main/resources/assets/livingcivilizations/textures/entity"
mkdir -p "$OUT"

# NameMC skin texture IDs selected from the Medieval and Adventurer tags.
# Direct PNG endpoint: https://s.namemc.com/i/<skin-id>.png
# NameMC may throttle downloads; failed downloads leave the bundled fallback skin intact.
download_skin() {
  local role="$1"; local skin_id="$2"; local target="$OUT/${role}.png"; local tmp="${target}.download"
  local url="https://s.namemc.com/i/${skin_id}.png"
  echo "NameMC skin: ${role} (${skin_id})"
  if curl --fail --location --silent --show-error --retry 2 --retry-delay 2 --connect-timeout 15 --max-time 60 \
      --user-agent "Mozilla/5.0 LivingCivilizationsMod/0.6" "$url" -o "$tmp"; then
    if python3 - "$tmp" <<'PY'
import struct, sys
p = sys.argv[1]
with open(p, "rb") as f: data = f.read(24)
if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n": raise SystemExit("not a PNG")
w, h = struct.unpack(">II", data[16:24])
if (w, h) != (64, 64): raise SystemExit(f"expected 64x64 skin, got {w}x{h}")
PY
    then mv "$tmp" "$target"; echo "  downloaded";
    else rm -f "$tmp"; echo "  invalid skin; keeping fallback" >&2; fi
  else rm -f "$tmp"; echo "  unavailable/rate-limited; keeping fallback" >&2; fi
  sleep 1
}

download_skin "traveler" "d39df4264a1139ba"
download_skin "caravan_merchant" "bc8f8764325054d7"
download_skin "village_merchant" "ee7b29a28b900600"
download_skin "worker" "f9bb3d5b5962982a"
download_skin "guard" "3cfd8cd8bd6d0020"
download_skin "warrior" "7142207cedf2c8ba"
download_skin "lord" "d348732fe5e216b2"
download_skin "king" "60f2d59004a68448"
download_skin "bandit" "b05c87d782470516"
