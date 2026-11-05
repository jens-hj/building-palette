#!/usr/bin/env python3
"""Original 16x16 pixel-art logo, matching the other projects' sprite-and-dark-tile system."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/assets'
OUT.mkdir(parents=True, exist_ok=True)
sprite = Image.new('RGBA', (16, 16))
draw = ImageDraw.Draw(sprite)
outline = (24, 18, 48, 255)

# An oak block, a stone half-slab, and a small turquoise corner. All geometry is
# rasterized on the native sprite grid, then enlarged with nearest-neighbor sampling.
def face(points, fill):
    draw.polygon(points, fill=fill)

face([(1, 6), (6, 9), (6, 15), (1, 12)], '#b58b51')
face([(6, 9), (11, 6), (11, 12), (6, 15)], '#78552f')
face([(1, 6), (6, 3), (11, 6), (6, 9)], '#e6c783')
# Pixel grain and plank seams follow the three projected faces.
for start, end, color in [
    ((2, 8), (5, 10), '#8a6338'), ((2, 10), (5, 12), '#8a6338'),
    ((2, 7), (5, 9), '#d1aa68'), ((2, 11), (5, 13), '#c39a5b'),
    ((7, 10), (10, 8), '#543e29'), ((7, 12), (10, 10), '#543e29'),
    ((3, 6), (6, 4), '#f6dda0'), ((5, 7), (8, 5), '#c6a366'),
]:
    draw.line([start, end], fill=color)
for point in [(2, 8), (5, 11), (8, 9), (4, 6)]:
    draw.point(point, fill='#a57d43')

face([(7, 11), (11, 13), (11, 15), (7, 13)], '#8a939c')
face([(11, 13), (15, 11), (15, 13), (11, 15)], '#596574')
face([(7, 11), (11, 9), (15, 11), (11, 13)], '#c8d0d4')
draw.line([(9, 11), (12, 10)], fill='#e5e9e9')
draw.point((12, 12), fill='#a8b4bb')
draw.point((9, 12), fill='#9fa9b0')
draw.line([(12, 14), (14, 13)], fill='#748491')

face([(10, 2), (12, 3), (12, 6), (10, 5)], '#40bec4')
face([(12, 3), (14, 2), (14, 5), (12, 6)], '#287f92')
face([(10, 2), (12, 1), (14, 2), (12, 3)], '#96ece2')
draw.point((12, 2), fill='#ecfffa')
draw.point((11, 4), fill='#6edbd5')
# A small ametrine highlight echoes the gear project's palette.
draw.point((3, 2), fill='#d040c1')
draw.point((2, 3), fill='#7f21b1')
draw.point((4, 3), fill='#ef6dab')

sprite.save(OUT / 'icon-sprite.png')
sprite.resize((128, 128), Image.Resampling.NEAREST).save(ROOT / 'src/main/resources/assets/building_palette/icon.png')
tile = Image.new('RGBA', (512, 512))
ImageDraw.Draw(tile).rounded_rectangle((0, 0, 511, 511), radius=96, fill=(28, 24, 44, 255))
tile.alpha_composite(sprite.resize((448, 448), Image.Resampling.NEAREST), (32, 32))
tile.save(OUT / 'icon.png')
print(OUT / 'icon.png')
