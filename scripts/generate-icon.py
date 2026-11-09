#!/usr/bin/env python3
"""Render the Building Palette icon: an animated GIF for Modrinth and a still PNG for the jar.

Frames follow the palette order (block, slab, step, corner), then show mixed-material
combinations on the 2x2x2 grid. Run inside `nix develop`.
"""
import math
from pathlib import Path
from PIL import Image, ImageDraw

SS = 4
OUT = 512
N = OUT * SS
c30, s30 = math.cos(math.radians(30)), math.sin(math.radians(30))
s = N * 0.215
cx0, cy0 = N / 2, N / 2

def P(x, y, z):
    # centre of the 2x2x2 block at (1,1,1) maps to image centre
    x, y, z = x - 1, y - 1, z - 1
    return (cx0 + (x - z) * c30 * s, cy0 + (x + z) * s30 * s - y * s)

BG = (84, 70, 110)
HEX = (58, 47, 78)
# (top, left(+z), right(+x))
MAT = {
    "A": ((250, 236, 205), (238, 205, 148), (196, 152, 98)),    # sand
    "B": ((250, 178, 150), (226, 120, 92), (170, 78, 64)),       # terracotta
    "C": ((196, 232, 212), (128, 196, 162), (78, 146, 120)),     # sage
}

def frame(octs):
    img = Image.new("RGB", (N, N), BG)
    d = ImageDraw.Draw(img)
    # dark hexagon = the full block's volume, slightly enlarged
    hexp = [P(0, 2, 0), P(2, 2, 0), P(2, 0, 0), P(2, 0, 2), P(0, 0, 2), P(0, 2, 2)]
    k = 1.10
    hexp = [(cx0 + (px - cx0) * k, cy0 + (py - cy0) * k) for px, py in hexp]
    d.polygon(hexp, fill=HEX)
    for (x, y, z) in sorted(octs, key=lambda p: (p[0] + p[1] + p[2], p[1])):
        top, left, right = MAT[octs[(x, y, z)]]
        if (x, y + 1, z) not in octs:
            d.polygon([P(x, y+1, z), P(x+1, y+1, z), P(x+1, y+1, z+1), P(x, y+1, z+1)], fill=top)
        if (x + 1, y, z) not in octs:
            d.polygon([P(x+1, y+1, z), P(x+1, y+1, z+1), P(x+1, y, z+1), P(x+1, y, z)], fill=right)
        if (x, y, z + 1) not in octs:
            d.polygon([P(x, y+1, z+1), P(x+1, y+1, z+1), P(x+1, y, z+1), P(x, y, z+1)], fill=left)
    return img.resize((OUT, OUT), Image.LANCZOS)

ALL = [(x, y, z) for x in (0, 1) for y in (0, 1) for z in (0, 1)]
def m(cells, mat): return {c: mat for c in cells}

frames = [
    m(ALL, "A"),                                                           # full block
    m([c for c in ALL if c[1] == 0], "A"),                                 # slab
    m([(0, 0, 1), (1, 0, 1)], "A"),                                        # step
    m([(1, 0, 1)], "A"),                                                   # corner
    {**m([c for c in ALL if c[1] == 0], "A"), **m([(0, 1, 0), (1, 1, 0)], "B")},          # mixed stairs
    {**m([c for c in ALL if c[0] == 0], "A"), **m([(1, 0, 0), (1, 1, 0)], "B")},          # vertical slab + vertical step
    {**m([c for c in ALL if c[1] == 0], "C"), **m([(0, 1, 0), (1, 1, 0), (0, 1, 1)], "B")},  # inner corner
    {c: ("A" if sum(c) % 2 == 0 else "B") for c in ALL},                   # checkerboard
]

root = Path(__file__).resolve().parents[1]
imgs = [frame(f) for f in frames]
still = imgs[4]  # mixed-material stairs
still.save(root / 'docs/assets/icon.png', optimize=True)
still.save(root / 'common/src/main/resources/assets/building_palette/icon.png', optimize=True)
pal = [im.quantize(colors=64, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE) for im in imgs]
pal[0].save(root / 'docs/assets/icon.gif', save_all=True, append_images=pal[1:],
            duration=600, loop=0, optimize=True, disposal=1)
