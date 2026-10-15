#!/usr/bin/env python3
"""Draws the Sky VeLor splash wordmark PNG used by the FML loading screen.

Pure standard library (zlib + struct), no Pillow. The wordmark is a 3x5 pixel font scaled up, on a transparent
background with a soft vertical gradient and an accent underline. Re-run to regenerate the committed asset:

    python3 tools/make_splash_logo.py
"""
import os
import struct
import zlib

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "SkyVeLor-Beta-0.7v", "src", "main", "resources", "assets", "pekaclient",
                   "textures", "gui", "skyvelor_splash.png")

# 3x5 pixel font, one string per row per glyph.
GLYPHS = {
    "S": ["###", "#..", "###", "..#", "###"],
    "K": ["#.#", "#.#", "##.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."],
    "V": ["#.#", "#.#", "#.#", ".#.", ".#."],
    "E": ["###", "#..", "###", "#..", "###"],
    "L": ["#..", "#..", "#..", "#..", "###"],
    "O": ["###", "#.#", "#.#", "#.#", "###"],
    "R": ["###", "#.#", "###", "#.#", "#.#"],
    " ": ["...", "...", "...", "...", "..."],
}
TEXT = "SKY VELOR"
SCALE = 6

# Accent blue (Sky VeLor Theme.accent) and a light sky blue for the gradient ends.
TOP = (156, 195, 255)
BOTTOM = (61, 120, 214)
ACCENT = (10, 132, 255)


def make_pixels():
    glyph_w, glyph_h = 3, 5
    text_w = len(TEXT) * (glyph_w + 1) - 1
    w = text_w * SCALE
    h = glyph_h * SCALE
    # 8 px of space under the wordmark for the accent bar, plus a 2 px margin either side.
    width = w + 24
    height = h + 26
    px = [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]

    def put(x, y, rgba):
        if 0 <= x < width and 0 <= y < height:
            px[y][x] = rgba

    for row in range(glyph_h):
        for col in range(glyph_w):
            for ci, ch in enumerate(TEXT):
                if GLYPHS[ch][row][col] != "#":
                    continue
                gx = ci * (glyph_w + 1) + col
                for sy in range(SCALE):
                    for sx in range(SCALE):
                        t = (sy / float(SCALE - 1)) if SCALE > 1 else 0.0
                        r = int(TOP[0] + (BOTTOM[0] - TOP[0]) * t)
                        g = int(TOP[1] + (BOTTOM[1] - TOP[1]) * t)
                        b = int(TOP[2] + (BOTTOM[2] - TOP[2]) * t)
                        put(12 + gx * SCALE + sx, 8 + row * SCALE + sy, (r, g, b, 255))

    # Accent underline, two rows tall, inset to the text width.
    for x in range(12, 12 + w):
        put(x, h + 16, (ACCENT[0], ACCENT[1], ACCENT[2], 220))
        put(x, h + 17, (ACCENT[0], ACCENT[1], ACCENT[2], 140))
    return width, height, px


def write_png(path, width, height, px):
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type 0
        for x in range(width):
            r, g, b, a = px[y][x]
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)
    return len(png)


if __name__ == "__main__":
    w, h, pixels = make_pixels()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    size = write_png(OUT, w, h, pixels)
    print("wrote %s (%dx%d, %d bytes)" % (OUT, w, h, size))
