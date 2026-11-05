#!/usr/bin/env python3
"""Generate the project's original vector icon; no Minecraft textures are embedded."""
from pathlib import Path
import shutil
import subprocess

root = Path(__file__).resolve().parents[1]
destination = root / "docs" / "assets"
destination.mkdir(parents=True, exist_ok=True)

parts = ['<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">',
         '<rect width="512" height="512" rx="96" fill="#1c182c"/>']

def block(x, y, width, depth, height, top, left, right):
    a, b, c, d = (x, y), (x + width, y - width / 2), (x + width + depth, y + (depth-width)/2), (x + depth, y + depth/2)
    def polygon(points, color):
        parts.append('<polygon points="' + ' '.join(f'{px},{py}' for px, py in points) + f'" fill="{color}" stroke="#12101d" stroke-width="7" stroke-linejoin="round"/>')
    polygon([a,d,(d[0],d[1]+height),(a[0],a[1]+height)], left)
    polygon([d,c,(c[0],c[1]+height),(d[0],d[1]+height)], right)
    polygon([a,b,c,d], top)

# Full cube, slab, and small corner share the same visual construction.
block(62,245,104,104,110,'#dfbe7c','#b48b52','#89683f')
block(248,338,100,100,48,'#bdc4c9','#89949e','#66727f')
block(306,185,58,58,58,'#c0e5bd','#7eaf83','#52836c')
parts += ['<path d="M134 164 Q225 90 316 132" fill="none" stroke="#d6c5f2" stroke-width="15" stroke-linecap="round"/>',
          '<path d="M298 111 L322 134 L290 142" fill="none" stroke="#d6c5f2" stroke-width="15" stroke-linecap="round" stroke-linejoin="round"/>', '</svg>']
(destination / "icon.svg").write_text('\n'.join(parts) + '\n')
renderer = shutil.which("rsvg-convert")
if renderer is None:
    candidates = sorted(Path("/nix/store").glob("*-librsvg-*/bin/rsvg-convert"))
    renderer = str(candidates[-1]) if candidates else None
if renderer is None:
    raise SystemExit("Install librsvg/rsvg-convert to rasterize the vector icon")
subprocess.run([renderer, "--width", "512", "--height", "512", "--output", str(destination / "icon.png"), str(destination / "icon.svg")], check=True)
mod_icon = root / "src/main/resources/assets/building_palette/icon.png"
subprocess.run([renderer, "--width", "128", "--height", "128", "--output", str(mod_icon), str(destination / "icon.svg")], check=True)
print(destination / "icon.png")
