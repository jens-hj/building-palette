#!/usr/bin/env python3
"""Build the Modrinth page art in docs/page/ from showcase renders in build/showcase/.

Render first:  python scripts/headless-test.py ./gradlew --no-daemon runClientGameTest -PpaletteShowcase [...]
Icons are drawn here from the same 2x2x2 cell masks the mod uses; fonts are OFL (docs/page/fonts).
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

root = Path(__file__).resolve().parents[1]
shots = root / "build" / "showcase"
out = root / "docs" / "page"
fonts = out / "fonts"
TITLE = ImageFont.truetype(str(fonts / "PressStart2P-Regular.ttf"), 8)
HEAD = ImageFont.truetype(str(fonts / "Silkscreen-Bold.ttf"), 8)
BODY = ImageFont.truetype(str(fonts / "Silkscreen-Regular.ttf"), 8)

# Material colours: (top, front, side).
OAK = ((214, 176, 112), (176, 138, 82), (138, 104, 60))
STONE = ((196, 196, 200), (156, 156, 162), (118, 118, 126))
SPRUCE = ((150, 108, 66), (118, 82, 48), (90, 62, 36))
MOSS = ((132, 160, 96), (104, 128, 74), (78, 98, 56))
INK = (24, 22, 30)


def cells(mask, material):
    return {i: material for i in range(8) if mask >> i & 1}


def iso(parts, unit=6):
    """Pixel-art isometric drawing of 2x2x2 cells {index: material}. index = x | y<<1 | z<<2."""
    w, h = unit * 4 + 3, unit * 4 + 3
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ox, oy = w // 2, unit * 2 + 1

    def p(x, y, z):
        return (ox + (x - z) * unit, oy + (x + z) * unit // 2 - y * unit)

    for i in sorted(parts, key=lambda i: ((i & 1) + (i >> 2 & 1) + (i >> 1 & 1), i >> 1 & 1)):
        x, y, z = i & 1, i >> 1 & 1, i >> 2 & 1
        top, front, side = parts[i]
        faces = [
            ([p(x, y + 1, z), p(x + 1, y + 1, z), p(x + 1, y + 1, z + 1), p(x, y + 1, z + 1)], top),
            ([p(x, y, z + 1), p(x + 1, y, z + 1), p(x + 1, y + 1, z + 1), p(x, y + 1, z + 1)], front),
            ([p(x + 1, y, z), p(x + 1, y, z + 1), p(x + 1, y + 1, z + 1), p(x + 1, y + 1, z)], side),
        ]
        for poly, colour in faces:
            d.polygon(poly, fill=colour, outline=INK)
    return img


def up(img, factor):
    return img.resize((img.width * factor, img.height * factor), Image.NEAREST)


SHAPES = {
    "block": cells(255, OAK),
    "slab": cells(0b00110011, OAK),
    "stair": cells(0b00110011 | 0b00001100, OAK),
    "step": cells(0b00000011, OAK),
    "corner": cells(0b00000001, OAK),
    "l": {**cells(0b00010011, OAK)},
    "mixed": {**cells(0b00110011, STONE), **cells(0b00001100, OAK)},
    "fence": {**cells(0b00000011, SPRUCE)},
    "cog": {**cells(0b01010101, STONE), **cells(0b00100000, MOSS)},
}


def text_mask(text, font, scale):
    box = font.getbbox(text)
    m = Image.new("L", (box[2] - box[0] + 2, box[3] - box[1] + 2), 0)
    ImageDraw.Draw(m).text((-box[0] + 1, -box[1] + 1), text, font=font, fill=255)
    return up(m, scale)


def extruded(text, font, scale, depth, face_top, face_bottom, extrude, outline):
    """Blocky 3D lettering: gradient face, stepped extrusion, hard outline."""
    from PIL import ImageChops
    m = text_mask(text, font, scale)
    pad = depth + scale * 2
    w, h = m.width + pad * 2, m.height + pad * 2
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def at(k):
        layer = Image.new("L", (w, h), 0)
        layer.paste(m, (pad + k, pad + k))
        return layer

    grown = Image.new("L", (w, h), 0)
    for k in range(depth + 1):
        grown = ImageChops.lighter(grown, at(k))
    img.paste(Image.new("RGBA", (w, h), outline), (0, 0), grown.filter(ImageFilter.MaxFilter(scale * 2 + 1)))
    for k in range(depth, 0, -1):
        shade = tuple(int(c * (0.7 + 0.3 * (1 - k / depth))) for c in extrude[:3]) + (255,)
        img.paste(Image.new("RGBA", (w, h), shade), (0, 0), at(k))
    grad = Image.new("RGBA", (w, h))
    gd = ImageDraw.Draw(grad)
    for yy in range(h):
        t = min(max((yy - pad) / max(m.height, 1), 0), 1)
        gd.line([(0, yy), (w, yy)], fill=tuple(int(face_top[c] + (face_bottom[c] - face_top[c]) * t) for c in range(3)) + (255,))
    face = at(0)
    img.paste(grad, (0, 0), face)
    below = Image.new("L", (w, h), 0)
    below.paste(m, (pad, pad + scale))
    edge = ImageChops.subtract(face, below)
    img.paste(Image.new("RGBA", (w, h), (255, 246, 220, 255)), (0, 0), edge)
    return img.crop(img.getbbox())


def load(name):
    """A showcase render with its sky filled in. The software renderer leaves Photon's sky black,
    so near-black pixels connected to the top edge are replaced with a morning gradient."""
    img = Image.open(shots / f"{name}.png").convert("RGB")
    w, h = img.size
    dark = img.convert("L").point(lambda v: 255 if v < 24 else 0)
    mask = Image.new("L", (w, h), 0)
    seed = dark.copy()
    for x in range(0, w, 8):
        if seed.getpixel((x, 0)) == 255:
            ImageDraw.floodfill(seed, (x, 0), 128)
    mask = seed.point(lambda v: 255 if v == 128 else 0)
    if not mask.getbbox():
        return img
    horizon = mask.getbbox()[3]
    sky = Image.new("RGB", (w, h))
    sd = ImageDraw.Draw(sky)
    top, low = (92, 146, 214), (236, 214, 178)
    for yy in range(h):
        t = min(yy / max(horizon, 1), 1) ** 1.6
        sd.line([(0, yy), (w, yy)], fill=tuple(int(top[c] + (low[c] - top[c]) * t) for c in range(3)))
    return Image.composite(sky, img, mask.filter(ImageFilter.GaussianBlur(1)))


def banner():
    hero = load("hero")
    W, H = 1600, 640
    scale = max(W / hero.width, H / hero.height)
    hero = hero.resize((round(hero.width * scale), round(hero.height * scale)), Image.LANCZOS)
    top = int((hero.height - H) * 0.42)
    left = (hero.width - W) // 2
    hero = hero.crop((left, top, left + W, top + H))
    base = Image.new("RGBA", (W, H))
    base.paste(hero)
    shade = Image.new("RGBA", (W, H))
    sd = ImageDraw.Draw(shade)
    for yy in range(H):
        t = max(0, (yy - H * 0.38) / (H * 0.62))
        sd.line([(0, yy), (W, yy)], fill=(22, 20, 28, int(235 * t ** 1.4)))
    base = Image.alpha_composite(base, shade)
    title = extruded("BUILDING PALETTE", TITLE, 8, 10, (255, 226, 140), (232, 140, 52), (122, 66, 30), INK + (255,))
    if title.width > W - 80:
        title = title.resize((W - 80, round(title.height * (W - 80) / title.width)), Image.NEAREST)
    base.alpha_composite(title, ((W - title.width) // 2, H - title.height - 120))
    sub = text_mask("SLABS  STEPS  CORNERS  ONE SLOT", HEAD, 4)
    subimg = Image.new("RGBA", sub.size, (236, 232, 222, 255))
    shadow = Image.new("RGBA", sub.size, INK + (255,))
    x, y = (W - sub.width) // 2, H - 92
    base.paste(shadow, (x + 4, y + 4), sub)
    base.paste(subimg, (x, y), sub)
    base.convert("RGB").save(out / "banner.jpg", quality=90)


def header(name, text):
    icon = up(iso(SHAPES[name]), 3)
    label = text_mask(text, HEAD, 5)
    w = icon.width + 24 + label.width
    h = max(icon.height, label.height)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    img.alpha_composite(icon, (0, (h - icon.height) // 2))
    ly = (h - label.height) // 2
    img.paste(Image.new("RGBA", label.size, INK + (255,)), (icon.width + 27, ly + 3), label)
    img.paste(Image.new("RGBA", label.size, (240, 236, 226, 255)), (icon.width + 24, ly), label)
    img.save(out / f"header_{text.lower().replace(' ', '_').replace('&', 'and')}.png")


def divider():
    W, H = 1200, 30
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for x in range(W):
        t = 1 - abs(x - W / 2) / (W / 2)
        a = int(255 * min(1, t * 1.6))
        d.line([(x, H // 2 - 1), (x, H // 2)], fill=(214, 150, 70, a))
    gem = up(iso(SHAPES["corner"], 6), 1)
    img.alpha_composite(Image.new("RGBA", (gem.width + 16, H), (0, 0, 0, 0)), ((W - gem.width) // 2 - 8, 0))
    clear = Image.new("RGBA", (gem.width + 20, 6), (0, 0, 0, 0))
    img.paste(clear, ((W - gem.width) // 2 - 10, H // 2 - 3))
    img.alpha_composite(gem, ((W - gem.width) // 2, (H - gem.height) // 2))
    img.save(out / "divider.png")


def shapes():
    entries = [("block", "8"), ("stair", "6"), ("slab", "4"), ("step", "2"), ("corner", "1")]
    tiles = []
    for name, units in entries:
        icon = up(iso(SHAPES[name], 7), 4)
        label = text_mask(name.upper() + "  " + units, HEAD, 3)
        tile = Image.new("RGBA", (max(icon.width, label.width) + 20, icon.height + label.height + 18), (0, 0, 0, 0))
        tile.alpha_composite(icon, ((tile.width - icon.width) // 2, 0))
        lx = (tile.width - label.width) // 2
        tile.paste(Image.new("RGBA", label.size, INK + (255,)), (lx + 3, icon.height + 17), label)
        tile.paste(Image.new("RGBA", label.size, (240, 236, 226, 255)), (lx, icon.height + 14), label)
        tiles.append(tile)
    W = sum(t.width for t in tiles) + 36 * (len(tiles) - 1)
    H = max(t.height for t in tiles)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    x = 0
    for t in tiles:
        img.alpha_composite(t, (x, H - t.height))
        x += t.width + 36
    img.save(out / "shapes.png")


def framed(img, caption=None):
    """Rounded dark frame with an optional pixel caption."""
    pad = 10
    W, H = img.width + pad * 2, img.height + pad * 2 + (44 if caption else 0)
    base = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(base).rounded_rectangle((0, 0, W - 1, H - 1), 14, fill=(30, 30, 38, 255), outline=(214, 150, 70, 255), width=3)
    base.paste(img, (pad, pad))
    if caption:
        m = text_mask(caption, HEAD, 3)
        base.paste(Image.new("RGBA", m.size, (240, 236, 226, 255)), ((W - m.width) // 2, H - 34), m)
    return base


def still(name, width=1000, source=None, box=None):
    img = load(source or name)
    if box:
        img = img.crop(box)
    img = img.resize((width, round(img.height * width / img.width)), Image.LANCZOS)
    img = ImageEnhance.Contrast(img).enhance(1.04)
    img.save(out / f"{name}.jpg", quality=88)


def gif(name, frames, durations, size):
    frames = [f.convert("RGBA") for f in frames]
    bg = Image.new("RGBA", frames[0].size, (30, 30, 38, 255))
    flat = [Image.alpha_composite(bg, f).convert("RGB").resize(size, Image.LANCZOS).quantize(colors=200, method=Image.Quantize.MEDIANCUT) for f in frames]
    flat[0].save(out / f"{name}.gif", save_all=True, append_images=flat[1:], duration=durations, loop=0, optimize=True, disposal=2)


def merge_gif():
    captions = ["", "STEP", "STEP ACROSS IT = L  (+1)", "ADD A STONE CORNER", "STONE STEP ON TOP"]
    frames = []
    for i, caption in enumerate(captions):
        img = load(f"merge_{i}")
        c = img.crop((150, 150, 750, 750)).resize((480, 480), Image.LANCZOS)
        frames.append(framed(c, caption or "EMPTY BLOCK"))
    gif("merge", frames, [900, 1100, 1700, 1300, 1700], frames[0].size)


def scroll_gif():
    frames = []
    for i in range(8):
        path = shots / f"scroll_{i}.png"
        if not path.exists():
            break
        # Tight crop around the shape column, item name and hotbar, scaled up crisply.
        # No sky fill here: the crop holds no sky and the fill leaks into dark wood.
        img = Image.open(path).convert("RGB")
        crop = img.crop((238, 226, 616, 486))
        frames.append(framed(crop.resize((crop.width * 2, crop.height * 2), Image.NEAREST)))
    if frames:
        gif("scroll", frames, [650] * len(frames), frames[0].size)


if __name__ == "__main__":
    out.mkdir(parents=True, exist_ok=True)
    banner()
    divider()
    shapes()
    for icon, text in [("stair", "Shapes"), ("mixed", "Combine"), ("fence", "Offgrid"), ("cog", "Controls"), ("block", "Requirements")]:
        header(icon, text)
    for name in ("detail_window", "detail_roof"):
        if (shots / f"{name}.png").exists():
            still(name)
    if (shots / "hero.png").exists():
        still("detail_bench", source="hero", box=(560, 372, 1420, 856))
    merge_gif()
    scroll_gif()
    print("Wrote", sorted(p.name for p in out.iterdir() if p.is_file()))
