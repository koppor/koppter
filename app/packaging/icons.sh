#!/bin/sh
# Builds the icons of the desktop packages from docs/logo.svg: koppter.ico for Windows, koppter.icns for macOS.
# Needs inkscape, ImageMagick and python3. Run from the root of the repository.
set -eu
work=$(mktemp -d)
for size in 128 256 512 1024; do
  inkscape docs/logo.svg -w "$size" -o "$work/$size.png"
done
convert "$work/256.png" -define icon:auto-resize=256,128,64,48,32,16 app/packaging/koppter.ico
python3 - "$work" <<'PYTHON'
import struct, sys
# An icns file is a list of PNG images, each under the type of its size.
images = [(kind, open(f'{sys.argv[1]}/{size}.png', 'rb').read()) for kind, size in ((b'ic07', 128), (b'ic08', 256), (b'ic09', 512), (b'ic10', 1024))]
entries = b''.join(kind + struct.pack('>I', len(data) + 8) + data for kind, data in images)
open('app/packaging/koppter.icns', 'wb').write(b'icns' + struct.pack('>I', len(entries) + 8) + entries)
PYTHON
rm -r "$work"
