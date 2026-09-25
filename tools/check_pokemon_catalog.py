"""Comprueba que el catálogo Kotlin de Pokémon se decodifica igual que la vista
previa: lee el archivo generado (paleta + figuras en texto) y dibuja la hoja de
contacto. Sirve para verificar sin dispositivo."""
import re
from PIL import Image

SRC = r"C:\GitHub\pixelados\app\src\main\java\com\pixelados\data\model\PokemonCatalog.kt"
OUT = r"C:\GitHub\pixelados\build\pokemon_from_kotlin.png"

text = open(SRC, encoding="utf-8").read()
chars = re.search(r'CHARS = "([^"]+)"', text).group(1)
palette = re.search(r'PALETTE = "([^"]+)"', text).group(1).split(",")
colors = [tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) for h in palette]

# entradas: nombre + texto de la figura (puede venir partido en trozos con +)
entries = []
for block in text.split("Stamp(")[1:]:
    name = re.search(r'name = "([^"]+)"', block).group(1)
    chunks = re.findall(r'"([^"]*)"', block)
    # el primer texto entrecomillado es el nombre; el resto es la figura
    figura = "".join(chunks[1:])
    entries.append((name, figura))

print("figuras:", len(entries))

cols, cell = 8, 120
rows = (len(entries) + cols - 1) // cols
sheet = Image.new("RGB", (cols * cell, rows * cell), (250, 250, 252))

for i, (name, figura) in enumerate(entries):
    filas = figura.split("/")
    n = len(filas)
    p = (cell - 12) / max(1, n)
    tile = Image.new("RGB", (cell - 12, cell - 12), (255, 255, 255))
    for y, fila in enumerate(filas):
        for x, ch in enumerate(fila):
            if ch != "." and chars.find(ch) >= 0:
                c = colors[chars.index(ch)]
                tile.paste(Image.new("RGB", (int(p) + 1, int(p) + 1), c), (int(x * p), int(y * p)))
    # filas con longitud distinta -> aviso
    if len({len(f) for f in filas}) != 1:
        print("  aviso: filas de distinto ancho en", name)
    sheet.paste(tile, ((i % cols) * cell + 6, (i // cols) * cell + 6))

sheet.save(OUT)
print("ok ->", OUT)
