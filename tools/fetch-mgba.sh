#!/bin/sh
# Scarica i sorgenti mGBA 0.10.5 in app/src/main/cpp/mgba (solo build locale).
# In CI lo fa il workflow. Non committare la cartella (vedi .gitignore).
set -eu
VER="0.10.5"
DST="$(dirname "$0")/../app/src/main/cpp/mgba"
if [ -f "$DST/CMakeLists.txt" ]; then
  echo "mGBA gia presente in $DST"
  exit 0
fi
rm -rf "$DST"
curl -sL -o /tmp/mgba.tgz "https://github.com/mgba-emu/mgba/archive/refs/tags/$VER.tar.gz"
tar -xzf /tmp/mgba.tgz -C /tmp
mv "/tmp/mgba-$VER" "$DST"
rm -f /tmp/mgba.tgz
echo "mGBA $VER pronto in $DST"
