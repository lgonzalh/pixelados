"""Hoja de contacto del catalogo de simbolos, para revisar que se lean bien."""
import re
from PIL import Image, ImageDraw

SRC = r"C:\GitHub\pixelados\app\src\main\java\com\pixelados\data\model\StampCatalog.kt"
OUT = r"C:\GitHub\pixelados\build\stamps_preview.png"

text = open(SRC, encoding="utf-8").read()
pattern = re.compile(
    r'name = "([^"]+)",\s*category = "([^"]+)",\s*pattern = mask\(((?:\s*"[01]+",)+)\s*\)'
)
entries = pattern.findall(text)
print("simbolos:", len(entries))

cols, cell = 12, 76
rows = (len(entries) + cols - 1) // cols
sheet = Image.new("RGB", (cols * cell, rows * cell), (250, 250, 252))
d = ImageDraw.Draw(sheet)

for i, (name, _cat, mask) in enumerate(entries):
    grid = re.findall(r'"([01]+)"', mask)
    n = len(grid)
    px = max(4, 60 // n)
    tile = Image.new("RGB", (n * px, n * px), (255, 255, 255))
    td = ImageDraw.Draw(tile)
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch == "1":
                td.rectangle([x * px, y * px, (x + 1) * px - 1, (y + 1) * px - 1], fill=(28, 28, 38))
    x0 = (i % cols) * cell
    y0 = (i // cols) * cell
    sheet.paste(tile, (x0 + 6, y0 + 4))
    d.text((x0 + 4, y0 + cell - 15), name[:14], fill=(0, 0, 0))

sheet.save(OUT)
print("ok")
